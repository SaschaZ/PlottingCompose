package dev.zieger.plottingcompose

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.zieger.plottingcompose.scopes.IPlotParameterScope
import dev.zieger.plottingcompose.scopes.IPlotScope
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat


interface IParameter {

    val horizontalPadding: IPlotScope.() -> Dp
    val verticalPadding: IPlotScope.() -> Dp
    val horizontalPlotPadding: IPlotScope.() -> Dp
    val verticalPlotPadding: IPlotScope.() -> Dp

    val gridStrokeWidth: IPlotScope.() -> Float

    val plotYTicks: IPlotParameterScope.(items: List<Float>) -> List<Pair<Number, String>>
    val plotXTicks: IPlotParameterScope.(items: List<Float>) -> List<Pair<Number, String>>
    val plotYLabelWidth: IPlotScope.() -> Dp
    val plotXLabelHeight: IPlotScope.() -> Dp
    val plotTickLength: IPlotScope.() -> Dp
    val plotTickWidth: IPlotScope.() -> Float

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
}

enum class Axis { X, Y, BOTH }
enum class ScrollAction { SCALE, X_TRANSLATION, WIDTH_FACTOR }

data class PlotParameter(
    override val horizontalPadding: IPlotScope.() -> Dp = { plotSize.value.width.dp * 0.05f },
    override val verticalPadding: IPlotScope.() -> Dp = { plotSize.value.height.dp * 0.05f },
    override val horizontalPlotPadding: IPlotScope.() -> Dp = { plotSize.value.width.dp * 0.025f },
    override val verticalPlotPadding: IPlotScope.() -> Dp = { plotSize.value.height.dp * 0.025f },
    override val gridStrokeWidth: IPlotScope.() -> Float = { 1f / scale.value },
    override val plotYTicks: IPlotParameterScope.(List<Float>) -> List<Pair<Number, String>> = {
        defaultYTicks(
            it
        )
    },
    override val plotXTicks: IPlotParameterScope.(List<Float>) -> List<Pair<Number, String>> = {
        defaultXTicks(
            it
        )
    },
    override val plotTickLength: IPlotScope.() -> Dp = { 10.dp },
    override val plotTickWidth: IPlotScope.() -> Float = { 1f },
    override val plotYLabelWidth: IPlotScope.() -> Dp = { plotSize.value.width.dp * 0.15f },
    override val plotXLabelHeight: IPlotScope.() -> Dp = { plotSize.value.height.dp * 0.15f },
    override val drawYLabels: Boolean = true,
    override val drawXLabels: Boolean = true,
    override val drawYTicks: Boolean = true,
    override val drawXTicks: Boolean = true,
    override val drawGrid: Boolean = true,
    override val drawChartBorder: Boolean = true,
    override val enableScale: Boolean = true,
    override val enableTranslation: Boolean = true,
    override val focusAxis: Axis = Axis.BOTH,
    override val scrollAction: ScrollAction = ScrollAction.SCALE
) : IParameter

fun IPlotParameterScope.defaultYTicks(items: List<Float>): List<Pair<Number, String>> {
    if (items.isEmpty()) return emptyList()

    val vRange = items.minOf { it }..items.maxOf { it }
    val vHeight = vRange.run { endInclusive - start }
    val pHeight = plotSize.value.height / 60
    val tickHeight = vHeight / pHeight / scale.value
    if (tickHeight.isInfinite() || tickHeight.isNaN()) return emptyList()

    val scaledTickHeight = tickHeight.toBigDecimal().round(MathContext(1, RoundingMode.HALF_UP)).toFloat()
    return (-100..(vHeight / scaledTickHeight).toInt().plus(100) step 1)
        .map { vRange.start - vRange.start % scaledTickHeight + it * scaledTickHeight }
        .map { it to format(it) }
}

private val decimalFormat = DecimalFormat("##,###.###")
private fun format(value: Number): String = decimalFormat.format(value.toDouble())

fun IPlotScope.defaultXTicks(items: List<Float>): List<Pair<Number, String>> {
    if (items.isEmpty()) return emptyList()

    val vRange = items.minOf { it }..items.maxOf { it }
    val vWidth = vRange.run { endInclusive - start }
    val tickWidth = vWidth / 5 / scale.value / widthFactor.value
    if (tickWidth.isInfinite() || tickWidth.isNaN()) return emptyList()

    val tickWidthBd = tickWidth.toBigDecimal()
    val scaledTickWidth = tickWidthBd.round(MathContext(1, RoundingMode.HALF_UP)).toFloat()
    return (-100..(vWidth / scaledTickWidth).toInt().plus(100) step 1)
        .map { vRange.start - vRange.start % scaledTickWidth + it * scaledTickWidth }
        .map { it to format(it) }
}