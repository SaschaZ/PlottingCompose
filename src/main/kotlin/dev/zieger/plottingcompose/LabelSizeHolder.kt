package dev.zieger.plottingcompose

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import dev.zieger.plottingcompose.definition.Chart
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.definition.Ticks
import dev.zieger.plottingcompose.indicators.candles.IndicatorCandle
import dev.zieger.plottingcompose.scopes.toDouble

interface LabelSizeHolder {

    val xLabelRect: Rect
    val xTicks: Ticks

    val yLabelRect: Rect
    val yTicks: Ticks

    fun buildLabels(
        chartSize: IntSize,
        chartData: Map<InputContainer<IndicatorCandle>, Map<Key<IndicatorCandle, *>, List<PortValue<*>>>>
    )
}

class LabelSizeHolderImpl(
    private val chart: Chart<IndicatorCandle>,
) : LabelSizeHolder {

    override var xLabelRect: Rect = Rect.Zero
    override var xTicks: Ticks = Ticks()
    override var yLabelRect: Rect = Rect.Zero
    override var yTicks: Ticks = Ticks()

    override fun buildLabels(
        chartSize: IntSize,
        chartData: Map<InputContainer<IndicatorCandle>, Map<Key<IndicatorCandle, *>, List<PortValue<*>>>>
    ) {
        val xKeyRange = chartData.keys.run { minOf { it.input.x.toFloat() }..maxOf { it.input.x.toFloat() } }
        val xIdxRange = chartData.keys.run { minOf { it.idx }..maxOf { it.idx } }
        val yValueRange = chartData.values.run {
            minOf { it.values.minOf { v -> v.minOf { m -> m.value.yRange.start } } }..
                    maxOf { it.values.minOf { v -> v.minOf { m -> m.value.yRange.start } } }
        }

        xTicks = chart.xTicks(xIdxRange.toInt(), xKeyRange.toDouble(), (chartSize.width / 75))
        yTicks = chart.yTicks(yValueRange, 15)
    }
}

fun LongRange.toInt() = start.toInt()..endInclusive.toInt()