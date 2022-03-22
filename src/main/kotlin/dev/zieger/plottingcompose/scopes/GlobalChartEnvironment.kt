package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

interface IGlobalChartEnvironment {
    val chartSize: MutableState<IntSize>
    val translation: MutableState<Offset>
    val scale: MutableState<Pair<Float, Float>>
    val scaleCenter: MutableState<Offset>
    val mousePosition: MutableState<Offset?>
}

interface IChartEnvironment : IGlobalChartEnvironment {
    val translationOffset: MutableState<Offset>
    val finalTranslation: Offset get() = translation.value + translationOffset.value
}

data class GlobalChartEnvironment(
    override val chartSize: MutableState<IntSize> = mutableStateOf(IntSize.Zero),
    override val translation: MutableState<Offset> = mutableStateOf(Offset.Zero),//(-20000f, 0f)),
    override val scale: MutableState<Pair<Float, Float>> = mutableStateOf(1f to 1f),
    override val scaleCenter: MutableState<Offset> = mutableStateOf(Offset.Zero),
    override val mousePosition: MutableState<Offset?> = mutableStateOf(null)
) : IGlobalChartEnvironment

data class ChartEnvironment(
    val globalChartEnvironment: IGlobalChartEnvironment,
    override val translationOffset: MutableState<Offset> = mutableStateOf(Offset.Zero)
) : IChartEnvironment, IGlobalChartEnvironment by globalChartEnvironment