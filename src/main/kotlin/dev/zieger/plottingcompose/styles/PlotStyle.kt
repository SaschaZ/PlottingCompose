@file:Suppress("unused")

package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.di.ChartScope
import kotlin.reflect.cast

open class PlotStyle<I : Input>(vararg slot: Slot<I, *>?) {

    val slots: List<Slot<I, *>> = slot.toList().filterNotNull()
    internal lateinit var data: Map<Key<I, *>, List<PortValue<*>>>

    open fun ChartScope.drawSeries(data: List<Pair<InputContainer<I>, Map<Key<I, *>, List<PortValue<*>>>>>) {
        data.forEach { (input, data) ->
            this@PlotStyle.data = data
            drawSingle(input.idx, input.input, data, focusedItem.value?.idx == input.idx)
        }
    }

    open fun ChartScope.drawSingle(
        idx: Long,
        value: I,
        data: Map<Key<I, *>, List<PortValue<*>>>,
        isFocused: Boolean
    ) = Unit

    protected fun <O : Output> Slot<I, O>.value(data: Map<Key<I, *>, List<PortValue<*>>> = this@PlotStyle.data): O? =
        data[key]?.firstOrNull { it.port == port }?.value?.let { v ->
            if (port.type.isInstance(v))
                port.type.cast(v)
            else null
        }
}

fun Path.moveTo(offset: Offset) = moveTo(offset.x, offset.y)
fun Path.lineTo(offset: Offset) = lineTo(offset.x, offset.y)

