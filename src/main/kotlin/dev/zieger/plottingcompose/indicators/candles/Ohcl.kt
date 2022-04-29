package dev.zieger.plottingcompose.indicators.candles

import dev.zieger.exchange.dto.*
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.utils.time.ITimeStamp
import dev.zieger.utils.time.hours
import dev.zieger.utils.time.toTime

class Ohcl : Indicator<IndicatorCandle>(key(), listOf(OHCL)) {

    companion object : IndicatorDefinition<Unit>() {

        override fun key(param: Unit) = Key("Ohcl", param) { Ohcl() }
        fun key() = key(Unit)

        val OHCL = Port<Ohcl>("OHCL")

        data class Ohcl(
            override val open: Double, override val high: Double, override val close: Double,
            override val low: Double, override val volume: Long, override val openTime: Long
        ) : IndicatorCandle, Output.Vector(openTime, listOf(open, high, close, low)) {

            override val x: Number = openTime
            override val time: ITimeStamp = openTime.toTime()
            override val interval: IInterval = IntervalSurrogate(1.hours)
            override val symbol: ISymbol =
                SymbolSurrogate(CurrencySurrogate(""), CurrencySurrogate(""))
        }
    }

    override suspend fun ProcessingScope<IndicatorCandle>.process() {
        set(OHCL, Ohcl(input.open, input.high, input.close, input.low, input.volume, input.openTime))
    }
}