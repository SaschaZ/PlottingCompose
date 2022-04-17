@file:Suppress("MemberVisibilityCanBePrivate", "unused", "CanBeParameter", "FunctionName")

package dev.zieger.plottingcompose.strategy

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.indicators.candles.BbValues
import dev.zieger.plottingcompose.indicators.candles.BollingerBands
import dev.zieger.plottingcompose.indicators.candles.BollingerBandsParameter
import dev.zieger.plottingcompose.indicators.candles.ICandle
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.strategy.dto.BearBuy
import dev.zieger.plottingcompose.strategy.dto.BearSell
import dev.zieger.plottingcompose.strategy.dto.BullBuy
import dev.zieger.plottingcompose.strategy.dto.BullSell
import dev.zieger.plottingcompose.strategy.dto.Direction.BUY
import dev.zieger.plottingcompose.strategy.dto.Direction.SELL
import dev.zieger.plottingcompose.strategy.dto.Pairs.XBTUSD
import dev.zieger.utils.misc.max
import dev.zieger.utils.misc.min

data class BbStrategyOptions(
    val bbOptions: BollingerBandsParameter = BollingerBandsParameter(),
    val strategyParams: StrategyParameter = StrategyParameter(),
    val initialCash: Long = strategyParams.initialCash,
    val leverage: Double = strategyParams.leverage,
    val dcaMinLossPercentForOrder: Double = 2.0,
    val dcaFactor: Double = 2.0,
    val dcaStart: Int = 1,
    val dcaNumMax: Int = strategyParams.maxOrdersPerSide,
    val stopLossFactor: Int = 2,
    val calcPriceIdxFactor: Double = 1.0
)

class BbStrategy(
    private val param: BbStrategyOptions = BbStrategyOptions(),
    internalExchange: Exchange<ICandle> = MockExchange(
        MockExchangeParameter(
            param.initialCash.toDouble(),
            param.leverage
        )
    )
) : Strategy<ICandle>(
    param.strategyParams, internalExchange, key(),
    listOf(), BollingerBands.key(param.bbOptions)
), DcaTool {

    companion object : IndicatorDefinition<BbStrategyOptions>() {
        override fun key(param: BbStrategyOptions) = Key("BbStrategy", param) { BbStrategy(param) }
        fun key() = key(BbStrategyOptions())
    }

    private var skipped = 0

    override suspend fun ProcessingScope<ICandle>.placeOrders() {
        if (++skipped < 200) return

        (BollingerBands.key(param.bbOptions) dataOf BollingerBands.BB_VALUES)?.also { bb ->
            placeOrders2(bb)
        }
    }

    private suspend fun ProcessingScope<ICandle>.placeOrders2(bb: BbValues) {
        placeBullOrders(bb)
        placeBearOrders(bb)
    }

    private suspend fun ProcessingScope<ICandle>.placeBullOrders(bb: BbValues) {
        val p = position?.takeIf { it.direction == BUY }
        val lastPrice = p?.enterTrades?.maxOfOrNull { it.order.counterPrice } ?: min(input.low, bb.low)
        ((p?.enterTrades?.size ?: 0) until param.maxNumDca() step 1).forEach { slotId ->
            val price = param.calcDcaPrice(lastPrice, BUY, slotId)
            bullBuys[slotId]?.change(price)
                ?: order(BullBuy(slotId, XBTUSD, input.x, price, param.calcDcaVolume(slotId)))
        }
        (param.maxNumDca() until param.dcaNumMax step 1).forEach { slotId ->
            bullBuys[slotId]?.cancel()
        }

        if (p?.direction == BUY)
            bullSells[0]?.change(
                bb.high - counterTickSize(bb.high),
                p.exitCounterVolumeDiff(bb.high - counterTickSize(bb.high))
            ) ?: order(
                BullSell(
                    0, XBTUSD, input.x, bb.high - counterTickSize(bb.high),
                    p.exitCounterVolumeDiff(bb.high - counterTickSize(bb.high))
                )
            )
    }

    private suspend fun ProcessingScope<ICandle>.placeBearOrders(bb: BbValues) {
        val p = position?.takeIf { it.direction == SELL }
        val lastPrice = p?.enterTrades?.minOfOrNull { it.order.counterPrice } ?: max(input.high, bb.high)
        ((p?.enterTrades?.size ?: 0) until param.maxNumDca() step 1).forEach { slotId ->
            val price = param.calcDcaPrice(lastPrice, SELL, slotId)
            bearSells[slotId]?.change(price)
                ?: order(BearSell(slotId, XBTUSD, input.x, price, param.calcDcaVolume(slotId)))
        }
        (param.maxNumDca() until param.dcaNumMax step 1).forEach { slotId ->
            bearSells[slotId]?.cancel()
        }

        if (p?.direction == SELL)
            bearBuys[0]?.change(
                bb.low + counterTickSize(bb.low),
                p.exitCounterVolumeDiff(bb.low + counterTickSize(bb.low))
            ) ?: order(
                BearBuy(
                    0, XBTUSD, input.x, bb.low + counterTickSize(bb.low),
                    p.exitCounterVolumeDiff(bb.low + counterTickSize(bb.low))
                )
            )
    }
}