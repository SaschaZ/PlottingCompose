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
        value: T,
        data: Map<Key<T>, List<PortValue<*>>>
    ) {
        slot.value(data)?.let {
            drawCircle(
                color,
                width / 2,
                Offset(value.x.toFloat(), it.scalar.toFloat()).toScene(),
                color.alpha,
                strokeWidth?.let { s -> Stroke(s) } ?: Fill)
        }
    }
}