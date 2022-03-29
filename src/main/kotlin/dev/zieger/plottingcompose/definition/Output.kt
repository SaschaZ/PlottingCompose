package dev.zieger.plottingcompose.definition

sealed class Output {
    abstract val xRange: ClosedRange<Double>
    abstract val yRange: ClosedRange<Double>

    open class Scalar(open val x: Number, open val scalar: Number) : Output() {
        override val xRange: ClosedRange<Double> get() = x.toDouble()..x.toDouble()
        override val yRange: ClosedRange<Double> get() = scalar.toDouble()..scalar.toDouble()
    }

    open class Offset(open val offset: androidx.compose.ui.geometry.Offset) : Output() {
        override val xRange: ClosedRange<Double> = offset.x.toDouble()..offset.x.toDouble()
        override val yRange: ClosedRange<Double> = offset.y.toDouble()..offset.y.toDouble()
    }

    open class Line(
        open val start: androidx.compose.ui.geometry.Offset,
        open val end: androidx.compose.ui.geometry.Offset
    ) : Output() {
        override val xRange: ClosedRange<Double> = start.x.toDouble()..end.x.toDouble()
        override val yRange: ClosedRange<Double> = start.y.toDouble()..end.y.toDouble()
    }

    open class Vector(open val x: Number, open val vector: Collection<Number>) : Output() {
        override val xRange: ClosedRange<Double> get() = x.toDouble()..x.toDouble()
        override val yRange: ClosedRange<Double> get() = vector.minOf { it.toDouble() }..vector.maxOf { it.toDouble() }
    }

    open class OffsetVector(open val offsets: Collection<androidx.compose.ui.geometry.Offset>) : Output() {
        override val xRange: ClosedRange<Double> = offsets.minOf { it.x.toDouble() }..offsets.maxOf { it.x.toDouble() }
        override val yRange: ClosedRange<Double> = offsets.minOf { it.y.toDouble() }..offsets.maxOf { it.y.toDouble() }
    }

    open class Label(x: Number, y: Number, val label: String) : Scalar(x, y)
    open class Lambda(x: Number, y: Number, val lambda: (Number) -> Boolean) : Scalar(x, y)
    open class Container<V : Output>(open val items: List<V>) : Output() {
        override val xRange: ClosedRange<Double> get() = items.minOf { it.xRange.start }..items.maxOf { it.xRange.endInclusive }
        override val yRange: ClosedRange<Double> get() = items.minOf { it.yRange.start }..items.maxOf { it.yRange.endInclusive }
    }
}

fun <V : Output.Scalar> List<V>.asFloats(): List<Float> =
    map { it.scalar.toFloat() }