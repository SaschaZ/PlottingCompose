package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.zieger.plottingcompose.definition.ChartDefinition
import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.processor.ProcessingScope


interface IChartDrawScope<T : Input> : DrawScope, IChartEnvironment, IDrawScopeRegionHolder {
    val definition: ChartDefinition<T>
    val scopes: SnapshotStateList<ProcessingScope<T>>

    val rootRect: Rect
    val chartRect: Rect
}

fun <T : Input> ChartDrawScope(
    definition: ChartDefinition<T>,
    drawScope: DrawScope,
    scopes: SnapshotStateList<ProcessingScope<T>>,
    chartEnvironment: IChartEnvironment,
    regionHolder: DrawScopeRegionHolder = DrawScopeRegionHolder(),
    drawScopeWrapper: DrawScopeWrapper = DrawScopeWrapper(drawScope, regionHolder),
): IChartDrawScope<T> = object : IChartDrawScope<T>,
    DrawScope by drawScopeWrapper,
    IDrawScopeRegionHolder by regionHolder,
    IChartEnvironment by chartEnvironment {

    override val definition: ChartDefinition<T> = definition
    override val scopes: SnapshotStateList<ProcessingScope<T>> = scopes

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