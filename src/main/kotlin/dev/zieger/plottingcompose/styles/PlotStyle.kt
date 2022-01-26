@file:Suppress("unused")

package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import dev.zieger.plottingcompose.definition.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import dev.zieger.plottingcompose.scopes.ValueHolder
import kotlin.reflect.cast

open class PlotStyle<I : InputContainer>(vararg slot: Slot<*, I>) {

    val slots: List<Slot<*, I>> = slot.toList()

    open fun IPlotDrawScope<I>.drawSeries(data: Map<I, Map<Key<I>, Map<Port<*>, ValueHolder?>>>) {
        data.entries.forEachIndexed { idx, (key, data) ->
            activeIdx = idx
            drawSingle(key, data)
        }
        activeIdx = -1
    }

    open fun IPlotDrawScope<I>.drawSingle(value: I, data: Map<Key<I>, Map<Port<*>, ValueHolder?>>) = Unit

    protected fun <T : Any, I : InputContainer> Slot<T, I>.value(data: Map<Key<I>, Map<Port<*>, ValueHolder?>>): T? =
        data[key]?.get(port)?.value?.let { v ->
            if (port.type.isInstance(v))
                port.type.cast(v)
            else null
        }
}

fun Path.moveTo(offset: Offset) = moveTo(offset.x, offset.y)
fun Path.lineTo(offset: Offset) = lineTo(offset.x, offset.y)

