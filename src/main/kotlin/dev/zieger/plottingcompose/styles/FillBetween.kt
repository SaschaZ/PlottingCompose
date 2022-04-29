package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.di.ChartScope


class FillBetween<I : Input>(
    private val between: Pair<Slot<I, Output.Scalar>, Slot<I, Output.Scalar>>?,
    private val color: Color = Color.Cyan.copy(alpha = 0.66f)
) : PlotStyle<I>(*listOfNotNull(between?.first, between?.second).toTypedArray()) {

    override fun ChartScope.drawSeries(data: List<Pair<InputContainer<I>, Map<Key<I, *>, List<PortValue<*>>>>>) {
        val offsets =
            data.map { (x, d) ->
                Offset(x.idx.toFloat(), between?.first?.value(d)?.scalar?.toFloat() ?: 0f).toScene() to
                        Offset(x.idx.toFloat(), between?.second?.value(d)?.scalar?.toFloat() ?: 0f).toScene()
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