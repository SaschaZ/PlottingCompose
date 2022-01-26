package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.processor.ProcessingScope

class Volume : Indicator(key(), listOf(VOLUME)) {

    companion object : IndicatorDefinition<Unit>() {

        override fun key(param: Unit) = Key("Volume", param) { Volume() }
        fun key() = key(Unit)

        val VOLUME = Port<Long>("VOLUME")
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        set(VOLUME, value.volume)
    }
}