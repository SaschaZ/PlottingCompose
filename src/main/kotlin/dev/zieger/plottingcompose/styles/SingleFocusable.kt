package dev.zieger.plottingcompose.styles

import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.di.ChartScope

open class SingleFocusable<I : Input>(
    private val unfocused: PlotStyle<I>,
    private val focused: PlotStyle<I>
) : SingleGroup<I>(*arrayOf(unfocused, focused)) {
    override fun ChartScope.drawSingle(
        idx: Long,
        value: I,
        data: Map<Key<I, *>, List<PortValue<*>>>,
        isFocused: Boolean
    ) {
        when (isFocused) {
            true -> focused.run { this.data = data; drawSingle(idx, value, data, true) }
            else -> unfocused.run { this.data = data; drawSingle(idx, value, data, false) }
        }
    }
}

fun <I : Input, SP : PlotStyle<I>> SP.whenFocused() = SingleFocusable(PlotStyle(), this)