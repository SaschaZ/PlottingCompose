@file:Suppress("unused")

package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import dev.zieger.plottingcompose.SinglePlot
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.definition.Value
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class PlotStyle(vararg slot: Slot<*>) {

    companion object {
        object Empty : PlotStyle()
    }

    val slots: List<Slot<*>> = slot.toList()

    open fun IPlotDrawScope.drawSeries(data: Map<Long, Map<Key, Map<Port<*>, Value?>>>, plot: SinglePlot) =
        data.entries.forEach { (key, data) -> drawSingle(key, data, plot) }

    open fun IPlotDrawScope.drawSingle(x: Long, data: Map<Key, Map<Port<*>, Value?>>, plot: SinglePlot) = Unit
}

fun Path.moveTo(offset: Offset) = moveTo(offset.x, offset.y)
fun Path.lineTo(offset: Offset) = lineTo(offset.x, offset.y)

