package dev.zieger.plottingcompose.styles

import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class SingleFocusable<I : Input>(
    private val unfocused: PlotStyle<I>,
    private val focused: PlotStyle<I>
) : SingleGroup<I>(*arrayOf(unfocused, focused)) {
    override fun IPlotDrawScope<I>.drawSingle(value: I, data: Map<Key<I>, List<PortValue<*>>>, isFocused: Boolean) {
        when (isFocused) {
            true -> focused.run { drawSingle(value, data, true) }
            else -> unfocused.run { drawSingle(value, data, false) }
        }
    }
}

fun <I : Input, SP : PlotStyle<I>> SP.whenFocused() = SingleFocusable(PlotStyle(), this)