package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

import dev.zieger.plottingcompose.SinglePlot
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.definition.Value
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class Dot(
    private val slot: Slot<Float>,
    val color: Color = Color.Black,
    val width: Float = 1f,
    private val strokeWidth: Float? = null
) : PlotStyle(slot) {

    override fun IPlotDrawScope.drawSingle(
        x: Long,
        data: Map<Key, Map<Port<*>, Value?>>,
        plot: SinglePlot
    ) {
        slot.float(data)?.let {
            drawCircle(
                color,
                width / 2,
                plot.toScene(Offset(x.toFloat(), it)),
                color.alpha,
                strokeWidth?.let { s -> Stroke(s) } ?: Fill)
        }
    }
}