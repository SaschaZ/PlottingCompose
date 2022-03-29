package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.processor.ProcessingUnit

abstract class Indicator<I : Input>(
    key: Key<I, *>,
    produces: List<Port<*>> = emptyList(),
    vararg dependsOn: Key<I, *>
) : ProcessingUnit<I>(key, produces, *dependsOn)

abstract class IndicatorDefinition<P : Any> {

    abstract fun key(param: P): Key<*, *>
}