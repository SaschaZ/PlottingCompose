package dev.zieger.plottingcompose.scopes

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.zieger.plottingcompose.definition.ChartDefinition
import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.processor.ProcessingScope


interface IChartDrawScope<T : Input> : DrawScope, IChartEnvironment {
    val definition: ChartDefinition<T>
    val scopes: List<Pair<Long, ProcessingScope<T>>>

    val rootRect: Rect
    val chartRect: Rect
}

fun <T : Input> ChartDrawScope(
    definition: ChartDefinition<T>,
    drawScope: DrawScope,
    scopes: List<Pair<Long, ProcessingScope<T>>>,
    chartEnvironment: IChartEnvironment,
): IChartDrawScope<T> = object : IChartDrawScope<T>,
    DrawScope by drawScope,
    IChartEnvironment by chartEnvironment {

    override val definition: ChartDefinition<T> = definition
    override val scopes: List<Pair<Long, ProcessingScope<T>>> = scopes

    override val rootRect: Rect = Rect(0f, 0f, chartSize.value.width.toFloat(), chartSize.value.height.toFloat())
    override val chartRect: Rect = rootRect.run {
        Rect(
            left + definition.margin.left.invoke(chartSize.value).value,
            top + definition.margin.top.invoke(chartSize.value).value,
            right - definition.margin.right.invoke(chartSize.value).value,
            bottom - definition.margin.bottom.invoke(chartSize.value).value
        )
    }
}