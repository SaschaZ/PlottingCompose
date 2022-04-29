package dev.zieger.plottingcompose.definition

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.styles.PlotStyle
import org.jetbrains.skia.Typeface
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope

class ChartDefinition<T : Input>(
    vararg chart: Chart<T>,
    val title: ChartTitle = ChartTitle(""),
    val margin: Margin = Margin({ 25.dp }, { 25.dp }),
    val backgroundColor: Color = Color.Black,
    val visibleArea: VisibleArea = VisibleArea()
) {
    val charts: List<Chart<T>> = chart.onEach {
        it.definition = this
        it.visibleArea = visibleArea
    }.toList()
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
    val yTicks: (yRange: ClosedRange<Double>, amount: Int) -> Ticks = { yRange, amount ->
        TickHelper.ticksY(yRange, amount)
    },
    val xTicks: (idxRange: ClosedRange<Int>, xRange: ClosedRange<Double>, amount: Int) -> Ticks = { idxRange, xRange, amount ->
        TickHelper.ticksX(xRange, amount, idxRange)
    },
    val drawYLabels: Boolean = true,
    val drawXLabels: Boolean = true,
    val backgroundColor: Color = Color(0xFF151924),
    val borderColor: Color = Color.DarkGray,
    val gridColor: Color = Color(0x11FFFFFF),
    val tickColor: Color = Color.Gray,
    val tickLabelColor: Color = Color.Gray
) : KoinScopeComponent {
    lateinit var definition: ChartDefinition<T>
    lateinit var visibleArea: VisibleArea
    val plots: List<PlotStyle<T>> = plot.toList()

    internal fun slot(key: Key<*, *>, port: Port<*>): Slot<*, *>? =
        plots.firstNotNullOfOrNull { p -> p.slots.firstOrNull { it.key == key && it.port == port } }

    override lateinit var scope: Scope
}