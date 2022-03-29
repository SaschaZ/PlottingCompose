@file:Suppress("MemberVisibilityCanBePrivate", "unused", "CanBeParameter", "FunctionName")

package dev.zieger.plottingcompose.strategy

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.indicators.candles.BbValues
import dev.zieger.plottingcompose.indicators.candles.BollingerBands
import dev.zieger.plottingcompose.indicators.candles.BollingerBandsParameter
import dev.zieger.plottingcompose.indicators.candles.ICandle
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.strategy.dto.Direction.BUY
import dev.zieger.plottingcompose.strategy.dto.Direction.SELL
import dev.zieger.plottingcompose.strategy.dto.Order
import dev.zieger.plottingcompose.strategy.dto.Pairs.XBTUSD
import dev.zieger.plottingcompose.strategy.dto.RemoteOrder
import dev.zieger.utils.misc.max
import dev.zieger.utils.misc.min
import dev.zieger.utils.misc.nullWhen
import kotlin.math.pow

data class BbStrategyOptions(
    val bbOptions: BollingerBandsParameter = BollingerBandsParameter(),
    val strategyParams: StrategyParameter = StrategyParameter(),
    val initialCash: Long = 1_000,
    val leverage: Double = 5.0,
    val dcaNumMax: Int = 3,
    val nextBullPrice: (idx: Int, lastPrice: Double) -> Double = { idx, lastPrice ->
        val step = lastPrice * 0.05
        lastPrice - step.pow(idx)
    },
    val nextBearPrice: (idx: Int, lastPrice: Double) -> Double = { idx, lastPrice ->
        val step = lastPrice * 0.05
        lastPrice + step.pow(idx)
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

    private val bullBuys = HashMap<Int, RemoteOrder>(param.dcaNumMax)
    private val bullSells = HashMap<Int, RemoteOrder>(param.dcaNumMax)
    private val bearSells = HashMap<Int, RemoteOrder>(param.dcaNumMax)
    private val bearBuys = HashMap<Int, RemoteOrder>(param.dcaNumMax)

    override suspend fun ProcessingScope<ICandle>.process() {
        processUnreferenced(this)
        if (++skipped < 200) return

        (BollingerBands.key(param.bbOptions) dataOf BollingerBands.BB_VALUES)?.also { bb ->
            placeOrders(bb)
        }
    }

    private fun ProcessingScope<ICandle>.placeOrders(bb: BbValues) {
        placeBullOrders(bb)
        placeBearOrders(bb)
    }

    private fun ProcessingScope<ICandle>.placeBullOrders(bb: BbValues) {
        val p = position?.nullWhen { it.direction != BUY }
        var lastPrice = max(p?.enterTrades?.maxOfOrNull { it.order.counterPrice }, input.low, bb.low)
        ((p?.enterTrades?.size ?: 0)..param.dcaNumMax step 1).forEach { slotId ->
            lastPrice = param.nextBullPrice(slotId, lastPrice)
            bullBuys[slotId]?.change(lastPrice) ?: run {
                bullBuys[slotId] = order(Order(XBTUSD, input.x, lastPrice, param.nextVolume(slotId), BUY))
            }
        }

        if (position?.direction == BUY)
            bullSells[0]?.change(position.exitBaseVolumeDiff(bb.high)) ?: run {
                bullSells[0] = order(Order(XBTUSD, input.x, bb.high - 1, position.exitBaseVolumeDiff(bb.high), SELL))
            }
    }

    private fun ProcessingScope<ICandle>.placeBearOrders(bb: BbValues) {
        val p = position?.nullWhen { it.direction != SELL }
        var lastPrice = min(p?.enterTrades?.minOfOrNull { it.order.counterPrice }, input.high, bb.high)
        ((p?.enterTrades?.size ?: 0)..param.dcaNumMax step 1).forEach { slotId ->
            lastPrice = param.nextBearPrice(slotId, lastPrice)
            bearSells[slotId]?.change(lastPrice) ?: run {
                bearSells[slotId] = order(Order(XBTUSD, input.x, lastPrice, param.nextVolume(slotId), SELL))
            }
        }

        if (position?.direction == SELL)
            bearBuys[0]?.change(position.exitBaseVolumeDiff(bb.low)) ?: run {
                bearBuys[0] = order(Order(XBTUSD, input.x, bb.low + 1, position.exitBaseVolumeDiff(bb.low), BUY))
            }
    }
}