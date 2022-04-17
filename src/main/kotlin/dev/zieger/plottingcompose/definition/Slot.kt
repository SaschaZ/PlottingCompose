@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter", "unused", "LeakingThis")

package dev.zieger.plottingcompose.definition

import dev.zieger.plottingcompose.processor.ProcessingUnit
import kotlin.reflect.KClass


data class Slot<I : Input, out O : Output>(
    val key: Key<I, *>,
    val port: Port<@UnsafeVariance O>,
    val scale: ((@UnsafeVariance O) -> Double?)? = null
)

infix fun <I : Input, O : Output> Slot<I, O>.select(scale: ((@UnsafeVariance O) -> Double?)?) = copy(scale = scale)

data class Key<I : Input, P : Any>(val name: String, val param: P, var create: (P) -> ProcessingUnit<I>) {
    operator fun invoke(): ProcessingUnit<I> = create(param)

    override fun equals(other: Any?): Boolean = (other as? Key<*, *>)?.let { o ->
        name == o.name && param == o.param
    } ?: false

    override fun hashCode(): Int = name.hashCode() + param.hashCode()

    override fun toString(): String = "Key($name, $param)"
}

inline fun <reified O : Output> Port(
    id: String,
    includeIntoScaling: Boolean = true
) = Port(O::class, id, includeIntoScaling)

data class Port<O : Output>(
    val type: KClass<O>,
    val id: String,
    val includeIntoScaling: Boolean = true
)

data class PortValue<O : Output>(val port: Port<O>, val value: O)

infix fun <I : Input, O : Output> Key<I, *>.with(port: Port<out O>): Slot<I, O> = Slot(this, port)

interface Input {
    val x: Number
}