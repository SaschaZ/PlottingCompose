package dev.zieger.plottingcompose.styles


import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class SingleGroup<I : Input>(vararg style: PlotStyle<I>) :
    PlotStyle<I>(*style.flatMap { it.slots }.toTypedArray()) {

    private val styles: List<PlotStyle<I>> = style.toList()

    override fun IPlotDrawScope<I>.drawSingle(
        idx: Long,
        value: I,
        data: Map<Key<I>, List<PortValue<*>>>,
        isFocused: Boolean
    ) = styles.forEach { it.run { this.data = data; drawSingle(idx, value, data, isFocused) } }
}