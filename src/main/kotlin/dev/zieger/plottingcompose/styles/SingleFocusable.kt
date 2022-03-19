package dev.zieger.plottingcompose.styles

import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class SingleFocusable<I : Input>(
    private val unfocused: PlotStyle<I>,
    private val focused: PlotStyle<I>
) : SingleGroup<I>(*arrayOf(unfocused, focused)) {
    override fun IPlotDrawScope<I>.drawSingle(
        idx: Long,
        value: I,
        data: Map<Key<I>, List<PortValue<*>>>,
        isFocused: Boolean
    ) {
        when (isFocused) {
            true -> focused.run { drawSingle(idx, value, data, true) }
            else -> unfocused.run { drawSingle(idx, value, data, false) }
        }
    }
}

fun <I : Input, SP : PlotStyle<I>> SP.whenFocused() = SingleFocusable(PlotStyle(), this)