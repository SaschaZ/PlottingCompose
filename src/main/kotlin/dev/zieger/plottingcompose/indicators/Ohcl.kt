package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.processor.ProcessingScope

class Ohcl : Indicator(key(), listOf(OHCL)) {

    companion object {

        private fun key() = "Ohcl"

        val OHCL = Port<Ohcl>("OHCL")

        data class Ohcl(
            override val open: Double, override val high: Double, override val close: Double,
            override val low: Double, override val volume: Long, override val openTime: Long
        ) : ICandle {
            fun map(map: (Double) -> Double): Ohcl = Ohcl(
                map(open), map(high), map(close), map(low), volume, openTime
            )
        }
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        set(OHCL, Ohcl(value.open, value.high, value.close, value.low, value.volume, value.openTime))
    }
}