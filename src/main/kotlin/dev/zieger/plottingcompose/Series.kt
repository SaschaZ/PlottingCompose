package dev.zieger.plottingcompose

import kotlin.math.absoluteValue
import kotlin.math.min

class Series<T : PlotItem>(
    val items: List<SeriesItem<T>>,
    vararg style: PlotStyle<T>
) {
    val xRange = items.minOf { it.data.xMin }..items.maxOf { it.data.xMax }
    val xWidth = xRange.run { endInclusive - start }
    val yRange = items.minOf { it.data.yMin }..items.maxOf { it.data.yMax }
    val yHeight = yRange.run { endInclusive - start }

    val styles = style.toList()

    val z: List<Int> = styles.map { it.z }

    init {
        items.forEach { it.parent = this }
    }
}

val List<Series<*>>.xRange get() = minOf { it.xRange.start }..maxOf { it.xRange.endInclusive }
val List<Series<*>>.xWidth get() = xRange.run { endInclusive - start }
val List<Series<*>>.yRange get() = minOf { it.yRange.start }..maxOf { it.yRange.endInclusive }
val List<Series<*>>.yHeight get() = yRange.run { endInclusive - start }

fun <T : PlotItem> List<SeriesItem<T>>.toSeries(vararg style: PlotStyle<T>) = Series(this, *style)

open class SeriesItem<out T : PlotItem>(
    val data: T
) {

    companion object {

        operator fun invoke(x: Number, y: Number) = SeriesItem(SimplePlotItem(x.toFloat(), y.toFloat()))
    }

    lateinit var parent: Series<*>

    open val yMin: Number = data.yMin
    open val yMax: Number = data.yMax
}

class OhclItem(
    ohcl: Ohcl
) : SeriesItem<Ohcl>(ohcl) {
    constructor(
        time: Long,
        open: Float,
        high: Float,
        close: Float,
        low: Float,
        volume: Long
    ) : this(OhclValue(time, open, high, close, low, volume))

    override val yMin: Number = ohcl.low
    override val yMax: Number = ohcl.high
}

interface Ohcl : PlotItem {
    val time: Long
    val open: Float
    val high: Float
    val close: Float
    val low: Float
    val volume: Long

    val mid: Float
        get() = (low + ((high - low) / 2) + (min(open, close) + (open - close).absoluteValue / 2)) / 2

    override val yMin: Float get() = low
    override val yMax: Float get() = high
}

data class OhclValue(
    override val time: Long,
    override val open: Float,
    override val high: Float,
    override val close: Float,
    override val low: Float,
    override val volume: Long
) : Ohcl {
    override val x: Float = time.toFloat()
    override val y: Float = close + (open - close) / 2
    override var hasFocus: Boolean = false
}
