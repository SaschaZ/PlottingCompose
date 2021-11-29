@file:Suppress("FunctionName")

package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.zieger.plottingcompose.PlotSeries

interface IPlotDrawScope : IPlotParameterScope, DrawScope {
    val allSeries: SnapshotStateList<PlotSeries<*, *>>
    val allItems get() = allSeries.flatMap { it.items }
    val allX get() = allSeries.flatMap { it.items }.map { it.item.x }
    val allY get() = allSeries.flatMap { it.items }.flatMap { it.item.y.values }.filterNotNull()
}


fun PlotDrawScope(
    plotParameterScope: IPlotParameterScope,
    drawScope: DrawScope,
    allSeries: SnapshotStateList<PlotSeries<*, *>>
): IPlotDrawScope =
    object : IPlotDrawScope, IPlotParameterScope by plotParameterScope, DrawScope by drawScope {
        override val allSeries: SnapshotStateList<PlotSeries<*, *>> = allSeries
    }
