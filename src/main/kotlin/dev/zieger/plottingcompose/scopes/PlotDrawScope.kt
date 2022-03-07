@file:Suppress("FunctionName")

package dev.zieger.plottingcompose.scopes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.zieger.plottingcompose.definition.Chart
import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.x
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine

interface IPlotDrawScope<T : Input> : IChartDrawScope<T> {
    val chart: Chart<T>
    val chartData: Map<T, Map<Key<T>, List<PortValue<*>>>>

    val yLabelRect: Rect
    val yTickRect: Rect
    val xLabelRect: Rect
    val xTickRect: Rect
    val plotBorderRect: Rect
    val plotRect: Rect

    val yValueRange: ClosedRange<Float>
    val xValueRange: ClosedRange<Float>

    val yTicks: Map<Float, String>
    val xTicks: Map<Float, String>

    val heightDivisor: Float
    val widthDivisor: Float

    val yLabelWidth: Float
    val yLabelHeight: Float
    val xLabelHeight: Float
    val xLabelWidth: Float
    fun Offset.toScene(): Offset

    val visibleXPixelRange: ClosedRange<Float>
    val visibleYPixelRange: ClosedRange<Float>
}

fun <T : Input> PlotDrawScope(
    chart: Chart<T>,
    chartDrawScope: IChartDrawScope<T>
): IPlotDrawScope<T> = object : IPlotDrawScope<T>, IChartDrawScope<T> by chartDrawScope {
    override val chart: Chart<T> = chart

    override val chartData: Map<T, Map<Key<T>, List<PortValue<*>>>> = scopes.chartData(chart)
        .let { data ->
            val amount = 100
            val start = (finalTranslation.x / -(chartRect.width * 0.88f) * amount).toInt().coerceIn(0..data.size - 2)
            val range = start..(start + amount).coerceIn(start..data.size - 1)
            data.entries.toList().subList(range.first, range.last).associate { (k, v) -> k to v }
        }

    override val xValueRange: ClosedRange<Float> = chartData.xValueRange()
    override val xTicks: Map<Float, String> = chart.xTicks(this, xValueRange)
    override val xLabelWidth = xTicks.values.toList().nullWhenEmpty()?.run { get(size / 2) }?.size(20f)?.width ?: 0f
    override val xLabelHeight = xTicks.values.toList().nullWhenEmpty()?.run { get(size / 2) }?.size(20f)?.height ?: 0f

    override val yValueRange: ClosedRange<Float> = chartData.yValueRange()
    override val yTicks: Map<Float, String> = chart.yTicks(this, yValueRange)
    override val yLabelWidth = yTicks.values.maxByOrNull { it.length }?.size(20f)?.width ?: 0f
    override val yLabelHeight = yTicks.values.maxByOrNull { it.length }?.size(20f)?.height ?: 0f

    override val plotBorderRect: Rect = chartRect.run {
        Rect(
            left, top, right - yLabelWidth - chart.tickLength(chartSize.value).value / 2f,
            bottom - xLabelHeight - chart.tickLength(chartSize.value).value / 2f
        )
    }
    override val plotRect: Rect = plotBorderRect.run {
        Rect(
            left + chart.margin.left.invoke(chartSize.value).value,
            top + chart.margin.top.invoke(chartSize.value).value,
            right - chart.margin.right.invoke(chartSize.value).value,
            bottom - chart.margin.bottom.invoke(chartSize.value).value
        )
    }

    override val visibleXPixelRange: ClosedRange<Float> = plotRect.run {
        val dX = translation.value.x + scaleCenter.value.x * (scale.value.x - 1f)
        (left - dX) / scale.value.x..(right - dX) / scale.value.x
    }

    override val heightDivisor: Float = yValueRange.range().toFloat() / plotRect.height
    override val widthDivisor: Float = xValueRange.range().toFloat() / plotRect.width

    override val yLabelRect: Rect = chartRect.run {
        Rect(
            right - yLabelWidth, top, right, bottom - xLabelHeight - chart.tickLength(chartSize.value).value / 2f
        )
    }
    override val yTickRect: Rect = yLabelRect.run {
        Rect(
            left - chart.tickLength(chartSize.value).value, top, left, bottom
        )
    }
    override val xLabelRect: Rect = chartRect.run {
        Rect(
            left, bottom - xLabelHeight, right - yLabelWidth, bottom
        )
    }
    override val xTickRect: Rect = xLabelRect.run {
        Rect(
            left, top - chart.tickLength(chartSize.value).value, right, top
        )
    }

    override val visibleYPixelRange: ClosedRange<Float> =
        yValueRange.run { start / heightDivisor..endInclusive / heightDivisor }

    override fun Offset.toScene(): Offset = Offset(x / widthDivisor, y / heightDivisor)
}

private operator fun ClosedRange<Float>.minus(value: Float): ClosedRange<Float> =
    start - value..endInclusive - value

fun <T> ClosedRange<T>.range() where T : Comparable<T>, T : Number = endInclusive.toDouble() - start.toDouble()

fun <E, C : Collection<E>> C.nullWhenEmpty(): C? = ifEmpty { null }

private fun <I : Input> List<ProcessingScope<I>>.chartData(chart: Chart<I>): Map<I, Map<Key<I>, List<PortValue<*>>>> {
    val slots = chart.plots.flatMap { it.slots }
    val keys = slots.map { it.key }
    val ports = slots.map { it.port }
    return associate {
        it.input to it.data.filterKeys { k -> k in keys }
            .map { (key, value) ->
                key to value.filter { portValue -> portValue.port in ports }
            }.toMap()
    }
}

private fun <I : Input> Map<I, Map<Key<I>, List<PortValue<*>>>>.yValueRange(xValueRange: ClosedRange<Float>? = null): ClosedRange<Float> {
    val inRange = xValueRange?.let { filterKeys { it.x.toDouble() in xValueRange } } ?: this
    if (inRange.isEmpty() || inRange.flatMap { it.value.values }.isEmpty()) return 0f..0f

    val yMin = inRange.minOf {
        it.value.minOf { i2 ->
            i2.value.filter { (port, _) -> port.includeIntoScaling }.minOf { (_, value) ->
                value.yRange.start
            }
        }
    }.toFloat()
    val yMax = inRange.maxOf {
        it.value.maxOf { i2 ->
            i2.value.filter { (port, _) -> port.includeIntoScaling }.maxOf { (_, value) ->
                value.yRange.endInclusive
            }
        }
    }.toFloat()
    return yMin..yMax
}

private fun <T : Input> Map<T, Map<Key<T>, List<PortValue<*>>>>.xValueRange(): ClosedRange<Float> {
    if (isEmpty()) return 0f..0f

    val xMin = minOf { it.key.x.toDouble() }.toFloat()
    val xMax = maxOf { it.key.x.toDouble() }.toFloat()
    return xMin..xMax
}

fun String.size(fontSize: Float): TextLine {
    val font = Font(null, fontSize)
    return TextLine.make(this, font)
}