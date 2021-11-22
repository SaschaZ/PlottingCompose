@file:Suppress("FunctionName")

package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.zieger.plottingcompose.Series

interface IPlotDrawScope : IPlotParameterScope, DrawScope {
    val allSeries: SnapshotStateList<Series<*>>
}


fun PlotDrawScope(
    plotParameterScope: IPlotParameterScope,
    drawScope: DrawScope,
    allSeries: SnapshotStateList<Series<*>>
): IPlotDrawScope =
    object : IPlotDrawScope, IPlotParameterScope by plotParameterScope, DrawScope by drawScope {
        override val allSeries: SnapshotStateList<Series<*>> = allSeries
    }
