package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

interface IChartEnvironment {
    val chartSize: MutableState<IntSize>
    val translation: MutableState<Offset>
    val translationOffset: MutableState<Offset>
    val finalTranslation: Offset get() = translation.value + translationOffset.value
    val scale: MutableState<Pair<Float, Float>>
    val scaleOffset: MutableState<Pair<Float, Float>>
    val finalScale: Pair<Float, Float>
        get() = scale.value.let { (x, y) ->
            scaleOffset.value.let { (xO, yO) -> x + xO to y + yO }
        }
    val scaleCenter: MutableState<Offset>
    val mousePosition: MutableState<Offset?>
    val xStretchFactor: MutableState<Float>
    val xStretchCenter: MutableState<Offset>
}

data class ChartEnvironment(
    override val chartSize: MutableState<IntSize> = mutableStateOf(IntSize.Zero),
    override val translation: MutableState<Offset> = mutableStateOf(Offset.Zero),//(-20000f, 0f)),
    override val translationOffset: MutableState<Offset> = mutableStateOf(Offset.Zero),
    override val scale: MutableState<Pair<Float, Float>> = mutableStateOf(1f to 1f),
    override val scaleOffset: MutableState<Pair<Float, Float>> = mutableStateOf(0f to 0f),
    override val scaleCenter: MutableState<Offset> = mutableStateOf(Offset.Zero),
    override val mousePosition: MutableState<Offset?> = mutableStateOf(null),
    override val xStretchFactor: MutableState<Float> = mutableStateOf(1f),
    override val xStretchCenter: MutableState<Offset> = mutableStateOf(Offset.Zero)
) : IChartEnvironment