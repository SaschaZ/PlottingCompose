package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Port


enum class AverageType {
    SMA,
    EMA;

    operator fun invoke(length: Int, source: Port<List<Float>> = Singles.CLOSES): Indicator =
        when (this) {
            SMA -> Sma(length, source)
            EMA -> Ema(length, source)
        }
}