package dev.zieger.plottingcompose.definition

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.zieger.plottingcompose.scopes.IGlobalChartEnvironment
import dev.zieger.plottingcompose.scopes.range
import dev.zieger.plottingcompose.styles.PlotStyle
import dev.zieger.utils.time.toTime
import org.jetbrains.skia.Typeface
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat

class ChartDefinition<T : Input>(
    vararg chart: Chart<T>,
    val title: ChartTitle = ChartTitle(""),
    val margin: Margin = Margin({ 25.dp }, { 25.dp }),
    val backgroundColor: Color = Color.Black
) {
    val charts: List<Chart<T>> = chart.toList()
}

fun <I : Input> ChartDefinition<I>.keys(): List<Key<I, *>> =
    charts.flatMap { c -> c.plots.flatMap { it.slots.map { s -> s.key } } }

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

class Chart<T : Input>(
    vararg plot: PlotStyle<T>,
    val margin: Margin = Margin({ 0.dp }, { 0.dp }),
    val verticalWeight: Float = 1f,
    val tickLength: IntSize.() -> Dp = { 15.dp },
    val yTicks: IGlobalChartEnvironment.(yRange: ClosedRange<Double>) -> Map<Double, String> = {
        TickHelper.ticksY(it, chartSize.value.height, 30f)
    },
    val xTicks: IGlobalChartEnvironment.(idxRange: ClosedRange<Int>, xRange: ClosedRange<Double>) -> Map<Double, String> = { idxRange, xRange ->
        TickHelper.ticksY(idxRange.run { start.toDouble()..endInclusive.toDouble() }, chartSize.value.width, 100f)
    },
    val drawYLabels: Boolean = true,
    val drawXLabels: Boolean = true,
    val backgroundColor: Color = Color(0xFF151924),
    val borderColor: Color = Color.DarkGray,
    val gridColor: Color = Color(0x11FFFFFF),
    val tickColor: Color = Color.Gray,
    val tickLabelColor: Color = Color.Gray
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

    fun ticksY(
        valueRange: ClosedRange<Double>,
        chartSize: Int,
        tickLength: Float = 30f
    ): Map<Double, String> {
        val valueLength = valueRange.range()
        val pSize = chartSize / tickLength
        val tickHeight = valueLength / pSize
        return if (!tickHeight.isInfinite() && !tickHeight.isNaN()) {
            val scaledTickLength = tickHeight.toBigDecimal().round(MathContext(1, RoundingMode.HALF_UP)).toFloat()
            (-100..(valueLength / scaledTickLength).toInt().plus(100) step 1)
                .map { valueRange.start - valueRange.start % scaledTickLength + it * scaledTickLength }
                .associateWith { format(it) }
        } else emptyMap()
    }

    fun ticksX(
        valueRange: ClosedRange<Double>,
        chartSize: Int,
        tickLength: Float = 30f,
        idxRange: ClosedRange<Int>
    ): Map<Double, String> {
        val valueLength = valueRange.range()
        val valueRelativeSize = chartSize / tickLength
        val valueTickHeight = valueLength / valueRelativeSize

        val idxLength = idxRange.range()
        val idxRelativeSize = chartSize / idxLength
        val idxTickHeight = idxLength / idxRelativeSize

        return if (!valueTickHeight.isInfinite() && !valueTickHeight.isNaN() &&
            !idxTickHeight.isInfinite() && !idxTickHeight.isNaN()
        ) {
            val scaledValueTickLength =
                valueTickHeight.toBigDecimal().round(MathContext(1, RoundingMode.HALF_UP)).toFloat()
            val valueTicks = (-100..(valueLength / scaledValueTickLength).toInt().plus(100) step 1).toList()
            val scaledIdxTickLength = idxTickHeight.toBigDecimal().round(MathContext(1, RoundingMode.HALF_UP)).toFloat()
            (-100..(idxLength / scaledIdxTickLength).toInt().plus(100) step 1)
                .mapIndexed { idx, _ ->
                    valueRange.start - valueRange.start % scaledValueTickLength + (valueTicks.getOrNull(
                        idx
                    ) ?: 1) * scaledValueTickLength
                }
                .associateWith { it.toTime().formatTime() }
        } else emptyMap()
    }
}