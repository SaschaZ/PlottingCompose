@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter", "unused", "LeakingThis")

package dev.zieger.plottingcompose.definition

import dev.zieger.plottingcompose.processor.ProcessingUnit
import kotlin.reflect.KClass

data class Slot<I : Input, O : Output>(val key: Key<I>, val port: Port<O>)

data class Key<I : Input>(val name: String, val param: Any, val create: () -> ProcessingUnit<I>) {
    operator fun invoke(): ProcessingUnit<I> = create()
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

infix fun <I : Input, O : Output> Key<I>.with(port: Port<O>): Slot<I, O> = Slot(this, port)

interface Input {
    val x: Number
}

sealed class Output {
    abstract val xRange: ClosedRange<Double>
    abstract val yRange: ClosedRange<Double>

    open class Scalar(open val x: Number, open val scalar: Number) : Output() {
        override val xRange: ClosedRange<Double> get() = x.toDouble()..x.toDouble()
        override val yRange: ClosedRange<Double> get() = scalar.toDouble()..scalar.toDouble()
    }

    open class Vector(open val x: Number, open val vector: Collection<Number>) : Output() {
        override val xRange: ClosedRange<Double> get() = x.toDouble()..x.toDouble()
        override val yRange: ClosedRange<Double> get() = vector.minOf { it.toDouble() }..vector.maxOf { it.toDouble() }
    }

    open class Label(x: Number, y: Number, val label: String) : Scalar(x, y)
    open class Lambda(x: Number, y: Number, val lambda: (Number) -> Boolean) : Scalar(x, y)
    open class Container<V : Output>(open val items: List<V>) : Output() {
        override val xRange: ClosedRange<Double> get() = items.minOf { it.xRange.start }..items.maxOf { it.xRange.endInclusive }
        override val yRange: ClosedRange<Double> get() = items.minOf { it.yRange.start }..items.maxOf { it.yRange.endInclusive }
    }
}
