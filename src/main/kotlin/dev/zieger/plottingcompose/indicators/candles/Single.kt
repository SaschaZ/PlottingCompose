package dev.zieger.plottingcompose.indicators.candles

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.processor.ProcessingScope

class Single : Indicator<ICandle>(
    key(),
    listOf(OPEN, HIGH, CLOSE, LOW, VOLUME)
) {

    companion object : IndicatorDefinition<Unit>() {

        override fun key(param: Unit) = Key("Single", param) { Single() }
        fun key() = key(Unit)

        val OPEN = Port<Output.Scalar>("Open")
        val HIGH = Port<Output.Scalar>("High")
        val CLOSE = Port<Output.Scalar>("Close")
        val LOW = Port<Output.Scalar>("Low")
        val VOLUME = Port<Output.Scalar>("Volume")
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        set(OPEN, Output.Scalar(input.x, input.open))
        set(HIGH, Output.Scalar(input.x, input.high))
        set(CLOSE, Output.Scalar(input.x, input.close))
        set(LOW, Output.Scalar(input.x, input.low))
        set(VOLUME, Output.Scalar(input.x, input.volume))
    }
}