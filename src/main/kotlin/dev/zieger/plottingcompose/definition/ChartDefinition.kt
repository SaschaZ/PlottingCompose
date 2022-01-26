package dev.zieger.plottingcompose.definition

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.zieger.plottingcompose.scopes.IChartEnvironment
import dev.zieger.plottingcompose.styles.PlotStyle
import dev.zieger.plottingcompose.x
import dev.zieger.plottingcompose.y
import org.jetbrains.skia.Typeface
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat

class ChartDefinition<T : InputContainer>(
    vararg chart: Chart<T>,
    val title: ChartTitle = ChartTitle(""),
    val margin: Margin = Margin(0.05f, 0.05f),
    val backgroundColor: Color = Color.Black
) {
    val charts: List<Chart<T>> = chart.toList()
}

fun <T : InputContainer> ChartDefinition<T>.keys(): List<Key<T>> =
    charts.flatMap { c -> c.plots.flatMap { it.slots.map { s -> s.key } } }

val ChartDefinition<*>.ports: List<Port<*>> get() = charts.flatMap { c -> c.plots.flatMap { p -> p.slots.map { s -> s.port } } }

class ChartTitle(
    val text: String,
    val color: Color = Color.Black,
    val fontSize: Float = 30f,
    val typeface: Typeface = Typeface.makeDefault()
)

class Margin(
    val left: IntSize.() -> Dp = { 0.dp },
    val top: IntSize.() -> Dp = { 0.dp },
    val right: IntSize.() -> Dp = { 0.dp },
    val bottom: IntSize.() -> Dp = { 0.dp }
) {
    constructor(
        left: Float = 0f, top: Float = 0f,
        right: Float = 0f, bottom: Float = 0f
    ) : this({ (width * left).dp }, { (height * top).dp }, { (width * right).dp }, { (height * bottom).dp })

    constructor(horizontal: IntSize.() -> Dp = { 0.dp }, vertical: IntSize.() -> Dp = { 0.dp }) :
            this(horizontal, vertical, horizontal, vertical)

    constructor(horizontal: Float, vertical: Float) : this({ (width * horizontal).dp }, { (height * vertical).dp })
}

class Chart<T : InputContainer>(
    vararg plot: PlotStyle<T>,
    val margin: Margin = Margin(0.005f, 0.005f),
    val verticalWeight: Float = 1f,
    val tickLength: IntSize.() -> Dp = { 15.dp },
    val yTicks: IChartEnvironment.(yRange: ClosedRange<Float>) -> Map<Float, String> = {
        TickHelper.ticks(it, chartSize.value.height, scale.value.y, 30f)
    },
    val xTicks: IChartEnvironment.(xRange: ClosedRange<Float>) -> Map<Float, String> = {
        TickHelper.ticks(it, chartSize.value.width, scale.value.x, 100f)
    },
    val backgroundColor: Color = Color.DarkGray,
    val borderColor: Color = Color.White,
    val gridColor: Color = Color.Gray,
    val tickColor: Color = Color.White,
    val tickLabelColor: Color = Color.White
) {
    companion object {
        private val decimalFormat = DecimalFormat("##,###.###")
        private fun format(value: Number): String = decimalFormat.format(value.toDouble())
    }

    val plots: List<PlotStyle<T>> = plot.toList()
}

object TickHelper {

    private val decimalFormat = DecimalFormat("##,###.###")
    private fun format(value: Number): String = decimalFormat.format(value.toDouble())

    fun ticks(
        valueRange: ClosedRange<Float>,
        chartSize: Int,
        scale: Float,
        tickLength: Float = 30f
    ): Map<Float, String> {
        val valueLength = valueRange.run { endInclusive - start }
        val pSize = chartSize / tickLength
        val tickHeight = valueLength / pSize / scale
        return if (!tickHeight.isInfinite() && !tickHeight.isNaN()) {
            val scaledTickLength = tickHeight.toBigDecimal().round(MathContext(1, RoundingMode.HALF_UP)).toFloat()
            (-100..(valueLength / scaledTickLength).toInt().plus(100) step 1)
                .map { valueRange.start - valueRange.start % scaledTickLength + it * scaledTickLength }
                .associateWith { format(it) }
        } else emptyMap()
    }
}