package dev.zieger.plottingcompose.definition

data class Slot<T>(val key: Key, val port: Port<T>) {

    inline fun <reified T> any(data: Map<Key, Map<Port<*>, Value?>>): T? = data[key]?.get(port) as? T
    inline fun <reified T> anyList(data: Map<Key, Map<Port<*>, Value?>>): List<T>? =
        (data[key]?.get(port) as? List<*>)?.filterIsInstance<T>()

    fun string(data: Map<Key, Map<Port<*>, Value?>>): String? = any(data)
    fun strings(data: Map<Key, Map<Port<*>, Value?>>): List<String>? = anyList(data)

    fun int(data: Map<Key, Map<Port<*>, Value?>>): Int? = any(data)
    fun ints(data: Map<Key, Map<Port<*>, Value?>>): List<Int>? = anyList(data)

    fun long(data: Map<Key, Map<Port<*>, Value?>>): Long? = any(data)
    fun longs(data: Map<Key, Map<Port<*>, Value?>>): List<Long>? = anyList(data)

    fun float(data: Map<Key, Map<Port<*>, Value?>>): Float? = any(data)
    fun floats(data: Map<Key, Map<Port<*>, Value?>>): List<Float>? = anyList(data)

    fun double(data: Map<Key, Map<Port<*>, Value?>>): Double? = any(data)
    fun doubles(data: Map<Key, Map<Port<*>, Value?>>): List<Double>? = anyList(data)
}

typealias Key = String

data class Port<T>(
    val id: String, val includeIntoScaling: Boolean = true,
    val extractValue: Map<Key, Map<Port<*>, Value?>>.(Slot<T>) -> T? = { slot ->
        @Suppress("UNCHECKED_CAST")
        this[slot.key]?.get(slot.port) as? T
    }
)

fun <T> Slot<T>.extractValue(data: Map<Key, Map<Port<*>, Value?>>): T? = port.extractValue(data, this)

infix fun <T> Key.with(port: Port<T>): Slot<T> = Slot(this, port)

typealias Value = Any


