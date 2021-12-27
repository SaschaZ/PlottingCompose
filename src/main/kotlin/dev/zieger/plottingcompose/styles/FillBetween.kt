package dev.zieger.plottingcompose.styles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import dev.zieger.plottingcompose.SinglePlot
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.definition.Value
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

class FillBetween(
    private val between: Pair<Slot<Float>, Slot<Float>>?,
    private val color: Color = Color.Cyan.copy(alpha = 0.66f)
) : PlotStyle(*listOfNotNull(between?.first, between?.second).toTypedArray()) {
    override fun IPlotDrawScope.drawSeries(data: Map<Long, Map<Key, Map<Port<*>, Value?>>>, plot: SinglePlot) {
        val offsets =
            data.map { (x, d) ->
                plot.toScene(x.toFloat(), between?.first?.float(d) ?: 0f) to
                        plot.toScene(x.toFloat(), between?.second?.float(d) ?: 0f)
            }
        val path = Path().apply {
            offsets.forEach { (top, _) ->
                when {
                    isEmpty -> moveTo(top)
                    else -> lineTo(top)
                }
            }
            offsets.reversed().forEach { (_, bottom) ->
                when {
                    isEmpty -> moveTo(bottom)
                    else -> lineTo(bottom)
                }
            }
        }
        drawPath(path, color, color.alpha, Fill)
    }
}