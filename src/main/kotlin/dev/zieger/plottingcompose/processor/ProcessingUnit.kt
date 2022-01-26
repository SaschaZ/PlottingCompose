@file:Suppress("MemberVisibilityCanBePrivate")

package dev.zieger.plottingcompose.processor

import dev.zieger.plottingcompose.definition.*

abstract class ProcessingUnit<T : InputContainer>(
    val key: Key<T>,
    val produces: List<Port<*>> = emptyList(),
    vararg dependsOn: Key<T>
) {
    val dependsOn: List<Key<T>> = dependsOn.toList()
    val slots: List<Slot<*, T>> = produces.map { key with it }

    protected fun ProcessingScope<T>.getAll(): Map<Port<*>, Value?>? = data[key]
    protected fun ProcessingScope<T>.get(port: Port<*>): Value? = getAll()?.get(port)

    protected fun ProcessingScope<T>.set(port: Port<*>, value: Value?) {
        data.getOrPut(key) { HashMap() }[port] = value
    }

    protected fun ProcessingScope<T>.set(port: Port<*>, values: List<Value>) {
        data.getOrPut(key) { HashMap() }[port] = values
    }

    abstract suspend fun ProcessingScope<T>.process()
}