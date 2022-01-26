package dev.zieger.plottingcompose.styles


import dev.zieger.plottingcompose.definition.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import dev.zieger.plottingcompose.scopes.ValueHolder

open class SingleGroup<I : InputContainer>(vararg style: PlotStyle<I>) :
    PlotStyle<I>(*style.flatMap { it.slots }.toTypedArray()) {

    private val styles: List<PlotStyle<I>> = style.toList()

    override fun IPlotDrawScope<I>.drawSingle(value: I, data: Map<Key<I>, Map<Port<*>, ValueHolder?>>) =
        styles.forEach { it.run { drawSingle(value, data) } }
}