package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

interface IGlobalChartEnvironment {
    val chartSize: MutableState<IntSize>
    val translation: MutableState<Offset>
    val translationOffsetX: MutableState<Float>
    val scale: MutableState<Pair<Float, Float>>
    val scaleCenterRelative: MutableState<Offset>
    val scaleCenter: Offset
        get() = scaleCenterRelative.value * chartSize.value
    val mousePosition: MutableState<Offset?>
}

private operator fun Offset.times(other: IntSize): Offset =
    Offset(x * other.width, y * other.height)

interface IChartEnvironment : IGlobalChartEnvironment {
    val translationOffsetY: MutableState<Float>
    val finalTranslation: Offset
        get() = translation.value +
                Offset(translationOffsetX.value, translationOffsetY.value)
}

data class GlobalChartEnvironment(
    override val chartSize: MutableState<IntSize> = mutableStateOf(IntSize.Zero),
    override val translation: MutableState<Offset> = mutableStateOf(Offset.Zero),//(-20000f, 0f)),
    override val translationOffsetX: MutableState<Float> = mutableStateOf(0f),
    override val scale: MutableState<Pair<Float, Float>> = mutableStateOf(1f to 1f),
    override val scaleCenterRelative: MutableState<Offset> = mutableStateOf(Offset(0.8f, 0.5f)),
    override val mousePosition: MutableState<Offset?> = mutableStateOf(null)
) : IGlobalChartEnvironment

data class ChartEnvironment(
    val globalChartEnvironment: IGlobalChartEnvironment,
    override val translationOffsetY: MutableState<Float> = mutableStateOf(0f)
) : IChartEnvironment, IGlobalChartEnvironment by globalChartEnvironment