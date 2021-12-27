package dev.zieger.plottingcompose.styles


import dev.zieger.plottingcompose.SinglePlot
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Value
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class SingleGroup(vararg style: PlotStyle) : PlotStyle(*style.flatMap { it.slots }.toTypedArray()) {

    private val styles: List<PlotStyle> = style.toList()

    override fun IPlotDrawScope.drawSingle(x: Long, data: Map<Key, Map<Port<*>, Value?>>, plot: SinglePlot) =
        styles.forEach { it.run { drawSingle(x, data, plot) } }
}