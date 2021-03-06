package dev.zieger.plottingcompose.indicators.candles

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.styles.ImpulseData

class Volume : Indicator<IndicatorCandle>(key(), listOf(VOLUME)) {

    companion object : IndicatorDefinition<Unit>() {

        override fun key(param: Unit) = Key("Volume", param) { Volume() }
        fun key() = key(Unit)

        val VOLUME = Port<ImpulseData>("VOLUME")
    }

    override suspend fun ProcessingScope<IndicatorCandle>.process() {
        set(VOLUME, ImpulseData(input.x, input.volume, input.close > input.open))
    }
}