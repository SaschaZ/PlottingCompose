package dev.zieger.plottingcompose

import androidx.compose.ui.geometry.Offset
import kotlin.math.absoluteValue
import kotlin.math.min

interface Ohcl : PlotItem<Position.Raw> {
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

data class OhclValue(
    override val time: Long,
    override val open: Float,
    override val high: Float,
    override val close: Float,
    override val low: Float,
    override val volume: Long
) : Ohcl {
    override val position: Position.Raw = Position.Raw(
        Offset(time.toFloat(), close),
        yRange = low..high
    )
    override var hasFocus: Boolean = false
}