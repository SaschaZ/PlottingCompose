package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.zieger.plottingcompose.definition.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import dev.zieger.plottingcompose.scopes.ValueHolder

open class Dot<T : InputContainer>(
    private val slot: Slot<Float, T>,
    val color: Color = Color.Black,
    val width: Float = 1f,
    private val strokeWidth: Float? = null
) : PlotStyle<T>(slot) {

    override fun IPlotDrawScope<T>.drawSingle(
        value: T,
        data: Map<Key<T>, Map<Port<*>, ValueHolder?>>
    ) {
        slot.value(data)?.let {
            drawCircle(
                color,
                width / 2,
                Offset(value.x.toFloat(), it).toScene(),
                color.alpha,
                strokeWidth?.let { s -> Stroke(s) } ?: Fill)
        }
    }
}