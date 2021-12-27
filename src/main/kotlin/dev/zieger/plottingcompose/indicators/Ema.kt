package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.indicators.Singles.Companion.CLOSES
import dev.zieger.plottingcompose.indicators.Singles.Companion.HIGHS
import dev.zieger.plottingcompose.indicators.Singles.Companion.LOWS
import dev.zieger.plottingcompose.indicators.Singles.Companion.OPENS
import dev.zieger.plottingcompose.indicators.Singles.Companion.VOLUMES
import dev.zieger.plottingcompose.processor.ProcessingScope

class Ema(private val length: Int, private val source: Port<List<Float>> = CLOSES) : Indicator(
    key(length, source), listOf(EMA)
) {

    companion object {

        fun key(length: Int, source: Port<List<Float>>) = "Ema$length$source"

        val EMA = Port<Float>("Ema")
    }

    private val k = 2.0 / (length + 1)
    private var ema: Double = 0.0

    override suspend fun ProcessingScope<ICandle>.process() {
        ema = when (source) {
            OPENS -> value.open
            HIGHS -> value.high
            CLOSES -> value.close
            LOWS -> value.low
            VOLUMES -> value.volume
            else -> throw IllegalArgumentException("Unknown source $source")
        }.toDouble() * k + ema * (1 - k)

        set(EMA, ema)
    }
}