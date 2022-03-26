package dev.zieger.plottingcompose.indicators.candles

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.processor.ProcessingScope

class Ohcl : Indicator<ICandle>(key(), listOf(OHCL)) {

    companion object : IndicatorDefinition<Unit>() {

        override fun key(param: Unit) = Key("Ohcl", param) { Ohcl() }
        fun key() = key(Unit)

        val OHCL = Port<Ohcl>("OHCL")

        data class Ohcl(
            override val open: Double, override val high: Double, override val close: Double,
            override val low: Double, override val volume: Double, override val openTime: Long
        ) : ICandle, Output.Vector(openTime, listOf(open, high, close, low)) {
            override val x: Number = openTime
        }
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        set(OHCL, Ohcl(input.open, input.high, input.close, input.low, input.volume, input.openTime))
    }
}