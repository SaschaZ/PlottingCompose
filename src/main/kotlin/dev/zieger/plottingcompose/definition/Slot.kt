package dev.zieger.plottingcompose.definition

import dev.zieger.plottingcompose.processor.ProcessingUnit
import kotlin.reflect.KClass

data class Slot<T : Any, I : InputContainer>(val key: Key<I>, val port: Port<T>)

data class Key<U : InputContainer>(val name: String, val param: Any, val create: () -> ProcessingUnit<U>) {
    operator fun invoke(): ProcessingUnit<U> = create()
}

inline fun <reified T : Any> Port(
    id: String,
    includeIntoScaling: Boolean = true
) = Port(T::class, id, includeIntoScaling)

data class Port<T : Any>(
    val type: KClass<T>,
    val id: String,
    val includeIntoScaling: Boolean = true
)

infix fun <T : Any, I : InputContainer> Key<I>.with(port: Port<T>): Slot<T, I> = Slot(this, port)

typealias Value = Any

interface ValueContainer {
    val yRange: ClosedRange<Double>
}

interface InputContainer {
    val x: Double
}

