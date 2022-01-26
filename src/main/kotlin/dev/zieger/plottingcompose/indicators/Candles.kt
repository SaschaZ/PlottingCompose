package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.processor.ProcessingScope

class Candles(val length: Int) : Indicator(
    key(length), listOf(CANDLES)
) {

    companion object : IndicatorDefinition<Int>() {

        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun key(length: Int) = Key("Candles", length) { Candles(length) }

        val CANDLES: Port<List<ICandle>> = Port("Candles", false)
    }

    private var candles: List<ICandle> = emptyList()

    override suspend fun ProcessingScope<ICandle>.process() {
        candles = (candles + value).takeLast(length)

        set(CANDLES, candles)
    }
}

interface ICandle : InputContainer {

    val openTime: Long
    override val x: Double
        get() = openTime.toDouble()

    val open: Double
    val high: Double
    val close: Double
    val low: Double

    val volume: Long
}

