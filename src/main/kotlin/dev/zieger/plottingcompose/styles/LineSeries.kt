@file:Suppress("unused")

package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.zieger.plottingcompose.InputContainer
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class LineSeriesValueSelect<T : Input, O : Output>(
    slot: Slot<T, O>,
    defaultColor: Color = Color.Black,
    lineWidth: Float = 1f,
    colorSelector: (O) -> Color? = { null },
    valueSelector: (O) -> Float?
) : LineSeriesSelect<T, O, O>(slot, defaultColor, lineWidth, slot, colorSelector, valueSelector)

open class LineSeriesColorSelect<T : Input, C : Output>(
    slot: Slot<T, Output.Scalar>,
    defaultColor: Color = Color.Black,
    lineWidth: Float = 1f,
    colorSlot: Slot<T, C>? = null,
    colorSelector: (C) -> Color? = { null },
) : LineSeriesSelect<T, Output.Scalar, C>(slot, defaultColor, lineWidth, colorSlot, colorSelector = colorSelector,
    valueSelector = { it.scalar.toFloat() })

open class LineSeries<T : Input>(
    slot: Slot<T, Output.Scalar>,
    defaultColor: Color = Color.Black,
    lineWidth: Float = 1f,
) : LineSeriesSelect<T, Output.Scalar, Output>(
    slot, defaultColor, lineWidth,
    valueSelector = { it.scalar.toFloat() })

open class LineSeriesSelect<T : Input, O : Output, C : Output>(
    private val slot: Slot<T, O>,
    private val defaultColor: Color = Color.Black,
    private val lineWidth: Float = 1f,
    private val colorSlot: Slot<T, C>? = null,
    private val colorSelector: (C) -> Color? = { null },
    private val valueSelector: (O) -> Float?
) : PlotStyle<T>(slot, colorSlot) {

    override fun IPlotDrawScope<T>.drawSeries(data: List<Pair<InputContainer<T>, Map<Key<T, *>, List<PortValue<*>>>>>) {
        if (data.isEmpty()) return

        fun Path.draw(color: Color) = drawPath(this, color, color.alpha, Stroke(lineWidth))

        var path: Path? = null

        data.map { (x, data) ->
            slot.value(data)
                ?.let { valueSelector(it) }
                .let { value ->
                    colorSlot?.value(data)?.let { colorSelector(it) } to
                            value?.let { Offset(x.idx.toFloat(), it).toScene() }
                }
        }.forEach { (color, offset) ->
            when {
                offset == null -> {
                    path?.draw(color ?: defaultColor)
                    path = null
                }
                color != null -> {
                    path?.lineTo(offset)
                    path?.draw(color)
                    path = null
                }
                path == null -> {
                    path = Path()
                    path!!.moveTo(offset)
                }
                else -> path?.lineTo(offset)
            }
        }
        path?.draw(defaultColor)
    }
}