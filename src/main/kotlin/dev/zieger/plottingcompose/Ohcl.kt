package dev.zieger.plottingcompose

import kotlin.math.absoluteValue
import kotlin.math.min

interface Ohcl : PlotItem {
    val time: Long
    val open: Float
    val high: Float
    val close: Float
    val low: Float
    val volume: Long

    override val x: Float
        get() = time.toFloat()
    override val y: Map<Int, Float?>
        get() = mapOf(0 to open, 1 to high, 2 to close, 3 to low)

    val mid: Float
        get() = (low + ((high - low) / 2) + (min(open, close) + (open - close).absoluteValue / 2)) / 2

    override val yMin: Float get() = low
    override val yMax: Float get() = high
}

class OhclItem(
    ohcl: Ohcl,
    vararg style: PlotStyle.SinglePlotStyle<Ohcl> = arrayOf(CandleSticks()),
) : PlotSeriesItem<Ohcl>(ohcl, *style) {
    constructor(
        time: Long,
        open: Float,
        high: Float,
        close: Float,
        low: Float,
        volume: Long
    ) : this(OhclValue(time, open, high, close, low, volume))

    override val yMin: Float = ohcl.low
    override val yMax: Float = ohcl.high
}

data class OhclValue(
    override val time: Long,
    override val open: Float,
    override val high: Float,
    override val close: Float,
    override val low: Float,
    override val volume: Long
) : Ohcl {
    override var hasFocus: Boolean = false

    override fun copy(x: Float, y: Map<Int, Float?>, hasFocus: Boolean) = OhclValue(
        x.toLong(), y[0]!!, y[1]!!, y[2]!!, y[3]!!, volume
    ).apply { this.hasFocus = hasFocus }
}