@file:Suppress("FunctionName")

package dev.zieger.plottingcompose.scopes

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.zieger.plottingcompose.definition.Chart
import dev.zieger.plottingcompose.definition.ChartDefinition
import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.processor.ProcessingScope


interface IChartDrawScope<T : Input> : DrawScope {
    val definition: ChartDefinition<T>
    val scopes: Map<Long, ProcessingScope<T>>
    val chartEnvironment: Map<Chart<T>, IChartEnvironment>

    val IChartEnvironment.rootRect: Rect
    val IChartEnvironment.chartRect: Rect
}

fun <T : Input> ChartDrawScope(
    definition: ChartDefinition<T>,
    drawScope: DrawScope,
    scopes: Map<Long, ProcessingScope<T>>,
    chartEnvironment: Map<Chart<T>, IChartEnvironment>
): IChartDrawScope<T> = object : IChartDrawScope<T>,
    DrawScope by drawScope {

    override val definition: ChartDefinition<T> = definition
    override val scopes: Map<Long, ProcessingScope<T>> = scopes
    override val chartEnvironment: Map<Chart<T>, IChartEnvironment> = chartEnvironment

    override val IChartEnvironment.rootRect: Rect
        get() = Rect(0f, 0f, chartSize.value.width.toFloat(), chartSize.value.height.toFloat())

    override val IChartEnvironment.chartRect: Rect
        get() = rootRect.run {
            Rect(
                left + definition.margin.left.invoke(chartSize.value).value,
                top + definition.margin.top.invoke(chartSize.value).value,
                right - definition.margin.right.invoke(chartSize.value).value,
                bottom - definition.margin.bottom.invoke(chartSize.value).value
            )
        }
}