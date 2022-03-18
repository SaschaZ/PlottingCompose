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
import kotlin.math.absoluteValue

interface IPlotDrawScope<T : Input> : IChartDrawScope<T>, IChartEnvironment, IStates {
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

    val widthDivisor: Float

    val yLabelHeight: Float
    val xLabelWidth: Float
    fun Offset.toScene(): Offset

    val visibleXPixelRange: ClosedRange<Float>
    val visibleYPixelRange: ClosedRange<Float>
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

    override val widthDivisor: Float = scale.value.x * 10f

    override val visibleXPixelRange: ClosedRange<Float> = plotRect.run {
        -finalTranslation.x..-finalTranslation.x + width
    }

    override val chartData: Map<T, Map<Key<T>, List<PortValue<*>>>> = scopes.chartData(chart)
        .let { data ->
            if (data.size <= 2) return@let emptyMap()

            val start =
                data.entries.indexOfFirst { (key, _) -> key.x.toFloat() / widthDivisor >= visibleXPixelRange.start }
                    .takeIf { it >= 0 }
                    ?.let { it - 5 }
                    ?.coerceAtLeast(0) ?: 0

            val end =
                data.entries.indexOfLast { (key, _) -> key.x.toFloat() / widthDivisor <= visibleXPixelRange.endInclusive }
                    .takeIf { it >= 0 }
                    ?.let { it + 5 }
                    ?.coerceAtMost(data.size - 1) ?: 150

            data.entries.toList().subList(start, end)
                .associate { (k, v) -> k to v }.also { cd ->
                    focusedItemIdx.value = findFocusedItemIdx(cd)
                }
        }

    private fun findFocusedItemIdx(data: Map<T, Map<Key<T>, List<PortValue<*>>>>): FocusedInfo? =
        mousePosition.value?.takeIf { plotRect.contains(it) }?.let { mp ->
            val relX = mp.x - plotRect.left - translation.value.x
            val d = data.entries.toList()
            d.minByOrNull { (it.key.x.toFloat() / widthDivisor - relX).absoluteValue }
                ?.let { d.indexOf(it).takeIf { i -> i > 0 }?.let { idx -> it to idx } }
                ?.let { (item, idx) -> FocusedInfo(idx, item.key.x.toFloat() / widthDivisor + translation.value.x) }
        }

    override val xValueRange: ClosedRange<Float> = chartData.xValueRange()
    override val xTicks: Map<Float, String> = chart.xTicks(this, xValueRange).also { xTicks ->
        xLabelHeight.value =
            (xTicks.values.toList().nullWhenEmpty()
                ?.run { get(size / 2) }?.size(20f)?.height)?.toDouble() ?: 0.0
    }
    override val xLabelWidth = xTicks.values.toList().nullWhenEmpty()?.run { get(size / 2) }?.size(20f)?.width ?: 0f

    override val yValueRange: ClosedRange<Float> = chartData.yValueRange().also {
        heightDivisor.value = it.range() / plotRect.height
    }
    override val yTicks: Map<Float, String> = chart.yTicks(this, yValueRange).also { yTicks ->
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

    override val visibleYPixelRange: ClosedRange<Float> = plotRect.run {
        yValueRange.run { start / heightDivisor.value.toFloat()..endInclusive / heightDivisor.value.toFloat() }
    }

    override fun Offset.toScene(): Offset =
        Offset(plotRect.left + x / widthDivisor, plotRect.bottom - y / heightDivisor.value.toFloat())

    private fun <I : Input> Map<I, Map<Key<I>, List<PortValue<*>>>>.yValueRange(): ClosedRange<Float> {
        if (isEmpty() || flatMap { it.value.values }.isEmpty()) return 0f..0f

        val yMin = minOf {
            it.value.minOf { i2 ->
                i2.value.filter { (port, _) -> port.includeIntoScaling }.minOf { (_, value) ->
                    value.yRange.start
                }
            }
        }.toFloat()
        val yMax = maxOf {
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

fun String.size(fontSize: Float): TextLine {
    val font = Font(null, fontSize)
    return TextLine.make(this, font)
}