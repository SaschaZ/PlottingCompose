package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class Dot<T : Input>(
    private val slot: Slot<T, Output.Scalar>,
    val color: Color = Color.Black,
    val width: Float = 1f,
    private val strokeWidth: Float? = null
) : PlotStyle<T>(slot) {

    override fun IPlotDrawScope<T>.drawSingle(
        idx: Long,
        value: T,
        data: Map<Key<T, *>, List<PortValue<*>>>,
        isFocused: Boolean
    ) {
        slot.value()?.let {
            drawCircle(
                color,
                width / 2,
                Offset(idx.toFloat(), it.scalar.toFloat()).toScene(),
                color.alpha,
                strokeWidth?.let { s -> Stroke(s) } ?: Fill)
        }
    }
}

open class DotIndependent<T : Input>(
    private val slot: Slot<T, Output.Offset>,
    val color: Color = Color.Black,
    val width: Float = 1f,
    private val strokeWidth: Float? = null
) : PlotStyle<T>(slot) {

    override fun IPlotDrawScope<T>.drawSingle(
        idx: Long,
        value: T,
        data: Map<Key<T, *>, List<PortValue<*>>>,
        isFocused: Boolean
    ) {
        slot.value()?.let {
            drawCircle(
                color,
                width / 2,
                it.offset.copy(x = idx.toFloat()).toScene(),
                color.alpha,
                strokeWidth?.let { s -> Stroke(s) } ?: Fill)
        }
    }
}