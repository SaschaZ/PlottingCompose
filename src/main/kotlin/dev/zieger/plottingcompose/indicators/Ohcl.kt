package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.ValueContainer
import dev.zieger.plottingcompose.processor.ProcessingScope

class Ohcl : Indicator(key(Unit), listOf(OHCL)) {

    companion object : IndicatorDefinition<Unit>() {

        override fun key(param: Unit) = Key("Ohcl", param) { Ohcl() }
        fun key() = key(Unit)

        val OHCL = Port<Ohcl>("OHCL")

        data class Ohcl(
            override val open: Double, override val high: Double, override val close: Double,
            override val low: Double, override val volume: Long, override val openTime: Long
        ) : ICandle, ValueContainer {
            fun map(map: (Double) -> Double): Ohcl = Ohcl(
                map(open), map(high), map(close), map(low), volume, openTime
            )

            override val yRange: ClosedRange<Double> = low..high
            override val x: Double = openTime.toDouble()
        }
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        set(OHCL, Ohcl(value.open, value.high, value.close, value.low, value.volume, value.openTime))
    }
}