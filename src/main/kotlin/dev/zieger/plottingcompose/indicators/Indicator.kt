package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.processor.ProcessingUnit
import kotlin.reflect.cast

abstract class Indicator(
    key: Key<ICandle>,
    produces: List<Port<*>> = emptyList(),
    vararg dependsOn: Key<ICandle>
) : ProcessingUnit<ICandle>(key, produces, *dependsOn) {

    protected fun <T : Any, I : InputContainer> Slot<T, I>.value(data: Map<Key<I>, Map<Port<*>, Value?>>): T? =
        data[key]?.get(port)?.let { v ->
            if (port.type.isInstance(v))
                port.type.cast(v)
            else null
        }
}

abstract class IndicatorDefinition<P : Any> {

    abstract fun key(param: P): Key<ICandle>
}