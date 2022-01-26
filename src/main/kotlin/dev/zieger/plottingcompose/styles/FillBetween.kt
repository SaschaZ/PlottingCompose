package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import dev.zieger.plottingcompose.definition.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import dev.zieger.plottingcompose.scopes.ValueHolder

class FillBetween<T : InputContainer>(
    private val between: Pair<Slot<Float, T>, Slot<Float, T>>?,
    private val color: Color = Color.Cyan.copy(alpha = 0.66f)
) : PlotStyle<T>(*listOfNotNull(between?.first, between?.second).toTypedArray()) {
    override fun IPlotDrawScope<T>.drawSeries(data: Map<T, Map<Key<T>, Map<Port<*>, ValueHolder?>>>) {
        val offsets =
            data.map { (x, d) ->
                Offset(x.x.toFloat(), between?.first?.value(d) ?: 0f).toScene() to
                        Offset(x.x.toFloat(), between?.second?.value(d) ?: 0f).toScene()
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