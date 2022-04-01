@file:Suppress("MemberVisibilityCanBePrivate", "unused", "CanBeParameter", "FunctionName")

package dev.zieger.plottingcompose.strategy

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.indicators.candles.BbValues
import dev.zieger.plottingcompose.indicators.candles.BollingerBands
import dev.zieger.plottingcompose.indicators.candles.BollingerBandsParameter
import dev.zieger.plottingcompose.indicators.candles.ICandle
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.strategy.dto.*
import dev.zieger.plottingcompose.strategy.dto.Direction.BUY
import dev.zieger.plottingcompose.strategy.dto.Direction.SELL
import dev.zieger.plottingcompose.strategy.dto.Pairs.XBTUSD
import dev.zieger.utils.misc.max
import dev.zieger.utils.misc.min
import dev.zieger.utils.misc.nullWhen
import kotlin.math.pow

data class BbStrategyOptions(
    val bbOptions: BollingerBandsParameter = BollingerBandsParameter(),
    val strategyParams: StrategyParameter = StrategyParameter(),
    val initialCash: Long = 1_000,
    val leverage: Double = 5.0,
    val dcaNumMax: Int = 4,
    val nextBullPrice: (idx: Int, lastPrice: Double) -> Double = { idx, lastPrice ->
        when (idx) {
            0 -> lastPrice
            else -> {
                val step = lastPrice * 0.05
                lastPrice - step * 2.0.pow(idx - 1)
            }
        }
    },
    val nextBearPrice: (idx: Int, lastPrice: Double) -> Double = { idx, lastPrice ->
        when (idx) {
            0 -> lastPrice
            else -> {
                val step = lastPrice * 0.05
                lastPrice + step * 2.0.pow(idx - 1)
            }
        }
    },
    val nextVolume: (idx: Int) -> Double = { idx ->
        (2.0.pow(idx))
    }
)

class BbStrategy(
    private val param: BbStrategyOptions = BbStrategyOptions(),
    internalExchange: Exchange<ICandle> = MockExchange()
) : Strategy<ICandle>(
    param.strategyParams, internalExchange, key(),
    listOf(), BollingerBands.key(param.bbOptions)
) {

    companion object : IndicatorDefinition<BbStrategyOptions>() {
        override fun key(param: BbStrategyOptions) = Key("BbStrategy", param) { BbStrategy(param) }
        fun key() = key(BbStrategyOptions())
    }

    private var skipped = 0

    private val bullBuys = HashMap<Int, RemoteOrder<BullBuy>>(param.dcaNumMax)
    private val bullSells = HashMap<Int, RemoteOrder<BullSell>>(param.dcaNumMax)
    private val bearSells = HashMap<Int, RemoteOrder<BearSell>>(param.dcaNumMax)
    private val bearBuys = HashMap<Int, RemoteOrder<BearBuy>>(param.dcaNumMax)

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
        val p = position?.nullWhen { it.direction != BUY }
        val lastPrice = p?.enterTrades?.maxOfOrNull { it.order.counterPrice } ?: min(input.low, bb.low)
        ((p?.enterTrades?.size ?: 0)..param.dcaNumMax step 1).forEach { slotId ->
            val price = param.nextBullPrice(slotId, lastPrice)
            bullBuys[slotId]?.change(price) ?: run {
                bullBuys[slotId] = order(BullBuy(slotId, XBTUSD, input.x, price, param.nextVolume(slotId), BUY))
                    .onExecuted {
                        bullBuys.remove(slotId)
                        false
                    }.onCancelled {
                        bullBuys.remove(slotId)
                        false
                    }
            }
        }

        if (position?.direction == BUY)
            bullSells[0]?.change(bb.high - 1, position.exitCounterVolumeDiff(bb.high - 1)) ?: run {
                bullSells[0] =
                    order(BullSell(0, XBTUSD, input.x, bb.high - 1, position.exitCounterVolumeDiff(bb.high - 1), SELL))
                        .onExecuted {
                            bullSells.remove(0)
                            false
                        }.onCancelled {
                            bullSells.remove(0)
                            false
                        }
            }
    }

    private suspend fun ProcessingScope<ICandle>.placeBearOrders(bb: BbValues) {
        val p = position?.nullWhen { it.direction != SELL }
        val lastPrice = p?.enterTrades?.minOfOrNull { it.order.counterPrice } ?: max(input.high, bb.high)
        ((p?.enterTrades?.size ?: 0)..param.dcaNumMax step 1).forEach { slotId ->
            val price = param.nextBearPrice(slotId, lastPrice)
            bearSells[slotId]?.change(price) ?: run {
                bearSells[slotId] = order(BearSell(slotId, XBTUSD, input.x, price, param.nextVolume(slotId), SELL))
                    .onExecuted {
                        bearSells.remove(slotId)
                        false
                    }.onCancelled {
                        bearSells.remove(slotId)
                        false
                    }
            }
        }

        if (position?.direction == SELL)
            bearBuys[0]?.change(bb.low + 1, position.exitCounterVolumeDiff(bb.low + 1)) ?: run {
                bearBuys[0] =
                    order(BearBuy(0, XBTUSD, input.x, bb.low + 1, position.exitCounterVolumeDiff(bb.low + 1), BUY))
                        .onExecuted {
                            bearBuys.remove(0)
                            false
                        }.onCancelled {
                            bearBuys.remove(0)
                            false
                        }
            }
    }
}