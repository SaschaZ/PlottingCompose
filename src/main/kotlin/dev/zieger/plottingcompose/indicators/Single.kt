package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.processor.ProcessingScope

class Single : Indicator(
    key(),
    listOf(OPEN, HIGH, CLOSE, LOW, VOLUME)
) {

    companion object : IndicatorDefinition<Unit>() {

        override fun key(param: Unit) = Key("Single", param) { Single() }
        fun key() = key(Unit)

        val OPEN = Port<Double>("Open")
        val HIGH = Port<Double>("High")
        val CLOSE = Port<Double>("Close")
        val LOW = Port<Double>("Low")
        val VOLUME = Port<Double>("Volume")
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        set(OPEN, value.open)
        set(HIGH, value.high)
        set(CLOSE, value.close)
        set(LOW, value.low)
        set(VOLUME, value.volume)
    }
}