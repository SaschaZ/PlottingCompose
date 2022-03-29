@file:Suppress("MemberVisibilityCanBePrivate")

package dev.zieger.plottingcompose.processor

import dev.zieger.plottingcompose.definition.*
import java.util.*

abstract class ProcessingUnit<I : Input>(
    val key: Key<I, *>,
    val produces: List<Port<*>> = emptyList(),
    vararg dependsOn: Key<I, *>
) {

    init {
        key.create = { this }
    }

    val dependsOn: List<Key<I, *>> = dependsOn.toList()
    val slots: List<Slot<*, *>> = produces.map { key with it }

    protected fun ProcessingScope<I>.getAll(): List<PortValue<*>>? = data[key]
    protected fun ProcessingScope<I>.get(port: Port<*>): Output? = getAll()?.firstOrNull { it.port == port }?.value

    protected fun <V : Output> ProcessingScope<I>.set(port: Port<V>, value: V) {
        data.getOrPut(key) { LinkedList() }.add(PortValue(port, value))
    }

    abstract suspend fun ProcessingScope<I>.process()
}