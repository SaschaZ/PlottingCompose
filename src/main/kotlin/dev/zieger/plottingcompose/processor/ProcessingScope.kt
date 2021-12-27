package dev.zieger.plottingcompose.processor

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Value

data class ProcessingScope<T : Any>(
    val value: T,
    val units: List<ProcessingUnit<T>>,
    val data: HashMap<Key, HashMap<Port, Value?>> = HashMap()
) {
    suspend fun List<ProcessingUnit<T>>.doProcess() {
        forEach { unit ->
            unit.run {
                dependsOn.doProcess()
                if (produces.all { data[key]?.containsKey(it) != true })
                    process()
            }
        }
    }
}