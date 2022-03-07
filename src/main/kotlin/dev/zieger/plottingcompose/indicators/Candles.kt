package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.processor.ProcessingScope

class Candles(val length: Int) : Indicator<ICandle>(
    key(length), listOf(CANDLES)
) {

    companion object : IndicatorDefinition<Int>() {

        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun key(length: Int) = Key("Candles", length) { Candles(length) }

        val CANDLES: Port<Output.Container<Ohcl.Companion.Ohcl>> = Port("Candles", false)
    }

    private var candles: List<ICandle> = emptyList()

    override suspend fun ProcessingScope<ICandle>.process() {
        candles = (candles + input).takeLast(length)

        set(CANDLES, Output.Container(candles.map {
            Ohcl.Companion.Ohcl(it.open, it.high, it.low, it.close, it.volume, it.openTime)
        }))
    }
}

interface ICandle : Input {

    val openTime: Long
    override val x: Number
        get() = openTime

    val open: Double
    val high: Double
    val close: Double
    val low: Double

    val volume: Long
}

