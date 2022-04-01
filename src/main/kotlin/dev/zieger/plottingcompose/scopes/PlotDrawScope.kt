@file:Suppress("FunctionName")

package dev.zieger.plottingcompose.scopes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.zieger.plottingcompose.InputContainer
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.utils.misc.nullWhen
import dev.zieger.utils.misc.whenNotNull
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine
import kotlin.math.absoluteValue

interface IPlotDrawScope<T : Input> : IChartDrawScope<T>, IChartEnvironment {
    val chart: Chart<T>
    val chartData: Map<InputContainer<T>, Map<Key<T, *>, List<PortValue<*>>>>

    val yLabelRect: Rect
    val yTickRect: Rect
    val xLabelRect: Rect
    val x1TickRect: Rect
    val x2TickRect: Rect
    val plotBorderRect: Rect
    val plotRect: Rect

    val yValueRange: ClosedRange<Double>?
    val xValueRange: ClosedRange<Double>

    val yTicks: Ticks
    val xTicks: Ticks

    val widthDivisor: Double

    val yLabelHeight: Float
    val xLabelFontSize: Float
    fun Offset.toScene(): Offset

    val visibleXPixelRange: ClosedRange<Double>
    val visibleYPixelRange: ClosedRange<Double>?
    var visibleItemXRange: ClosedRange<Float>
    val visibleXItemIndices: ClosedRange<Int>
    val rawXRange: ClosedRange<Float>
}

