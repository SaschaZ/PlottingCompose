package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.scopes.IPlotDrawScope


class FillBetween<I : Input>(
    private val between: Pair<Slot<I, Output.Scalar>, Slot<I, Output.Scalar>>?,
    private val color: Color = Color.Cyan.copy(alpha = 0.66f)
) : PlotStyle<I>(*listOfNotNull(between?.first, between?.second).toTypedArray()) {

    override fun IPlotDrawScope<I>.drawSeries(data: Map<I, Map<Key<I>, List<PortValue<*>>>>) {
        val offsets =
            data.map { (x, d) ->
                Offset(x.x.toFloat(), between?.first?.value(d)?.scalar?.toFloat() ?: 0f).toScene() to
                        Offset(x.x.toFloat(), between?.second?.value(d)?.scalar?.toFloat() ?: 0f).toScene()
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