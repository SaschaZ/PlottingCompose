@file:Suppress("MemberVisibilityCanBePrivate")

package dev.zieger.plottingcompose.processor

import dev.zieger.plottingcompose.definition.*

abstract class ProcessingUnit<T : Any>(
    val key: Key,
    val produces: List<Port> = emptyList(),
    open val dependsOn: List<ProcessingUnit<T>> = emptyList()
) {

    val slots: List<Slot> = produces.map { key with it }

    protected fun ProcessingScope<T>.getAll(): Map<Port, Value?>? = data[key]
    protected fun ProcessingScope<T>.get(port: Port): Value? = getAll()?.get(port)

    protected fun ProcessingScope<T>.set(port: Port, value: Value?) {
        data.getOrPut(key) { HashMap() }[port] = value
    }

    protected fun ProcessingScope<T>.set(port: Port, values: List<Value>) {
        data.getOrPut(key) { HashMap() }[port] = values
    }

    abstract suspend fun ProcessingScope<T>.process()
}