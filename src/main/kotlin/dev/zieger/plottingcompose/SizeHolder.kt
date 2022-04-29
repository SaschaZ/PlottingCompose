package dev.zieger.plottingcompose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.indicators.candles.IndicatorCandle

interface GlobalSizeHolder {

    val chartSize: MutableState<IntSize>
    var yLabelWidth: MutableState<Float>

    val rootRect: Rect
    val rootBorderRect: Rect
}

class GlobalSizeHolderImpl(
    private val definition: ChartDefinition<IndicatorCandle>
) : GlobalSizeHolder {

    override val chartSize = mutableStateOf(IntSize.Zero)
    override var yLabelWidth = mutableStateOf(0f)

    override val rootRect: Rect
        get() = Rect(Offset.Zero, chartSize.value.toSize())
    override val rootBorderRect: Rect
        get() = rootRect.run {
            Rect(
                left + definition.margin.left.invoke(chartSize.value).value,
                top + definition.margin.top.invoke(chartSize.value).value,
                right - definition.margin.right.invoke(chartSize.value).value,
                bottom - definition.margin.bottom.invoke(chartSize.value).value
            )
        }
}

interface SizeHolder : GlobalSizeHolder {

    val chartRect: Rect
    val plotRect: Rect

    val xLabelRect: Rect
    val xLabelFontSize: Float
    val xTicks: State<Ticks>

    val yLabelRect: Rect
    val yLabelFontSize: Float
    val yTicks: State<Ticks>

    val yTicksRect: Rect
    val x1TickRect: Rect
    val x2TickRect: Rect

    fun buildLabels(
        chartSize: IntSize,
        chartData: Map<InputContainer<IndicatorCandle>, Map<Key<IndicatorCandle, *>, List<PortValue<*>>>>
    )
}

class SizeHolderImpl(
    private val chart: Chart<IndicatorCandle>,
    globalSizeHolder: GlobalSizeHolder
) : SizeHolder,
    GlobalSizeHolder by globalSizeHolder {

    override val chartRect: Rect
        get() = rootBorderRect.run {
            val chartTop = top + chart.definition.charts.subList(0, chart.definition.charts.indexOf(chart))
                .sumOf { it.verticalWeight.toDouble() }.toFloat() * height
            copy(
                top = chartTop,
                right = right - (if (chart.drawYLabels) yLabelRect.width else 0f) -
                        (if (chart.drawYLabels) chart.tickLength(chartSize.value).value / 2f else 0f),
                bottom = chartTop - height * chart.verticalWeight - (if (chart.drawXLabels) xLabelRect.height else 0f) -
                        (if (chart.drawXLabels) chart.tickLength(chartSize.value).value / 2f else 0f)
            )
        }

    override val plotRect: Rect
        get() = chartRect.run {
            Rect(
                left + chart.margin.left.invoke(chartSize.value).value,
                top + chart.margin.top.invoke(chartSize.value).value,
                right - chart.margin.right.invoke(chartSize.value).value,
                bottom - chart.margin.bottom.invoke(chartSize.value).value
            )
        }


    override var xTicks: MutableState<Ticks> = mutableStateOf(Ticks())
    override val xLabelRect
        get() = rootBorderRect.copy(top = rootBorderRect.bottom + 50f)
    override val xLabelFontSize: Float
        get() = xTicks.value.ticks.values.toList()[xTicks.value.ticks.size / 2].maxByOrNull { it.length }
            ?.let { longest ->
                var size = 21f
                while (size > 5 ||
                    longest.size(size).width > plotRect.width / 6 // TODO get visible tick amount
                ) size -= 0.5f
                size
            } ?: 1f

    override var yTicks: MutableState<Ticks> = mutableStateOf(Ticks())
    override val yLabelRect
        get() = rootBorderRect.copy(rootBorderRect.right - yLabelWidth.value)
    override val yLabelFontSize: Float
        get() = yTicks.value.ticks.values.lastOrNull()?.let { last ->
            var size = 21f
            while (size > 5f ||
                last.sumOf { it.size(size).height.toDouble() }
                    .toFloat() > yLabelRect.height / 15 // TODO get visible tick amount
            ) size -= 0.5f
            size
        } ?: 1f

    override val yTicksRect
        get() = yLabelRect.copy(
            yLabelRect.left - (if (chart.drawYLabels) chart.tickLength(chartSize.value).value else 0f)
        )
    override val x1TickRect: Rect = chartRect.run {
        Rect(
            left, top - chart.tickLength(chartSize.value).value * 0.5f,
            right - (if (chart.drawYLabels) yLabelWidth.value else 0f),
            top + chart.tickLength(chartSize.value).value * 0.5f
        )
    }
    override val x2TickRect: Rect = xLabelRect.run {
        Rect(
            left, top - chart.tickLength(chartSize.value).value * if (chart.drawXLabels) 1f else 0.5f,
            right, top + chart.tickLength(chartSize.value).value * if (chart.drawXLabels) 0f else 0.5f
        )
    }

    override fun buildLabels(
        chartSize: IntSize,
        chartData: Map<InputContainer<IndicatorCandle>, Map<Key<IndicatorCandle, *>, List<PortValue<*>>>>
    ) {
        if (chartData.isEmpty()) return

        val xKeyRange = chartData.keys.run { minOf { it.input.x.toFloat() }..maxOf { it.input.x.toFloat() } }
        val xIdxRange = chartData.keys.run { minOf { it.idx }..maxOf { it.idx } }
        val yValueRange = chartData.values.run {
            minOf { it.values.minOf { v -> v.minOf { m -> m.value.yRange.start } } }..
                    maxOf { it.values.minOf { v -> v.minOf { m -> m.value.yRange.start } } }
        }

        xTicks.value = chart.xTicks(xIdxRange.toInt(), xKeyRange.toDouble(), (chartSize.width / 75))
        yTicks.value = chart.yTicks(yValueRange, chartSize.height / 40)
        yLabelWidth.run {
            value = yTicks.value.ticks.run { entries.toList()[size - 1] }
                .value.maxOf { it.size(yLabelFontSize).width }
                .coerceAtLeast(value)
        }
    }
}

fun LongRange.toInt(): IntRange = IntRange(start.toInt(), endInclusive.toInt())
fun ClosedRange<Float>.toDouble() = start.toDouble()..endInclusive.toDouble()