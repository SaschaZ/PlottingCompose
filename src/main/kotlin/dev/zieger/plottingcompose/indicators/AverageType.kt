package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.definition.with


enum class AverageType {
    SMA,
    EMA;

    operator fun invoke(
        length: Int,
        smaSource: Slot<List<Double>, ICandle> = Singles.key(length) with Singles.CLOSES,
        emaSource: Slot<Double, ICandle> = Single.key() with Single.CLOSE
    ): Key<ICandle> =
        when (this) {
            SMA -> Sma.key(length, smaSource)
            EMA -> Ema.key(length, emaSource)
        }
}