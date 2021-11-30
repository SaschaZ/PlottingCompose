package dev.zieger.plottingcompose.scopes

import androidx.compose.ui.geometry.Offset
import dev.zieger.plottingcompose.PlotSeries

data class PlotHandler(
    val set: (PlotSeries<*, *>) -> Unit,
    val add: (PlotSeries<*, *>) -> Unit,
    val scale: (Float) -> Unit,
    val scaleListener: ((Float) -> Unit) -> Unit,
    val relativeScaleCenter: (Offset) -> Unit,
    val relativeScaleCenterListener: ((Offset) -> Unit) -> Unit,
    val translate: (Offset) -> Unit,
    val translateListener: ((Offset) -> Unit) -> Unit,
    val xStretch: (Float) -> Unit,
    val xStretchListener: ((Float) -> Unit) -> Unit,
    val xStretchCenter: (Offset) -> Unit,
    val xStretchCenterListener: ((Offset) -> Unit) -> Unit,
    val resetTransformations: () -> Unit
)