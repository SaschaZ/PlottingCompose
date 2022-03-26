package dev.zieger.plottingcompose.processor

import dev.zieger.plottingcompose.definition.*
import kotlin.reflect.cast

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

    fun <O : Output> Slot<I, O>.value(): O? = data[key]?.firstOrNull { it.port == port }?.let { v ->
        if (port.type == v.port.type)
            port.type.cast(v.value)
        else null
    }

    infix fun <O : Output> Key<I>.dataOf(port: Port<O>): O? = Slot(this, port).value()
}