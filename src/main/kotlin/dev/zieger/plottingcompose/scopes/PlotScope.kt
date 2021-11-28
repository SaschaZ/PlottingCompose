package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import dev.zieger.plottingcompose.IParameter


interface IPlotScope {
    val plotSize: MutableState<IntSize>
    val translation: MutableState<Offset>
    val translationOffset: MutableState<Offset>
    val scale: MutableState<Float>
    val scaleCenter: MutableState<Offset>
    val mousePosition: MutableState<Offset?>
    val widthFactor: MutableState<Float>
    val widthFactorCenter: MutableState<Offset>

    fun finalTranslation(params: IParameter): Offset = params.run {
        translation.value.run {
            copy(x = x - (widthFactorCenter.value.x - horizontalPadding.value - horizontalPlotPadding.value).let {
                it * (widthFactor.value - 1)
            })
        } + translationOffset.value
    }
}

@Composable
fun PlotScope(parameter: IParameter): IPlotScope = (object : IPlotScope {
    override val plotSize: MutableState<IntSize> = remember { mutableStateOf(IntSize.Zero) }
    override val translation: MutableState<Offset> = remember { mutableStateOf(Offset.Zero) }
    override val translationOffset: MutableState<Offset> = remember { mutableStateOf(Offset.Zero) }
    override val scale: MutableState<Float> = remember { mutableStateOf(1f) }
    override val scaleCenter: MutableState<Offset> = remember { mutableStateOf(Offset.Zero) }
    override val mousePosition: MutableState<Offset?> = remember { mutableStateOf(null) }
    override val widthFactor: MutableState<Float> = remember { mutableStateOf(1f) }
    override val widthFactorCenter: MutableState<Offset> = remember { mutableStateOf(Offset.Zero) }
})