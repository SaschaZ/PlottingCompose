package dev.zieger.plottingcompose.styles

import dev.zieger.plottingcompose.definition.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import dev.zieger.plottingcompose.scopes.ValueHolder

open class SingleFocusable<I : InputContainer>(
    private val focusedSlot: Slot<(Double) -> Boolean, I>,
    private val unfocused: PlotStyle<I>,
    private val focused: PlotStyle<I>
) : SingleGroup<I>(*arrayOf(unfocused, focused)) {
    override fun IPlotDrawScope<I>.drawSingle(value: I, data: Map<Key<I>, Map<Port<*>, ValueHolder?>>) {
        when (focusedSlot.value(data)?.invoke(value.x)) {
            true -> focused.run { drawSingle(value, data) }
            else -> unfocused.run { drawSingle(value, data) }
        }
    }
}

fun <I : InputContainer, SP : PlotStyle<I>> SP.focused(focusedSlot: Slot<(Double) -> Boolean, I>) =
    SingleFocusable(focusedSlot, PlotStyle(), this)