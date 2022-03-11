package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.processor.ProcessingUnit
import kotlin.reflect.cast

abstract class Indicator<I : Input>(
    key: Key<I>,
    produces: List<Port<*>> = emptyList(),
    vararg dependsOn: Key<I>
) : ProcessingUnit<I>(key, produces, *dependsOn) {

    protected fun <I : Input, O : Output> Slot<I, O>.value(data: Map<Key<I>, List<PortValue<*>>>): O? =
        data[key]?.firstOrNull { it.port == port }?.let { v ->
            if (port.type == v.port.type)
                port.type.cast(v.value)
            else null
        }
}

abstract class IndicatorDefinition<P : Any> {

    abstract fun key(param: P): Key<*>
}