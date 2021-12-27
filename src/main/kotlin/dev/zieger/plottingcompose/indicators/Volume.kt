package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.processor.ProcessingScope

class Volume : Indicator(key(), listOf(VOLUME)) {

    companion object {

        private fun key() = "Volume"

        val VOLUME = Port<Long>("VOLUME")
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        set(VOLUME, value.volume)
    }
}