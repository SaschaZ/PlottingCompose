package dev.zieger.plottingcompose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import kotlin.math.absoluteValue
import kotlin.math.min

data class Series<T>(
    val items: List<SeriesItem<T>>,
    val preDrawer: List<PlotDrawer<T>> = emptyList(),
    val postDrawer: List<PlotDrawer<T>> = emptyList()
) {
    val z: List<Int> = items.flatMap { it.z }.distinct()

    fun IPlotDrawScope.preDrawerDraw(offsets: Map<SeriesItem<*>, Offset>) {
        preDrawer.forEach { it.run { draw(offsets.map { m -> m.key as SeriesItem<T> to m.value }.toMap()) } }
    }

    fun IPlotDrawScope.postDrawerDraw(offsets: Map<SeriesItem<*>, Offset>) {
        postDrawer.forEach { it.run { draw(offsets.map { m -> m.key as SeriesItem<T> to m.value }.toMap()) } }
    }
}

fun <T> List<SeriesItem<T>>.toSeries() = Series(this)

fun <T> Series<T>.withPreDrawer(drawer: PlotDrawer<T>): Series<T> = copy(preDrawer = preDrawer + drawer)
fun <T> Series<T>.withPostDrawer(drawer: PlotDrawer<T>): Series<T> = copy(postDrawer = postDrawer + drawer)

sealed class SeriesYValue {
    abstract val min: Number
    abstract val value: Number
    abstract val max: Number

    data class Single(val single: Number): SeriesYValue() {
        override val min: Number = single
        override val value: Number = single
        override val max: Number = single
    }
    data class Range(val range: ClosedRange<Float>): SeriesYValue() {
        override val min: Number = range.start
        override val value: Number = range.run { endInclusive - start } / 2
        override val max: Number = range.endInclusive
    }
}

open class SeriesItem<T>(
    private val data: T,
    val x: Number,
    val y: SeriesYValue,
    vararg style: PlotStyle<T> = arrayOf(Focusable(Dot(), Dot(width = 50f)), Line())
) {
    constructor(data: T, x: Number, y: Number, vararg style: PlotStyle<T>) :
            this(data, x, SeriesYValue.Single(y), *style)

    open val yMin: Number = y.min
    open val yMax: Number = y.max

    private val styles: List<PlotStyle<T>> = style.toList()

    val isFocused: MutableState<Boolean> = mutableStateOf(false)

    val z: List<Int> = styles.flatMap { it.z }.distinct()

    fun IPlotDrawScope.draw(offset: Offset, previousOffset: Offset?, z: Int, map: Offset.() -> Offset) =
        styles.filter { z in it.z }.forEach { style ->
            style.run { draw(data, offset, previousOffset, isFocused.value, z, map) }
        }
}

class OhclItem(
    ohcl: Ohcl,
    vararg styles: PlotStyle<Ohcl> = arrayOf(CandleSticks())
) : SeriesItem<Ohcl>(ohcl, ohcl.time, ohcl.mid, *styles) {
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

interface Ohcl {
    val time: Long
    val open: Float
    val high: Float
    val close: Float
    val low: Float
    val volume: Long

    val mid: Float
        get() = (low + ((high - low) / 2) + (min(open, close) + (open - close).absoluteValue / 2)) / 2
}

data class OhclValue(
    override val time: Long,
    override val open: Float,
    override val high: Float,
    override val close: Float,
    override val low: Float,
    override val volume: Long
) : Ohcl
