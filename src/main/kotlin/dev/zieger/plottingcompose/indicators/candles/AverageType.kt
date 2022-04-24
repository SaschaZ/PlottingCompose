package dev.zieger.plottingcompose.indicators.candles

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.definition.with


enum class AverageType {
    SMA,
    EMA;

    operator fun invoke(
        length: Int,
        smaSource: Slot<IndicatorCandle, Output.Container<Output.Scalar>> = Singles.key(SinglesParameter(length)) with Singles.CLOSES,
        emaSource: Slot<IndicatorCandle, Output.Scalar> = Single.key() with Single.CLOSE
    ): Key<IndicatorCandle, *> =
        when (this) {
            SMA -> Sma.key(length, smaSource)
            EMA -> Ema.key(length, emaSource)
        }
}