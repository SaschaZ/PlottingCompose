package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.Composable
import dev.zieger.plottingcompose.IParameter
import dev.zieger.plottingcompose.IPlotColors


interface IPlotParameterScope : IPlotScope, IParameter, IPlotColors

@Composable
fun PlotParameterScope(plotScope: IPlotScope, parameter: IParameter, colors: IPlotColors): IPlotParameterScope =
    object : IPlotParameterScope, IPlotScope by plotScope, IPlotColors by colors, IParameter by parameter {}