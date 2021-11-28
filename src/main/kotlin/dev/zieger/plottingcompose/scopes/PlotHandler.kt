package dev.zieger.plottingcompose.scopes

import androidx.compose.ui.geometry.Offset
import dev.zieger.plottingcompose.PlotSeries

data class PlotHandler(
    val set: (PlotSeries<*>) -> Unit,
    val add: (PlotSeries<*>) -> Unit,
    val scale: (Float) -> Unit,
    val translate: (Offset) -> Unit,
    val resetTransformations: () -> Unit
)