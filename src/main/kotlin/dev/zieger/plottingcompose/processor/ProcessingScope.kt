package dev.zieger.plottingcompose.processor

import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.PortValue

data class ProcessingScope<I : Input>(
    val input: I,
    val data: HashMap<Key<I>, MutableList<PortValue<*>>> = HashMap()
) {
    suspend fun Map<Key<I>, ProcessingUnit<I>>.doProcess() {
        forEach { (key, unit) ->
            doProcess(key, unit)
        }
    }

    private suspend fun Map<Key<I>, ProcessingUnit<I>>.doProcess(key: Key<I>, unit: ProcessingUnit<I>) {
        unit.dependsOn.forEach { doProcess(it, get(it)!!) }
        if (unit.produces.any { it !in (data[key]?.map { m -> m.port } ?: emptyList()) })
            unit.run { process() }
    }
}