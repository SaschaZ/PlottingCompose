package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import dev.zieger.plottingcompose.IParameter
import dev.zieger.plottingcompose.ListeningMutableState


interface IPlotScope {
    val plotSize: MutableState<IntSize>
    val translation: MutableState<Offset>
    val translationListener: SnapshotStateList<(Offset) -> Unit>
    val translationOffset: MutableState<Offset>
    val scale: MutableState<Float>
    val scaleListener: SnapshotStateList<(Float) -> Unit>
    val scaleCenter: MutableState<Offset>
    val scaleCenterListener: SnapshotStateList<(Offset) -> Unit>
    val mousePosition: MutableState<Offset?>
    val widthFactor: MutableState<Float>
    val xStretchListener: SnapshotStateList<(Float) -> Unit>
    val widthFactorCenter: MutableState<Offset>
    val xStretchCenterListener: SnapshotStateList<(Offset) -> Unit>
    val applyTranslationOffset: MutableState<Boolean>

    fun finalTranslation(params: IParameter): Offset = params.run {
        try {
            translation.value.run {
                copy(x = x - (widthFactorCenter.value.x - horizontalPadding().value - horizontalPlotPadding().value).let {
                    it * (widthFactor.value - 1)
                })
            } + translationOffset.value
        } catch (t: Throwable) {
            translation.value
        }
    }
}

@Composable
fun PlotScope(parameter: IParameter): IPlotScope = (object : IPlotScope {
    override val plotSize: MutableState<IntSize> = remember { mutableStateOf(IntSize.Zero) }
    override val translationListener: SnapshotStateList<(Offset) -> Unit> = remember { mutableStateListOf() }
    override val translation: MutableState<Offset> =
        remember { ListeningMutableState(Offset.Zero) { translationListener.forEach { l -> l(it) } } }
    override val translationOffset: MutableState<Offset> = remember { mutableStateOf(Offset.Zero) }
    override val scaleListener: SnapshotStateList<(Float) -> Unit> = remember { mutableStateListOf() }
    override val scale: MutableState<Float> =
        remember { ListeningMutableState(1f) { scaleListener.forEach { l -> l(it) } } }
    override val scaleCenterListener: SnapshotStateList<(Offset) -> Unit> = remember { mutableStateListOf() }
    override val scaleCenter: MutableState<Offset> =
        remember { ListeningMutableState(Offset.Zero) { scaleCenterListener.forEach { l -> l(it) } } }
    override val mousePosition: MutableState<Offset?> = remember { mutableStateOf(null) }
    override val widthFactor: MutableState<Float> =
        remember { ListeningMutableState(1f) { xStretchListener.forEach { l -> l(it) } } }
    override val xStretchListener: SnapshotStateList<(Float) -> Unit> = remember { mutableStateListOf() }
    override val widthFactorCenter: MutableState<Offset> =
        remember { ListeningMutableState(Offset.Zero) { xStretchCenterListener.forEach { l -> l(it) } } }
    override val xStretchCenterListener: SnapshotStateList<(Offset) -> Unit> = remember { mutableStateListOf() }
    override val applyTranslationOffset: MutableState<Boolean> = remember { mutableStateOf(false) }
})

