package dev.zieger.plottingcompose.definition

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.zieger.plottingcompose.scopes.IGlobalChartEnvironment
import dev.zieger.plottingcompose.styles.PlotStyle
import org.jetbrains.skia.Typeface

class ChartDefinition<T : Input>(
    vararg chart: Chart<T>,
    val title: ChartTitle = ChartTitle(""),
    val margin: Margin = Margin({ 25.dp }, { 25.dp }),
    val backgroundColor: Color = Color.Black,
    val visibleArea: VisibleArea = VisibleArea()
) {
    val charts: List<Chart<T>> = chart.onEach { it.visibleArea = visibleArea }.toList()
}

data class VisibleArea(
    val relativeEnd: Float = 1f,
    val numData: NumData = NumData.All
)

sealed class NumData {
    data class Fixed(val numData: Int) : NumData()
    object All : NumData()
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
    val margin: Margin = Margin({ 0.dp }, { 20.dp }),
    val verticalWeight: Float = 1f,
    val tickLength: IntSize.() -> Dp = { 15.dp },
    val yTicks: IGlobalChartEnvironment.(yRange: ClosedRange<Double>) -> Map<Double, Set<String>> = {
        TickHelper.ticksY(it, chartSize.value.height, 50f)
    },
    val xTicks: IGlobalChartEnvironment.(idxRange: ClosedRange<Int>, xRange: ClosedRange<Double>) -> Map<Double, Set<String>> = { idxRange, xRange ->
        TickHelper.ticksX(xRange, chartSize.value.width, 100f, idxRange)
    },
    val drawYLabels: Boolean = true,
    val drawXLabels: Boolean = true,
    val backgroundColor: Color = Color(0xFF151924),
    val borderColor: Color = Color.DarkGray,
    val gridColor: Color = Color(0x11FFFFFF),
    val tickColor: Color = Color.Gray,
    val tickLabelColor: Color = Color.Gray
) {
    lateinit var visibleArea: VisibleArea
    val plots: List<PlotStyle<T>> = plot.toList()
}