package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.processor.ProcessingScope

class Candles(val length: Int) : Indicator(
    key(length), listOf(CANDLES)
) {

    companion object {

        fun key(length: Int): Key = "Candles$length"

        val CANDLES: Port<List<ICandle>> = Port("Candles", false) { slot ->
            slot.anyList(this)
        }
    }

    private var candles: List<ICandle> = emptyList()

    override suspend fun ProcessingScope<ICandle>.process() {
        candles = (candles + value).takeLast(length)

        set(CANDLES, candles)
    }
}

interface ICandle {

    val openTime: Long

    val open: Double
    val high: Double
    val close: Double
    val low: Double

    val volume: Long
}

