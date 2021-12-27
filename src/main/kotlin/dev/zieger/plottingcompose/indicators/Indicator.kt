package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.processor.ProcessingUnit

abstract class Indicator(
    key: Key,
    produces: List<Port<*>> = emptyList(),
    override val dependsOn: List<Indicator> = emptyList()
) : ProcessingUnit<ICandle>(key, produces, dependsOn)