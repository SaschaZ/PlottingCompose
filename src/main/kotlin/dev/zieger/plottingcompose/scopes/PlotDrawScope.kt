@file:Suppress("FunctionName")

package dev.zieger.plottingcompose.scopes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.x
import dev.zieger.plottingcompose.y
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine

interface IPlotDrawScope<T : InputContainer> : IChartDrawScope<T> {
    val chart: Chart<T>
    val chartData: Map<T, Map<Key<T>, Map<Port<*>, ValueHolder?>>>

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

    val visibleXValueRange: ClosedRange<Float>
    val visibleYValueRange: ClosedRange<Float>
    val visibleXPixelRange: ClosedRange<Float>
    val visibleYPixelRange: ClosedRange<Float>
}

fun <T : InputContainer> PlotDrawScope(
    chart: Chart<T>,
    chartDrawScope: IChartDrawScope<T>
): IPlotDrawScope<T> = object : IPlotDrawScope<T>, IChartDrawScope<T> by chartDrawScope {
    override val chart: Chart<T> = chart
    override val chartData: Map<T, Map<Key<T>, Map<Port<*>, ValueHolder?>>> = scopes.chartData(chart)

    override val xValueRange: ClosedRange<Float> = chartData.xValueRange()
    override val xTicks: Map<Float, String> = chart.xTicks(this, xValueRange)
    override val xLabelWidth = xTicks.values.toList().nullWhenEmpty()?.run { get(size / 2) }?.size(20f)?.width ?: 0f
    override val xLabelHeight = xTicks.values.toList().nullWhenEmpty()?.run { get(size / 2) }?.size(20f)?.height ?: 0f

    override val yValueRange: ClosedRange<Float> get() = chartData.yValueRange()
    override val yTicks: Map<Float, String> = chart.yTicks(this, yValueRange)
    override val yLabelWidth = yTicks.values.maxByOrNull { it.length }?.size(20f)?.width ?: 0f
    override val yLabelHeight = yTicks.values.maxByOrNull { it.length }?.size(20f)?.height ?: 0f

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

    override val visibleXPixelRange: ClosedRange<Float>
        get() = plotRect.run {
            ((left - translation.value.x + scaleCenter.value.x * (scale.value.x - 1f)) / scale.value.x).coerceIn(left..right)..
                    ((right - translation.value.x + scaleCenter.value.x * (scale.value.x - 1f)) / scale.value.x).coerceIn(
                        left..right
                    )
        }
    private val yPixelOffset: Float
        get() = visibleYValueRange.start / heightDivisor
    override val visibleYPixelRange: ClosedRange<Float>
        get() = plotRect.run {
            ((top - translation.value.y + scaleCenter.value.y * (scale.value.y - 1f)) / scale.value.y + yPixelOffset).coerceIn(
                top..bottom
            )..
                    ((bottom - translation.value.y + scaleCenter.value.y * (scale.value.y - 1f)) / scale.value.y + yPixelOffset).coerceIn(
                        top..bottom
                    )
        }

    override val visibleXValueRange: ClosedRange<Float>
        get() = regions.entries.filter { (idx, r) -> idx >= 0 && r.any { it.isInsideXRange(visibleXPixelRange) } }
            .let { (it.minOfOrNull { (key, _) -> key } ?: 0)..(it.maxOfOrNull { (key, _) -> key } ?: 0) }
            .let {
                chartData.entries.toList().subList(it.first, it.last + 1)
                    .associate { (key, value) -> key to value }
            }
            .xValueRange()
    override val visibleYValueRange: ClosedRange<Float>
        get() = chartData.yValueRange(visibleXValueRange)

    override val heightDivisor: Float get() = yValueRange.run { endInclusive - start }.toFloat() / plotRect.height
    override val widthDivisor: Float get() = xValueRange.run { endInclusive - start }.toFloat() / plotRect.width
    override fun Offset.toScene(): Offset = Offset(x / widthDivisor, y / heightDivisor)
}

fun <E, C : Collection<E>> C.nullWhenEmpty(): C? = ifEmpty { null }

data class ValueHolder(val value: Value, val visible: Boolean = false, val focused: Boolean = false)

private fun <T : InputContainer> List<ProcessingScope<T>>.chartData(chart: Chart<T>): Map<T, Map<Key<T>, Map<Port<*>, ValueHolder?>>> {
    val slots = chart.plots.flatMap { it.slots }
    val keys = slots.map { it.key }
    val ports = slots.map { it.port }
    return associate {
        it.value to it.data.filterKeys { k -> k in keys }
            .map { (key, value) ->
                key to value.filterKeys { port -> port in ports }
                    .map { (port, v) -> port to v?.let { ValueHolder(v) } }.toMap()
            }.toMap()
    }
}

private fun <T : InputContainer> Map<T, Map<Key<T>, Map<Port<*>, ValueHolder?>>>.yValueRange(xValueRange: ClosedRange<Float>? = null): ClosedRange<Float> {
    val inRange = xValueRange?.let { filterKeys { it.x in xValueRange } } ?: this
    if (inRange.isEmpty()) return 0f..0f

    val yMin = inRange.minOf {
        it.value.minOf { i2 ->
            i2.value.filterKeys { k -> k.includeIntoScaling }.values.minOf { it2 ->
                (it2?.value as? Number)?.toDouble()
                    ?: (it2?.value as? ValueContainer)?.yRange?.start
                    ?: Double.MAX_VALUE
            }
        }
    }.toFloat()
    val yMax = inRange.maxOf {
        it.value.maxOf { i2 ->
            i2.value.filterKeys { k -> k.includeIntoScaling }.values.maxOf { it2 ->
                (it2?.value as? Number)?.toDouble()
                    ?: (it2?.value as? ValueContainer)?.yRange?.endInclusive
                    ?: Double.MIN_VALUE
            }
        }
    }.toFloat()
    return yMin..yMax
}

private fun <T : InputContainer> Map<T, Map<Key<T>, Map<Port<*>, ValueHolder?>>>.xValueRange(): ClosedRange<Float> {
    if (isEmpty()) return 0f..0f

    val xMin = minOf { it.key.x }.toFloat()
    val xMax = maxOf { it.key.x }.toFloat()
    return xMin..xMax
}

fun String.size(fontSize: Float): TextLine {
    val font = Font(null, fontSize)
    return TextLine.make(this, font)
}