package dev.zieger.plottingcompose.styles

import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class SingleFocusable<I : Input>(
    private val focusedSlot: Slot<I, Output.Lambda>,
    private val unfocused: PlotStyle<I>,
    private val focused: PlotStyle<I>
) : SingleGroup<I>(*arrayOf(unfocused, focused)) {
    override fun IPlotDrawScope<I>.drawSingle(value: I, data: Map<Key<I>, List<PortValue<*>>>) {
        when (focusedSlot.value(data)?.lambda?.invoke(value.x)) {
            true -> focused.run { drawSingle(value, data) }
            else -> unfocused.run { drawSingle(value, data) }
        }
    }
}

fun <I : Input, SP : PlotStyle<I>> SP.focused(focusedSlot: Slot<I, Output.Lambda>) =
    SingleFocusable(focusedSlot, PlotStyle(), this)