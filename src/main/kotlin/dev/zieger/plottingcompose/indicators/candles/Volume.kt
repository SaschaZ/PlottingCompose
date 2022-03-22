package dev.zieger.plottingcompose.indicators.candles

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.styles.ImpulseData

class Volume : Indicator<ICandle>(key(), listOf(VOLUME)) {

    companion object : IndicatorDefinition<Unit>() {

        override fun key(param: Unit) = Key("Volume", param) { Volume() }
        fun key() = key(Unit)

        val VOLUME = Port<ImpulseData>("VOLUME")
    }

    private var prev: ICandle? = null

    override suspend fun ProcessingScope<ICandle>.process() {
        set(VOLUME, ImpulseData(input.x, input.volume, prev?.let { it.close < input.close } == true))
        prev = input
    }
}