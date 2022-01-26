package dev.zieger.plottingcompose.processor

import dev.zieger.plottingcompose.definition.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Value

data class ProcessingScope<T : InputContainer>(
    val value: T,
    val data: HashMap<Key<T>, HashMap<Port<*>, Value?>> = HashMap()
) {
    suspend fun HashMap<Key<T>, ProcessingUnit<T>>.doProcess() {
        forEach { unit ->
            unit.let { (key, unit) ->
                unit.dependsOn.associateWith { getOrPut(it) { it() } }.let { HashMap(it) }.doProcess()
                if (unit.produces.any { data[key]?.containsKey(it) != true })
                    unit.run { process() }
            }
        }
    }
}