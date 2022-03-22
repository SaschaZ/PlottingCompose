package dev.zieger.plottingcompose.indicators.candles

import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.processor.ProcessingScope

class Candles(val length: Int) : Indicator<ICandle>(
    key(length), listOf(CANDLES)
) {

    companion object : IndicatorDefinition<Int>() {

        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun key(length: Int) = Key("Candles", length) { Candles(length) }

        val CANDLES: Port<Output.Container<Ohcl.Companion.Ohcl>> = Port("Candles", false)
    }

    private var candles: HashMap<Long, ICandle> = HashMap()

    override suspend fun ProcessingScope<ICandle>.process() {
        candles[input.openTime] = input
        if (candles.size == length + 1)
            candles.remove(candles.minByOrNull { it.key }!!.key)

        set(CANDLES, Output.Container(
            candles.entries.sortedBy { (k, _) -> k }.map { (_, c) ->
                Ohcl.Companion.Ohcl(c.open, c.high, c.close, c.low, c.volume, c.openTime)
            })
        )
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

