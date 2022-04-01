package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.zieger.plottingcompose.InputContainer
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import dev.zieger.utils.misc.whenNotNull

open class LineSeries<T : Input>(
    private val slot: Slot<T, Output.Scalar>,
    private val lineColor: Color = Color.Black,
    private val colorSlot: Slot<T, Output.Scalar>? = null,
    private val width: Float = 1f
) : PlotStyle<T>(slot, colorSlot) {

    override fun IPlotDrawScope<T>.drawSeries(data: Map<InputContainer<T>, Map<Key<T, *>, List<PortValue<*>>>>) {
        if (data.isEmpty()) return

        var path: Path? = null
        var color: Color? = null
        data.map { (x, data) ->
            slot.value(data)?.scalar?.toFloat()?.let {
                (colorSlot?.value(data)?.scalar?.toFloat()?.let { diff ->
                    when {
                        diff < 0 -> Color.Red
                        else -> Color.Green
                    }
                } ?: lineColor) to Offset(x.idx.toFloat(), it).toScene()
            }
        }.forEach { pair ->
            when {
                pair == null -> {
                    whenNotNull(path, color) { p, c -> drawPath(p, c, c.alpha, Stroke(width)) }
                    path = null
                    color = null
                }
                path == null -> {
                    path = Path()
                    color = pair.first
                    path!!.moveTo(pair.second)
                }
                color != pair.first -> {
                    path?.let { p ->
                        p.lineTo(pair.second)
                        drawPath(p, pair.first, pair.first.alpha, Stroke(width))
                    }

                    path = Path()
                    color = pair.first
                    path!!.moveTo(pair.second)
                }
                else -> path!!.lineTo(pair.second)
            }
        }
        whenNotNull(path, color) { p, c -> drawPath(p, c, c.alpha, Stroke(width)) }
    }
}