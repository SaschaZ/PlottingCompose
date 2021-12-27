package dev.zieger.plottingcompose.definition

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.zieger.plottingcompose.processor.ProcessingUnit
import dev.zieger.plottingcompose.styles.PlotStyle
import org.jetbrains.skia.Typeface

class ChartDefinition<T : Any>(
    val title: ChartTitle = ChartTitle(""),
    val margin: ChartMargin = ChartMargin(),
    vararg chart: Chart<T>
) {
    val charts: List<Chart<T>> = chart.toList()
}

fun <T : Any> ChartDefinition<T>.units(): List<ProcessingUnit<T>> = charts.flatMap { c -> c.plots.map { p -> p.unit } }
val ChartDefinition<*>.ports: List<PlotStyle> get() = charts.flatMap { c -> c.plots.flatMap { p -> p.styles } }

class ChartTitle(
    val text: String,
    val color: Color = Color.Black,
    val fontSize: Float = 30f,
    val typeface: Typeface = Typeface.makeDefault()
)

class ChartMargin(
    val left: Size.() -> Dp = { 0.dp },
    val top: Size.() -> Dp = { 0.dp },
    val right: Size.() -> Dp = { 0.dp },
    val bottom: Size.() -> Dp = { 0.dp }
) {
    constructor(
        left: Dp = 0.dp, top: Dp = 0.dp,
        right: Dp = 0.dp, bottom: Dp = 0.dp
    ) : this({ left }, { top }, { right }, { bottom })

    constructor(horizontal: Size.() -> Dp = { 0.dp }, vertical: Size.() -> Dp = { 0.dp }) :
            this(horizontal, vertical, horizontal, vertical)

    constructor(horizontal: Dp, vertical: Dp) : this({ horizontal }, { vertical })
}

class Chart<T : Any>(
    val verticalWeight: Float = 1f,
    vararg plot: Plot<T>
) {
    val plots: List<Plot<T>> = plot.toList()
}

class Plot<T : Any>(
    val unit: ProcessingUnit<T>,
    vararg style: PlotStyle
) {
    val styles: List<PlotStyle> = style.toList()
}