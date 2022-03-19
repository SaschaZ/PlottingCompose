@file:Suppress("FunctionName")

package dev.zieger.plottingcompose.scopes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.zieger.plottingcompose.InputContainer
import dev.zieger.plottingcompose.definition.Chart
import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.x
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine
import kotlin.math.absoluteValue

interface IPlotDrawScope<T : Input> : IChartDrawScope<T>, IChartEnvironment, IStates {
    val chart: Chart<T>
    val chartData: Map<InputContainer<T>, Map<Key<T>, List<PortValue<*>>>>

    val yLabelRect: Rect
    val yTickRect: Rect
    val xLabelRect: Rect
    val xTickRect: Rect
    val plotBorderRect: Rect
    val plotRect: Rect

    val yValueRange: ClosedRange<Double>
    val xValueRange: ClosedRange<Double>

    val yTicks: Map<Double, String>
    val xTicks: Map<Double, String>

    val widthDivisor: Float

    val yLabelHeight: Float
    val xLabelWidth: Float
    fun Offset.toScene(): Offset

    val visibleXPixelRange: ClosedRange<Double>
    val visibleYPixelRange: ClosedRange<Double>
}

fun <T : Input> PlotDrawScope(
    chart: Chart<T>,
    chartDrawScope: IChartDrawScope<T>,
    states: IStates,
    rect: Rect
): IPlotDrawScope<T> = object : IPlotDrawScope<T>, IChartDrawScope<T> by chartDrawScope,
    IStates by states {

    override val chart: Chart<T> = chart

    override val plotBorderRect: Rect = rect.run {
        Rect(
            left, top, right - yLabelWidth.value.toFloat() - chart.tickLength(chartSize.value).value / 2f,
            bottom - xLabelHeight.value.toFloat() - chart.tickLength(chartSize.value).value / 2f
        )
    }
    override val plotRect: Rect = plotBorderRect.run {
        Rect(
            left + chart.margin.left.invoke(chartSize.value).value,
            top,
            right - chart.margin.right.invoke(chartSize.value).value,
            bottom
        )
    }

    override val widthDivisor: Float = scale.value.x / 10

    override val visibleXPixelRange: ClosedRange<Double> = plotRect.run {
        -finalTranslation.x.toDouble()..-finalTranslation.x.toDouble() + width
    }

    private val rawData = scopes.chartData(chart)

    private var startIdx = 0
    private var endIdx = 0

    override val chartData: Map<InputContainer<T>, Map<Key<T>, List<PortValue<*>>>> = rawData.let { data ->
        if (data.size <= 2) return@let emptyMap()

        startIdx =
            data.entries.indexOfFirst { (input, _) -> input.idx / widthDivisor >= visibleXPixelRange.start }
                .takeIf { it >= 0 }
                ?.let { it - 5 }
                ?.coerceAtLeast(0) ?: 0

        endIdx =
            data.entries.indexOfLast { (input, _) -> input.idx / widthDivisor <= visibleXPixelRange.endInclusive }
                .takeIf { it >= 0 }
                ?.let { it + 5 }
                ?.coerceAtMost(data.size) ?: 150

        data.entries.toList().subList(startIdx, endIdx)
            .associate { (k, v) -> k to v }.also { cd ->
//                rawData.keys.lastOrNull()?.also { last ->
//                    translationOffset.value =
//                        translationOffset.value.copy(x = (last.idx / -widthDivisor + plotRect.width * 0.9f) * scale.value.x)
//                }
                focusedItemIdx.value = findFocusedItemIdx(cd)
            }
    }

    private fun findFocusedItemIdx(data: Map<InputContainer<T>, Map<Key<T>, List<PortValue<*>>>>): FocusedInfo? =
        mousePosition.value?.takeIf { plotRect.contains(it) }?.let { mp ->
            val relX = mp.x - plotRect.left - finalTranslation.x
            val d = data.entries.toList()
            d.minByOrNull { (it.key.idx / widthDivisor - relX).absoluteValue }
                ?.let { (input, _) -> FocusedInfo(input.idx, input.idx / widthDivisor + finalTranslation.x) }
        }

    override val xValueRange: ClosedRange<Double> = chartData.xValueRange()
    override val xTicks: Map<Double, String> = chart.xTicks(this, startIdx..endIdx, xValueRange).also { xTicks ->
        xLabelHeight.value =
            (xTicks.values.toList().nullWhenEmpty()
                ?.run { get(size / 2) }?.size(20f)?.height)?.toDouble() ?: 0.0
    }
    override val xLabelWidth = xTicks.values.toList().nullWhenEmpty()?.run { get(size / 2) }?.size(20f)?.width ?: 0f

    override val yValueRange: ClosedRange<Double> = chartData.yValueRange().also {
        heightDivisor.value = it.range() / plotRect.height
    }
    override val yTicks: Map<Double, String> = chart.yTicks(this, yValueRange).also { yTicks ->
        yLabelWidth.value = (yTicks.values.maxByOrNull { it.length }?.size(20f)?.width)?.toDouble() ?: 0.0
    }
    override val yLabelHeight = yTicks.values.maxByOrNull { it.length }?.size(20f)?.height ?: 0f

    override val yLabelRect: Rect = rect.run {
        Rect(
            right - yLabelWidth.value.toFloat(), top, right,
            bottom - xLabelHeight.value.toFloat() - chart.tickLength(chartSize.value).value / 2f
        )
    }
    override val yTickRect: Rect = yLabelRect.run {
        Rect(
            left - chart.tickLength(chartSize.value).value, top, left, bottom
        )
    }
    override val xLabelRect: Rect = rect.run {
        Rect(
            left, bottom - xLabelHeight.value.toFloat(), right - yLabelWidth.value.toFloat(), bottom
        )
    }
    override val xTickRect: Rect = xLabelRect.run {
        Rect(
            left, top - chart.tickLength(chartSize.value).value, right, top
        )
    }

    override val visibleYPixelRange: ClosedRange<Double> = plotRect.run {
        yValueRange.run { start / heightDivisor.value.toFloat()..endInclusive / heightDivisor.value.toFloat() }
    }

    override fun Offset.toScene(): Offset =
        Offset(plotRect.left + x / widthDivisor, plotRect.bottom - y / heightDivisor.value.toFloat())

    private fun <I : Input> Map<InputContainer<I>, Map<Key<I>, List<PortValue<*>>>>.yValueRange(): ClosedRange<Double> {
        if (isEmpty() || flatMap { it.value.values }.isEmpty()) return 0.0..0.0

        val yMin = minOf {
            it.value.minOf { i2 ->
                i2.value.filter { (port, _) -> port.includeIntoScaling }.minOf { (_, value) ->
                    value.yRange.start
                }
            }
        }
        val yMax = maxOf {
            it.value.maxOf { i2 ->
                i2.value.filter { (port, _) -> port.includeIntoScaling }.maxOf { (_, value) ->
                    value.yRange.endInclusive
                }
            }
        }
        return yMin..yMax
    }

    private fun <I : Input> Map<InputContainer<I>, Map<Key<I>, List<PortValue<*>>>>.xValueRange(): ClosedRange<Double> {
        if (isEmpty()) return 0.0..0.0

        val xMin = minOf { it.key.input.x.toDouble() }
        val xMax = maxOf { it.key.input.x.toDouble() }
        return xMin..xMax
    }
}

private operator fun ClosedRange<Float>.minus(value: Float): ClosedRange<Float> =
    start - value..endInclusive - value

fun <T> ClosedRange<T>.range() where T : Comparable<T>, T : Number = endInclusive.toDouble() - start.toDouble()

fun <E, C : Collection<E>> C.nullWhenEmpty(): C? = ifEmpty { null }

private fun <I : Input> List<Pair<Long, ProcessingScope<I>>>.chartData(chart: Chart<I>): Map<InputContainer<I>, Map<Key<I>, List<PortValue<*>>>> {
    val slots = chart.plots.flatMap { it.slots }
    val keys = slots.map { it.key }
    val ports = slots.map { it.port }
    return associate { (idx, scope) ->
        InputContainer(scope.input, idx) to scope.data.filterKeys { k -> k in keys }
            .map { (key, value) ->
                key to value.filter { portValue -> portValue.port in ports }
            }.toMap()
    }
}

fun String.size(fontSize: Float): TextLine {
    val font = Font(null, fontSize)
    return TextLine.make(this, font)
}