fun <T : Input> PlotDrawScope(
    chart: Chart<T>,
    chartDrawScope: IChartDrawScope<T>,
    chartEnvironment: IChartEnvironment,
    rect: Rect
): IPlotDrawScope<T> = object : IPlotDrawScope<T>, IChartDrawScope<T> by chartDrawScope,
    IChartEnvironment by chartEnvironment {

    override val chart: Chart<T> = chart

    override val plotBorderRect: Rect = rect.run {
        Rect(
            left, top, right - (if (chart.drawYLabels) yLabelWidth.value.toFloat() else 0f) -
                    chart.tickLength(chartSize.value).value / 2f,
            bottom - (if (chart.drawXLabels) xLabelHeight.value.toFloat() else 0f) -
                    (if (chart.drawXLabels) chart.tickLength(chartSize.value).value / 2f else 0f)
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

    override val widthDivisor: Double get() = finalScale.x

    override val visibleXPixelRange: ClosedRange<Double>
        get() = plotRect.run {
            -finalTranslation.x.toDouble()..-finalTranslation.x.toDouble() + width
        }

    private val rawData = scopes.chartData(chart)
    override val rawXRange: ClosedRange<Float>
        get() = rawData.nullWhen { it.isEmpty() }
            ?.run { minOf { it.key.idx / widthDivisor }.toFloat()..maxOf { it.key.idx / widthDivisor }.toFloat() }
            ?: 0f..0f

    override var visibleItemXRange: ClosedRange<Float> = 0f..0f
    override var visibleXItemIndices: ClosedRange<Int> = 0..0

    override val chartData: Map<InputContainer<T>, Map<Key<T, *>, List<PortValue<*>>>> = rawData.let { data ->
        if (data.size <= 2) return@let emptyMap()

        applyScaleRange(data.size, plotRect)
        applyScaleOffset(chart.visibleArea, data.size, plotRect.width)
        applyTranslationOffsetX(chart.visibleArea, rawXRange, plotRect, data.size, widthDivisor.toFloat())

        visibleXItemIndices =
            (data.entries.indexOfFirst { (input, _) -> input.idx / widthDivisor >= visibleXPixelRange.start }
                .takeIf { it >= 0 }
                ?.let { it - 5 }
                ?.coerceAtLeast(0) ?: 0)..0

        visibleXItemIndices =
            visibleXItemIndices.start..(data.entries.indexOfLast { (input, _) -> input.idx / widthDivisor <= visibleXPixelRange.endInclusive }
                .takeIf { it >= 0 } ?: 150)
                .coerceAtMost(data.size)

        visibleItemXRange = (visibleXItemIndices.start / widthDivisor).toFloat()..
                (visibleXItemIndices.endInclusive / widthDivisor).toFloat()

        data.entries.toList().subList(visibleXItemIndices)
            .associate { (k, v) -> k to v }.also { cd ->
                focusedItemIdx.value = findFocusedItemIdx(cd, visibleXItemIndices)
                    ?: focusedItemIdx.value.nullWhen { mousePosition.value?.let { rootRect.contains(it) } != true }
            }
    }

    private fun findFocusedItemIdx(
        data: Map<InputContainer<T>, Map<Key<T, *>, List<PortValue<*>>>>,
        idxRange: ClosedRange<Int>
    ): FocusedInfo? =
        mousePosition.value?.takeIf { plotRect.contains(it) }?.let { mp ->
            val relX = mp.x - plotRect.left - finalTranslation.x
            val d = data.entries.toList()
            d.minByOrNull { (it.key.idx / widthDivisor - relX).absoluteValue }
                ?.let { (input, _) ->
                    FocusedInfo(
                        input.idx,
                        (input.idx / widthDivisor + finalTranslation.x).toFloat(),
                        idxRange
                    )
                }
        }

    override val xValueRange: ClosedRange<Double> = chartData.xValueRange()
    override val xTicks: Ticks = chart.xTicks(this,
        visibleXItemIndices,
        rawData.minOf { it.key.input.x.toDouble() }..rawData.maxOf { it.key.input.x.toDouble() },
        (10 / scale.value.x).toInt()
    )

    override val xLabelFontSize: Float = run {
        fun xTicksFontSize(fontSize: Float): Float =
            xTicks.ticks.values.toList().nullWhenEmpty()?.run { get(size / 2) }?.maxOf { it.size(fontSize).width } ?: 0f

        val numVisible = xTicks.ticks.count { it.key / widthDivisor in visibleXPixelRange }
        var fontSize = 20f
        if (numVisible > 0)
            while (xTicksFontSize(fontSize) > plotRect.width / numVisible) fontSize -= 0.5f
        fontSize
    }.also { fontSize ->
        xLabelHeight.value = xTicks.ticks.values.toList().nullWhenEmpty()
            ?.run { get(size / 2) }?.sumOf { it.size(fontSize).height.toDouble() } ?: 0.0
    }

    override val yValueRange: ClosedRange<Double>? = chartData.yValueRange()?.also {
        val marginRel = chart.margin.top(chartSize.value).value / chartSize.value.height +
                chart.margin.bottom(chartSize.value).value / chartSize.value.height
        heightDivisor.value = (it.range() * (1 + marginRel)) / plotRect.height
    }
    override val yTicks: Ticks = yValueRange?.let { yValueRange ->
        chart.yTicks(this, yValueRange).also { yTicks ->
            yLabelWidth.value = yTicks.ticks.values.maxByOrNull { it.maxOf { set -> set.length } }
                ?.maxOf { it.size(20f).width.toDouble() } ?: 0.0
        }
    } ?: Ticks()
    override val yLabelHeight = yTicks.ticks.values.maxByOrNull { set -> set.maxOf { it.length } }
        ?.maxOf { it.size(20f).height } ?: 0f

    override val yLabelRect: Rect = rect.run {
        Rect(
            right - (if (chart.drawYLabels) (yLabelWidth.value.toFloat()) else 0f), top, right,
            bottom - (if (chart.drawXLabels) xLabelHeight.value.toFloat() else 0f) -
                    chart.tickLength(chartSize.value).value / 2f
        )
    }
    override val yTickRect: Rect = yLabelRect.run {
        Rect(
            left - (if (chart.drawYLabels) chart.tickLength(chartSize.value).value else 0f), top, left, bottom
        )
    }
    override val xLabelRect: Rect = rect.run {
        Rect(
            left,
            bottom - (if (chart.drawXLabels) xLabelHeight.value.toFloat() else 0f),
            right - (if (chart.drawYLabels) yLabelWidth.value.toFloat() * 1.1f else 0f),
            bottom
        )
    }
    override val x1TickRect: Rect = rect.run {
        Rect(
            left, top - chart.tickLength(chartSize.value).value * 0.5f,
            right - (if (chart.drawYLabels) yLabelWidth.value.toFloat() else 0f),
            top + chart.tickLength(chartSize.value).value * 0.5f
        )
    }
    override val x2TickRect: Rect = xLabelRect.run {
        Rect(
            left, top - chart.tickLength(chartSize.value).value * if (chart.drawXLabels) 1f else 0.5f,
            right, top + chart.tickLength(chartSize.value).value * if (chart.drawXLabels) 0f else 0.5f
        )
    }

    override val visibleYPixelRange: ClosedRange<Double>? = plotRect.run {
        yValueRange?.run { start / heightDivisor.value.toFloat()..endInclusive / heightDivisor.value.toFloat() }
    }?.also { visibleYPixelRange ->
        applyTranslationOffsetY(chart, visibleYPixelRange, plotRect)
    }

    override fun Offset.toScene(): Offset =
        Offset((plotRect.left + x / widthDivisor).toFloat(), plotRect.bottom - y / heightDivisor.value.toFloat())

    private fun <I : Input> Map<InputContainer<I>, Map<Key<I, *>, List<PortValue<*>>>>.yValueRange(): ClosedRange<Double>? {
        if (isEmpty() || flatMap { it.value.values }.isEmpty()) return 0.0..0.0

        val yMin = minOfOrNull {
            it.value.minOfOrNull { i2 ->
                i2.value.filter { (port, _) -> port.includeIntoScaling }.minOfOrNull { (_, value) ->
                    value.yRange.start
                } ?: Double.MAX_VALUE
            } ?: Double.MAX_VALUE
        }
        val yMax = maxOfOrNull {
            it.value.maxOfOrNull { i2 ->
                i2.value.filter { (port, _) -> port.includeIntoScaling }.maxOfOrNull { (_, value) ->
                    value.yRange.endInclusive
                } ?: Double.MIN_VALUE
            } ?: Double.MIN_VALUE
        }
        return whenNotNull(yMin, yMax) { min, max -> min..max }
    }

    private fun <I : Input> Map<InputContainer<I>, Map<Key<I, *>, List<PortValue<*>>>>.xValueRange(): ClosedRange<Double> {
        if (isEmpty()) return 0.0..0.0

        val xMin = minOf { it.key.input.x.toDouble() }
        val xMax = maxOf { it.key.input.x.toDouble() }
        return xMin..xMax
    }
}

private fun <E> List<E>.subList(range: ClosedRange<Int>): List<E> =
    subList(range.start, range.endInclusive)

private operator fun Offset.div(size: Rect): Offset =
    Offset(x / size.width, y / size.height)

private operator fun ClosedRange<Float>.minus(value: Float): ClosedRange<Float> =
    start - value..endInclusive - value

fun <T> ClosedRange<T>.range() where T : Comparable<T>, T : Number = endInclusive.toDouble() - start.toDouble()

fun <E, C : Collection<E>> C.nullWhenEmpty(): C? = ifEmpty { null }

private fun <I : Input> Map<Long, ProcessingScope<I>>.chartData(chart: Chart<I>): Map<InputContainer<I>, Map<Key<I, *>, List<PortValue<*>>>> {
    val slots = chart.plots.flatMap { it.slots }
    val keys = slots.map { it.key }
    val ports = slots.map { it.port }
    return entries.associate { (idx, scope) ->
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