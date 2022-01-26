package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.definition.with
import dev.zieger.plottingcompose.processor.ProcessingScope

data class SinglesParameter(
    val length: Int,
    val candles: Slot<List<ICandle>, ICandle> = Candles.key(length) with Candles.CANDLES
)

data class Singles(
    val param: SinglesParameter
) : Indicator(
    key(param),
    listOf(OPENS, HIGHS, CLOSES, LOWS, VOLUMES), param.candles.key
) {

    companion object : IndicatorDefinition<SinglesParameter>() {

        override fun key(param: SinglesParameter) = Key("Singles", param) { Singles(param) }
        fun key(length: Int) = key(SinglesParameter(length))

        val OPENS = Port<List<Double>>("Opens")
        val HIGHS = Port<List<Double>>("Highs")
        val CLOSES = Port<List<Double>>("Closes")
        val LOWS = Port<List<Double>>("Lows")
        val VOLUMES = Port<List<Double>>("Volumes")
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        param.candles.value(data)?.let { candles ->
            set(OPENS, candles.map { it.open })
            set(HIGHS, candles.map { it.high })
            set(CLOSES, candles.map { it.close })
            set(LOWS, candles.map { it.low })
            set(VOLUMES, candles.map { it.volume })
        }
    }
}