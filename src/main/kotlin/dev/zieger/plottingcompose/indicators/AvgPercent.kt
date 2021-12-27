package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.extractValue
import dev.zieger.plottingcompose.definition.with
import dev.zieger.plottingcompose.indicators.Singles.Companion.VOLUMES
import dev.zieger.plottingcompose.processor.ProcessingScope

class AvgPercent(
    length: Int, source: Port<List<Float>>,
    private val avgType: AverageType = AverageType.SMA,
    private val singles: Singles = Singles(length),
    private val avg: Indicator = when (avgType) {
        AverageType.SMA -> Sma(length, source)
        AverageType.EMA -> Ema(length, source)
    }
) : Indicator(
    key(source, avgType), listOf(AVG_PERCENT), listOf(singles, avg)
) {

    companion object {
        private fun key(source: Port<List<Float>>, avgType: AverageType): String = "AvgPercent$source$avgType"
        val AVG_PERCENT = Port<Float>("AVG_PERCENT")
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        (singles.key with VOLUMES).extractValue(data)?.lastOrNull()?.let { single ->
            (avg.key with when (avgType) {
                AverageType.SMA -> Sma.SMA
                AverageType.EMA -> Ema.EMA
            }).extractValue(data)?.let { a ->
                set(AVG_PERCENT, single / a * 100)
            }
        }
    }
}