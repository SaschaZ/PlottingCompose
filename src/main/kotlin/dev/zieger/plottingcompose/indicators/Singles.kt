package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.extractValue
import dev.zieger.plottingcompose.definition.with
import dev.zieger.plottingcompose.processor.ProcessingScope

data class Singles(
    val length: Int,
    private val candles: Candles = Candles(length)
) : Indicator(
    key(length),
    listOf(OPENS, HIGHS, CLOSES, LOWS, VOLUMES), listOf(candles)
) {

    companion object {

        fun key(length: Int): Key = "Singles$length"

        val OPENS = Port<List<Float>>("Opens")
        val HIGHS = Port<List<Float>>("Highs")
        val CLOSES = Port<List<Float>>("Closes")
        val LOWS = Port<List<Float>>("Lows")
        val VOLUMES = Port<List<Float>>("Volumes")
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        (candles.key with Candles.CANDLES).extractValue(data)?.let { candles ->
            set(OPENS, candles.map { it.open })
            set(HIGHS, candles.map { it.high })
            set(CLOSES, candles.map { it.close })
            set(LOWS, candles.map { it.low })
            set(VOLUMES, candles.map { it.volume })
        }
    }
}