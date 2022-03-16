@file:Suppress("unused")

package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import kotlin.reflect.cast

open class PlotStyle<I : Input>(vararg slot: Slot<I, *>) {

    val slots: List<Slot<I, *>> = slot.toList()

    open fun IPlotDrawScope<I>.drawSeries(data: Map<I, Map<Key<I>, List<PortValue<*>>>>) {
        data.entries.forEachIndexed { idx, (key, data) ->
            drawSingle(key, data, focusedItemIdx.value?.itemIdx == idx)
        }
    }

    open fun IPlotDrawScope<I>.drawSingle(value: I, data: Map<Key<I>, List<PortValue<*>>>, isFocused: Boolean) = Unit

    protected fun <I : Input, O : Output> Slot<I, O>.value(data: Map<Key<I>, List<PortValue<*>>>): O? =
        data[key]?.firstOrNull { it.port == port }?.value?.let { v ->
            if (port.type.isInstance(v))
                port.type.cast(v)
            else null
        }
}

fun Path.moveTo(offset: Offset) = moveTo(offset.x, offset.y)
fun Path.lineTo(offset: Offset) = lineTo(offset.x, offset.y)

