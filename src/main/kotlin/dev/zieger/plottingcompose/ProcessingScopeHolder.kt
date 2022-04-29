package dev.zieger.plottingcompose

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.indicators.candles.IndicatorCandle
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.processor.Processor
import dev.zieger.utils.time.ITimeStamp
import dev.zieger.utils.time.delay
import dev.zieger.utils.time.millis
import kotlinx.coroutines.*
import java.util.*

interface GlobalProcessingScopeHolder {

    val rawXRange: ClosedRange<Float>
    val chartData: SnapshotStateMap<InputContainer<IndicatorCandle>, Map<Key<IndicatorCandle, *>, List<PortValue<*>>>>
    fun onScopesUpdated(listener: () -> Unit): () -> Unit

    suspend fun processRange(range: ClosedRange<ITimeStamp>)
    val visibleXPixelRange: ClosedRange<Double>
}

class GlobalProcessingScopeHolderImpl(
    private val cs: CoroutineScope,
    private val definition: ChartDefinition<IndicatorCandle>,
    private val transformationHolder: TransformationHolder,
    private val processingSource: ProcessingSource<IndicatorCandle>,
    private val globalSizeHolder: GlobalSizeHolder
) : GlobalProcessingScopeHolder {

    override val rawXRange: ClosedRange<Float>
        get() = allScopes.ifEmpty { null }
            ?.run { minOf { it.value.input.x.toFloat() }..maxOf { it.value.input.x.toFloat() } } ?: 0f..0f
    private val allScopes: MutableMap<Long, ProcessingScope<IndicatorCandle>> = HashMap()
    override val chartData: SnapshotStateMap<InputContainer<IndicatorCandle>, Map<Key<IndicatorCandle, *>, List<PortValue<*>>>> =
        mutableStateMapOf()

    private val scopeUpdatedListener = LinkedList<() -> Unit>()

    override fun onScopesUpdated(listener: () -> Unit): () -> Unit {
        scopeUpdatedListener += listener
        return { scopeUpdatedListener -= listener }
    }

    override val visibleXPixelRange: ClosedRange<Double>
        get() = -transformationHolder.finalTranslation.x.toDouble()..
                -transformationHolder.finalTranslation.x.toDouble() +
                globalSizeHolder.rootBorderRect.width - globalSizeHolder.yLabelWidth.value

    init {
        transformationHolder.onTransformationChanged { _, _ -> updateScopes() }
    }

    override suspend fun processRange(range: ClosedRange<ITimeStamp>) {
        var updateJob: Job? = null
        Processor(definition.keys()).process(processingSource.input(range)).collect { (idx, scope) ->
            allScopes[idx] = scope

            updateJob?.cancel()
            if (idx % 100L == 0L) withContext(Dispatchers.Main) {
                updateScopes()
            }
            else updateJob = cs.launch {
                delay(250.millis)
                if (!isActive) return@launch
                withContext(Dispatchers.Main) {
                    updateScopes()
                }
            }
        }
        updateJob?.cancel()
    }

    private fun updateScopes() {
        chartData.clear()
        chartData.putAll(allScopes.chartData())
        scopeUpdatedListener.forEach { it() }
    }

    private fun <I : Input> Map<Long, ProcessingScope<I>>.chartData(): Map<InputContainer<I>, Map<Key<I, *>, List<PortValue<*>>>> {
        if (isEmpty()) return emptyMap()
        return entries.toList().subList(
            (visibleXPixelRange.start * transformationHolder.finalScale.x).toInt(),
            (visibleXPixelRange.endInclusive * transformationHolder.finalScale.x).toInt()
        ).associate { (idx, scope) ->
            InputContainer(scope.input, idx) to scope.data
        }
    }
}

interface ProcessingScopeHolder : GlobalProcessingScopeHolder {

    fun release()
}

class ProcessingScopeHolderImpl(
    private val chart: Chart<IndicatorCandle>,
    private val transformationHolder: TransformationHolder,
    private val sizeHolder: SizeHolder,
    globalProcessingScopeHolder: GlobalProcessingScopeHolder
) : ProcessingScopeHolder,
    GlobalProcessingScopeHolder by globalProcessingScopeHolder {

    private val removeListener = onScopesUpdated { update() }

    private fun update() {
        transformationHolder.run {
            applyScaleRange(chartData.size, sizeHolder.plotRect)
            applyScaleOffset(chart.visibleArea, chartData.size, sizeHolder.plotRect.width)
            applyTranslationRange(chartData.size, sizeHolder.plotRect)
            applyTranslationOffsetX(chart.visibleArea, rawXRange, sizeHolder.plotRect, chartData.size, finalScale.x)
        }

        sizeHolder.buildLabels(sizeHolder.chartSize.value, chartData)
    }

    override fun release() {
        removeListener()
    }
}

