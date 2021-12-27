package dev.zieger.plottingcompose.styles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.zieger.plottingcompose.SinglePlot
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.definition.Value
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class Line(
    private val slot: Slot<Float>,
    private val color: Color = Color.Black,
    private val width: Float = 1f
) : PlotStyle(slot) {

    override fun IPlotDrawScope.drawSeries(data: Map<Long, Map<Key, Map<Port<*>, Value?>>>, plot: SinglePlot) {
        if (data.isEmpty()) return

        drawPath(Path().apply {
            data.map { (x, data) -> plot.toScene(x.toFloat(), slot.float(data) ?: 0f) }
                .forEach { (x, y) ->
                    when {
                        isEmpty -> moveTo(x, y)
                        else -> lineTo(x, y)
                    }
                }
        }, color, color.alpha, Stroke(width))
    }
}