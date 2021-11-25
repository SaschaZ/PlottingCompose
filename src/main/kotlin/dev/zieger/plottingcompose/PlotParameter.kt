package dev.zieger.plottingcompose

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.zieger.plottingcompose.scopes.IPlotParameterScope
import dev.zieger.plottingcompose.scopes.IPlotScope
import java.lang.Integer.min
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat


interface IParameter {

    val horizontalPadding: Dp
    val verticalPadding: Dp
    val horizontalPlotPadding: Dp
    val verticalPlotPadding: Dp

    fun IPlotParameterScope.gridSize(items: List<SeriesItem<*>>): Dp
    val gridStrokeWidth: Float

    fun IPlotParameterScope.plotYTicks(items: List<SeriesItem<*>>): List<Pair<Number, String>>
    fun IPlotParameterScope.plotXTicks(items: List<SeriesItem<*>>): List<Pair<Number, String>>
    val plotLabelFontSize: Float
    val plotTickLength: Dp
    val plotTickWidth: Float

    val drawYLabels: Boolean
    val drawXLabels: Boolean
    val drawYTicks: Boolean
    val drawXTicks: Boolean
    val drawGrid: Boolean
    val drawChartBorder: Boolean

    val enableScale: Boolean
    val enableTranslation: Boolean

    val focusAxis: Axis

    val scrollAction: ScrollAction
    fun withPlotScope(plotScope: IPlotScope): IParameter
}

enum class Axis { X, Y, BOTH }
enum class ScrollAction { SCALE, X_TRANSLATION, WIDTH_FACTOR }

@Composable
fun PlotParameter(
    horizontalPadding: IPlotScope.() -> Dp = { min(plotSize.value.width, plotSize.value.height).dp * 0.05f },
    verticalPadding: IPlotScope.() -> Dp = { min(plotSize.value.width, plotSize.value.height).dp * 0.05f },
    horizontalPlotPadding: IPlotScope.() -> Dp = { min(plotSize.value.width, plotSize.value.height).dp * 0.025f },
    verticalPlotPadding: IPlotScope.() -> Dp = { min(plotSize.value.width, plotSize.value.height).dp * 0.025f },
    gridSize: IPlotParameterScope.(List<SeriesItem<*>>) -> Dp = { defaultGridSize(it) },
    gridStrokeWidth: IPlotScope.() -> Float = { 1f / scale.value },
    plotYTicks: IPlotParameterScope.(List<SeriesItem<*>>) -> List<Pair<Number, String>> = { defaultYTicks(it) },
    plotXTicks: IPlotParameterScope.(List<SeriesItem<*>>) -> List<Pair<Number, String>> = { defaultXTicks(it) },
    plotYLabelFontSize: IPlotScope.() -> Float = { 24f },
    plotTickLength: IPlotScope.() -> Dp = { 10.dp },
    plotTickWidth: IPlotScope.() -> Float = { 1f },
    drawYLabels: Boolean = true,
    drawXLabels: Boolean = true,
    drawYTicks: Boolean = true,
    drawXTicks: Boolean = true,
    drawGrid: Boolean = true,
    drawChartBorder: Boolean = true,
    enableScale: Boolean = true,
    enableTranslation: Boolean = true,
    focusAxis: Axis = Axis.BOTH,
    scrollAction: ScrollAction = ScrollAction.SCALE
) = object : IParameter {

    private lateinit var plotScope: IPlotScope

    override val horizontalPadding: Dp get() = horizontalPadding(plotScope)
    override val verticalPadding: Dp get() = verticalPadding(plotScope)
    override val horizontalPlotPadding: Dp get() = horizontalPlotPadding(plotScope)
    override val verticalPlotPadding: Dp get() = verticalPlotPadding(plotScope)

    override fun IPlotParameterScope.gridSize(items: List<SeriesItem<*>>): Dp = gridSize(items)
    override val gridStrokeWidth: Float get() = gridStrokeWidth(plotScope)

    override fun IPlotParameterScope.plotYTicks(items: List<SeriesItem<*>>): List<Pair<Number, String>> =
        plotYTicks(items)

    override fun IPlotParameterScope.plotXTicks(items: List<SeriesItem<*>>): List<Pair<Number, String>> =
        plotXTicks(items)

    override val plotLabelFontSize: Float get() = plotYLabelFontSize(plotScope)
    override val plotTickLength: Dp get() = plotTickLength(plotScope)
    override val plotTickWidth: Float get() = plotTickWidth(plotScope)

    override val drawYLabels: Boolean = drawYLabels
    override val drawXLabels: Boolean = drawXLabels
    override val drawYTicks: Boolean = drawYTicks
    override val drawXTicks: Boolean = drawXTicks
    override val drawGrid: Boolean = drawGrid
    override val drawChartBorder: Boolean = drawChartBorder

    override val enableScale: Boolean = enableScale
    override val enableTranslation: Boolean = enableTranslation

    override val focusAxis: Axis = focusAxis
    override val scrollAction: ScrollAction = scrollAction

    override fun withPlotScope(plotScope: IPlotScope): IParameter {
        this.plotScope = plotScope
        return this
    }
}

fun IPlotParameterScope.defaultGridSize(items: List<SeriesItem<*>>): Dp {
    if (items.isEmpty()) return 50.dp
    val vHeight = items.maxOf { it.yMax.toFloat() } - items.minOf { it.yMin.toFloat() }
    val hHeight = plotSize.value.height - verticalPadding.value * 2 - verticalPlotPadding.value * 2
    val heightFactor = vHeight / hHeight
    val tickHeight = vHeight / 10f
    if (tickHeight.isNaN() || tickHeight.isInfinite()) return 50.dp
    return (tickHeight / heightFactor).dp
}

fun IPlotParameterScope.defaultYTicks(items: List<SeriesItem<*>>): List<Pair<Number, String>> {
    if (items.isEmpty()) return emptyList()
    val vRange = items.minOf { it.yMin.toFloat() }..items.maxOf { it.yMax.toFloat() }
    val vHeight = vRange.run { endInclusive - start }
    val tickHeight = vHeight / 10 / scale.value
    if (tickHeight.isInfinite() || tickHeight.isNaN()) return emptyList()

    val scaledTickHeight = tickHeight.toBigDecimal().round(MathContext(1, RoundingMode.HALF_UP)).toFloat()
    return (-10..(vHeight / scaledTickHeight).toInt().plus(10) step 1)
        .map { vRange.start - vRange.start % scaledTickHeight + it * scaledTickHeight }
        .map { it to format(it) }
}

private val decimalFormat = DecimalFormat("##,###.###")
private fun format(value: Number): String = decimalFormat.format(value.toDouble())

fun IPlotScope.defaultXTicks(items: List<SeriesItem<*>>): List<Pair<Number, String>> {
    if (items.isEmpty()) return emptyList()
    val vRange = items.minOf { it.data.xMin.toLong() }..items.maxOf { it.data.xMax.toLong() }
    val vWidth = vRange.run { endInclusive - start }
    val tickWidth = vWidth / 5 / scale.value
    if (tickWidth.isInfinite() || tickWidth.isNaN()) return emptyList()

    val tickWidthBd = tickWidth.toBigDecimal()
    val scaledTickWidth = tickWidthBd.round(MathContext(1, RoundingMode.HALF_UP)).toFloat()
    return (-10..(vWidth / scaledTickWidth).toInt().plus(10) step 1)
        .map { vRange.first - vRange.first % scaledTickWidth + it * scaledTickWidth }
        .map { it to format(it) }
}