package dev.zieger.plottingcompose.indicators.candles

import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.processor.ProcessingScope

data class SinglesParameter(
    val length: Int,
    val candles: Slot<ICandle, Output.Container<Ohcl.Companion.Ohcl>> =
        Candles.key(length) with Candles.CANDLES
)

data class Singles(
    val param: SinglesParameter
) : Indicator<ICandle>(
    key(param),
    listOf(OPENS, HIGHS, CLOSES, LOWS, VOLUMES), param.candles.key
) {

    companion object : IndicatorDefinition<SinglesParameter>() {

        override fun key(param: SinglesParameter) = Key("Singles", param) { Singles(param) }
        fun key(length: Int) = key(SinglesParameter(length))

        val OPENS = Port<Output.Container<Output.Scalar>>("Opens")
        val HIGHS = Port<Output.Container<Output.Scalar>>("Highs")
        val CLOSES = Port<Output.Container<Output.Scalar>>("Closes")
        val LOWS = Port<Output.Container<Output.Scalar>>("Lows")
        val VOLUMES = Port<Output.Container<Output.Scalar>>("Volumes")
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        param.candles.value(data)?.let { candles ->
            set(OPENS, Output.Container(candles.items.map { Output.Scalar(it.x, it.open) }))
            set(HIGHS, Output.Container(candles.items.map { Output.Scalar(it.x, it.high) }))
            set(CLOSES, Output.Container(candles.items.map { Output.Scalar(it.x, it.close) }))
            set(LOWS, Output.Container(candles.items.map { Output.Scalar(it.x, it.low) }))
            set(VOLUMES, Output.Container(candles.items.map { Output.Scalar(it.x, it.volume) }))
        }
    }
}