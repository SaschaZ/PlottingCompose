package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class LineSeries<T : Input>(
    private val slot: Slot<T, Output.Scalar>,
    private val color: Color = Color.Black,
    private val width: Float = 1f
) : PlotStyle<T>(slot) {

    override fun IPlotDrawScope<T>.drawSeries(data: Map<T, Map<Key<T>, List<PortValue<*>>>>) {
        if (data.isEmpty()) return

        drawPath(Path().apply {
            data.map { (x, data) ->
                Offset(x.x.toFloat(), slot.value(data)?.scalar?.toFloat() ?: 0f).toScene()
            }.forEach { (x, y) ->
                when {
                    isEmpty -> moveTo(x, y)
                    else -> lineTo(x, y)
                }
            }
        }, color, color.alpha, Stroke(width))
    }
}