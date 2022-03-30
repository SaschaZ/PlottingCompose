package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import dev.zieger.plottingcompose.div
import dev.zieger.plottingcompose.x
import dev.zieger.plottingcompose.y
import kotlin.math.pow

interface IGlobalChartEnvironment {
    val chartSize: MutableState<IntSize>
    val translation: MutableState<Offset>
    val translationOffsetX: MutableState<Float>
    val scale: MutableState<Pair<Float, Float>>
    val scaleOffset: MutableState<Pair<Float, Float>>
    val finalScale: Pair<Float, Float>
        get() = (scale.value.x + scaleOffset.value.x).coerceAtLeast(0.01f) to
                (scale.value.y + scaleOffset.value.y).coerceAtLeast(0.01f)
    val scaleCenterRelative: MutableState<Offset>
    val scaleCenter: Offset
        get() = scaleCenterRelative.value * chartSize.value
    val mousePosition: MutableState<Offset?>

    fun reset() {
        chartSize.value = IntSize.Zero
        translation.value = Offset.Zero
        translationOffsetX.value = 0f
        scale.value = 1f to 1f
        scaleOffset.value = 0f to 0f
        scaleCenterRelative.value = Offset(0.5f, 0.5f)
        mousePosition.value = null
    }

    fun processVerticalScrolling(delta: Float) {
        scale.value =
            (scale.value.x.let { it - it.pow(2) * 0.005f * delta }).coerceAtLeast(0.5f) to 1f

        mousePosition.value?.also { mp -> scaleCenterRelative.value = mp / chartSize.value }
    }

    fun processHorizontalScroll(delta: Float) {
        translation.value = translation.value.run {
            copy(x + delta * finalScale.x.pow(0.01f) * 100)
        }
    }

    fun processDrag(dragAmount: Offset) {
        translation.value = translation.value.run { copy(x + dragAmount.x) }
    }
}

private operator fun Offset.times(other: IntSize): Offset =
    Offset(x * other.width, y * other.height)

interface IChartEnvironment : IGlobalChartEnvironment {
    val translationOffsetY: MutableState<Float>
    val finalTranslation: Offset
        get() = translation.value +
                Offset(translationOffsetX.value, translationOffsetY.value)

    override fun reset() {
        super.reset()

        translationOffsetY.value = 0f
    }
}

data class GlobalChartEnvironment(
    override val chartSize: MutableState<IntSize> = mutableStateOf(IntSize.Zero),
    override val translation: MutableState<Offset> = mutableStateOf(Offset.Zero),//(-20000f, 0f)),
    override val translationOffsetX: MutableState<Float> = mutableStateOf(0f),
    override val scale: MutableState<Pair<Float, Float>> = mutableStateOf(1f to 1f),
    override val scaleOffset: MutableState<Pair<Float, Float>> = mutableStateOf(0f to 0f),
    override val scaleCenterRelative: MutableState<Offset> = mutableStateOf(Offset(0.5f, 0.5f)),
    override val mousePosition: MutableState<Offset?> = mutableStateOf(null)
) : IGlobalChartEnvironment

data class ChartEnvironment(
    val globalChartEnvironment: IGlobalChartEnvironment,
    override val translationOffsetY: MutableState<Float> = mutableStateOf(0f)
) : IChartEnvironment, IGlobalChartEnvironment by globalChartEnvironment {

    override fun reset() = super<IChartEnvironment>.reset()
}