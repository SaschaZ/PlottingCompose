package dev.zieger.plottingcompose.strategy

import dev.zieger.plottingcompose.strategy.dto.Direction
import kotlin.math.log10
import kotlin.math.pow

interface DcaTool {

    val availableBalance: Double

    fun BbStrategyOptions.maxNumDca(initialOrderSize: Int = maxDcaStart(dcaNumMax)): Int =
        (log10(availableBalance / initialOrderSize) / log10(dcaFactor)).toInt()

    fun BbStrategyOptions.maxDcaStart(numDca: Int = dcaNumMax): Int =
        (availableBalance / dcaFactor.pow(numDca)).toInt().coerceAtLeast(1)

    fun BbStrategyOptions.calcDcaPrice(startPrice: Double, direction: Direction, idx: Int): Double =
        startPrice + startPrice * dcaMinLossPercentForOrder / 100 * idx * calcPriceIdxFactor * when (direction) {
            Direction.BUY -> -1
            Direction.SELL -> 1
        }

    fun BbStrategyOptions.calcDcaVolume(idx: Int): Double =
        (dcaStart * dcaFactor.pow(idx))
}