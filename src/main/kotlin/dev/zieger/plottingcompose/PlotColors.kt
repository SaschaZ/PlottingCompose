package dev.zieger.plottingcompose

import androidx.compose.ui.graphics.Color

interface IPlotColors {
    val background: Color
    val border: Color
    val grid: Color
    val axisTicks: Color
    val axisLabels: Color
}

data class PlotColors(
    override val background: Color = Color.White,
    override val border: Color = Color.Black,
    override val grid: Color = Color.LightGray,
    override val axisTicks: Color = Color.Black,
    override val axisLabels: Color = Color.Black,
) : IPlotColors