package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class Line<T : Input>(
    private val slot: Slot<T, Output.OffsetVector>,
    private val color: Color = Color.Cyan,
    private val width: Float = 1f
) : PlotStyle<T>(slot) {
    override fun IPlotDrawScope<T>.drawSingle(value: T, data: Map<Key<T>, List<PortValue<*>>>, isFocused: Boolean) {
        slot.value(data)?.offsets?.toList()?.let { (from, to) ->
            drawLine(color, Offset(from.x, from.y), Offset(to.x, to.y), width, alpha = color.alpha)
        }
    }
}