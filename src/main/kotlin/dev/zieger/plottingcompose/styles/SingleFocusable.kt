package dev.zieger.plottingcompose.styles

import dev.zieger.plottingcompose.SinglePlot
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.definition.Value
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class SingleFocusable(
    private val focusedSlot: Slot<(Long) -> Boolean>,
    private val unfocused: PlotStyle,
    private val focused: PlotStyle
) : SingleGroup(*arrayOf(unfocused, focused)) {
    override fun IPlotDrawScope.drawSingle(x: Long, data: Map<Key, Map<Port<*>, Value?>>, plot: SinglePlot) {
        when (focusedSlot.any<(Long) -> Boolean>(data)?.invoke(x)) {
            true -> focused.run { drawSingle(x, data, plot) }
            else -> unfocused.run { drawSingle(x, data, plot) }
        }
    }
}

fun <SP : PlotStyle> SP.focused(focusedSlot: Slot<(Long) -> Boolean>) =
    SingleFocusable(focusedSlot, PlotStyle.Companion.Empty, this)