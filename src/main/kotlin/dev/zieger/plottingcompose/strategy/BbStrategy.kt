@file:Suppress("MemberVisibilityCanBePrivate", "unused", "CanBeParameter")

package dev.zieger.plottingcompose.strategy

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.indicators.candles.BbValues
import dev.zieger.plottingcompose.indicators.candles.BollingerBands
import dev.zieger.plottingcompose.indicators.candles.BollingerBandsParameter
import dev.zieger.plottingcompose.indicators.candles.ICandle
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.scopes.nullWhenEmpty
import dev.zieger.utils.misc.max
import dev.zieger.utils.misc.min
import dev.zieger.utils.misc.nullWhen
import java.util.*
import kotlin.math.pow

data class BbStrategyOptions(
    val bbOptions: BollingerBandsParameter = BollingerBandsParameter(),
    val initialCash: Long = 1_000,
    val leverage: Double = 5.0,
    val dcaNumMax: Int = 15,
    val nextBullPrice: (idx: Int, lastPrice: Double) -> Double = { idx, lastPrice ->
        val step = lastPrice * 0.1
        lastPrice + step.pow(idx)
    },
    val nextBearPrice: (idx: Int, lastPrice: Double) -> Double = { idx, lastPrice ->
        val step = lastPrice * 0.1
        lastPrice - step.pow(idx)
    },
    val nextVolume: (idx: Int) -> Long = { idx ->
        (2.0.pow(idx)).toLong()
    }
)

class BbStrategy(
    private val param: BbStrategyOptions = BbStrategyOptions()
) : Indicator<ICandle>(
    key(), listOf(
        BULL_BUY_ORDERS, BULL_SELL_ORDERS, BEAR_SELL_ORDERS, BEAR_BUY_ORDERS,
        BULL_SELL_TRADES, BULL_BUY_TRADES, BEAR_SELL_TRADES, BEAR_BUY_TRADES,
        POSITION
    ), BollingerBands.key(param.bbOptions)
) {

    companion object : IndicatorDefinition<BbStrategyOptions>() {
        override fun key(param: BbStrategyOptions) = Key("BbStrategy", param) { BbStrategy(param) }
        fun key() = key(BbStrategyOptions())

        val BULL_BUY_ORDERS = Port<Output.Container<Order>>("BullBuyOrders")
        val BULL_SELL_ORDERS = Port<Output.Container<Order>>("BullSellOrders")
        val BEAR_BUY_ORDERS = Port<Output.Container<Order>>("BearBuyOrders")
        val BEAR_SELL_ORDERS = Port<Output.Container<Order>>("BearSellOrders")

        val BULL_BUY_TRADES = Port<Output.Container<Trade>>("BullBuyTrades")
        val BULL_SELL_TRADES = Port<Output.Container<Trade>>("BullSellTrades")
        val BEAR_BUY_TRADES = Port<Output.Container<Trade>>("BearBuyTrades")
        val BEAR_SELL_TRADES = Port<Output.Container<Trade>>("BearSellTrades")

        val POSITION = Port<Position>("Position")
    }

    private var cash: Double = param.initialCash.toDouble()
    private var baseVolume = cash * param.leverage

    private val bullBuys = HashMap<Int, Order>()
    private val bullSells = HashMap<Int, Order>()
    private val bearSells = HashMap<Int, Order>()
    private val bearBuys = HashMap<Int, Order>()

    private var position: Position? = null
    private val closedPositions = LinkedList<Position>()

    override suspend fun ProcessingScope<ICandle>.process() {
        processTrades()
        (BollingerBands.key(param.bbOptions) dataOf BollingerBands.BB_VALUES)?.also { bb ->
            placeOrders(bb)
        }
        position?.also { set(POSITION, it) }
    }

    private fun ProcessingScope<ICandle>.placeOrders(bb: BbValues) {
        placeBearBuyOrders(bb)
        placeBearSellOrders(bb)

        placeBearBuyOrders(bb)
        placeBearSellOrders(bb)
    }

    private fun ProcessingScope<ICandle>.placeBullBuyOrders(bb: BbValues) {
        bullBuys.clear()
        val p = position?.nullWhen { it.direction != Direction.BUY }
        var lastPrice = p?.enterTrades?.maxOfOrNull { it.order.basePrice } ?: max(input.high, bb.high)
        ((p?.enterTrades?.size ?: 0)..param.dcaNumMax step 1).forEach { slotId ->
            lastPrice = param.nextBearPrice(slotId, lastPrice)
            bullBuys[slotId] = Order(input.x, slotId, lastPrice, param.nextVolume(slotId), Direction.BUY)
        }
        set(BULL_BUY_ORDERS, Output.Container(bullBuys.values.toList()))
    }

    private fun ProcessingScope<ICandle>.placeBullSellOrders(bb: BbValues) {
        bullSells.clear()
        if (position?.direction == Direction.BUY) {
            bullSells[0] = Order(input.x, 0, bb.low, position!!.exitBaseVolume(bb.low), Direction.SELL)
        }
        set(BULL_SELL_ORDERS, Output.Container(bullSells.values.toList()))
    }

    private fun ProcessingScope<ICandle>.placeBearSellOrders(bb: BbValues) {
        bearSells.clear()
        val p = position?.nullWhen { it.direction != Direction.BUY }
        var lastPrice = p?.enterTrades?.minOfOrNull { it.order.basePrice } ?: min(input.low, bb.low)
        ((p?.enterTrades?.size ?: 0)..param.dcaNumMax step 1).forEach { slotId ->
            lastPrice = param.nextBullPrice(slotId, lastPrice)
            bearSells[slotId] = Order(input.x, slotId, lastPrice, param.nextVolume(slotId), Direction.SELL)
        }
        set(BEAR_SELL_ORDERS, Output.Container(bearSells.values.toList()))
    }

    private fun ProcessingScope<ICandle>.placeBearBuyOrders(bb: BbValues) {
        bearBuys.clear()
        if (position?.direction == Direction.BUY) {
            bearBuys[0] = Order(input.x, 0, bb.high, position!!.exitBaseVolume(bb.high), Direction.BUY)
        }
        set(BEAR_BUY_ORDERS, Output.Container(bearBuys.values.toList()))
    }

    private fun ProcessingScope<ICandle>.processTrades() {
        when (position?.direction) {
            null,
            Direction.BUY -> {
                processBullTrades()
                processBearTrades()
            }
            Direction.SELL -> {
                processBearTrades()
                processBullTrades()
            }
        }
    }

    private fun ProcessingScope<ICandle>.processBullTrades() {
        set(BULL_SELL_TRADES, Output.Container(bullSells.mapNotNull { (_, order) ->
            if (order.basePrice <= input.high) {
                val trade = Trade(input.x, order)
                position = position?.run { copy(idx = input.x, exitTrades = exitTrades + trade) }
                trade
            } else null
        }))
        processPosition()
        set(BULL_BUY_TRADES, Output.Container(bullBuys.mapNotNull { (_, order) ->
            if (order.basePrice >= input.low) {
                val trade = Trade(input.x, order)
                position = position?.run { copy(idx = input.x, enterTrades = enterTrades + trade) }
                trade
            } else null
        }))
    }

    private fun ProcessingScope<ICandle>.processBearTrades() {
        set(BEAR_BUY_TRADES, Output.Container(bearBuys.mapNotNull { (_, order) ->
            if (order.basePrice >= input.low) {
                val trade = Trade(input.x, order)
                position = position?.run { copy(idx = input.x, exitTrades = exitTrades + trade) }
                trade
            } else null
        }))
        processPosition()
        set(BEAR_SELL_TRADES, Output.Container(bearSells.mapNotNull { (_, order) ->
            if (order.basePrice <= input.high) {
                val trade = Trade(input.x, order)
                position = position?.run { copy(idx = input.x, enterTrades = enterTrades + trade) }
                trade
            } else null
        }))
    }

    private fun ProcessingScope<ICandle>.processPosition() {
        when {
            position?.run { counterExitVolume >= counterEnterVolume } == true -> {
                closedPositions += position!!
                position = null
            }
        }
    }
}

enum class Direction { BUY, SELL }

data class Order(
    val idx: Number,
    val slotId: Int,
    val basePrice: Double,
    val counterVolume: Long,
    val direction: Direction
) : Output.Scalar(idx, basePrice)

data class Trade(
    val idx: Number,
    val order: Order
) : Output.Scalar(idx, order.basePrice)

data class Position(
    val idx: Number,
    val id: Int,
    val enterTrades: List<Trade> = emptyList(),
    val exitTrades: List<Trade> = emptyList()
) : Output.Scalar(idx, id) {
    val direction: Direction? = enterTrades.firstOrNull()?.order?.direction

    val counterEnterVolume: Long = enterTrades.nullWhenEmpty()?.sumOf { it.order.counterVolume } ?: 0L
    val counterExitVolume: Long = exitTrades.nullWhenEmpty()?.sumOf { it.order.counterVolume } ?: 0L
    val counterDiff: Long = counterEnterVolume - counterExitVolume
    val closed: Boolean = counterDiff <= 0L

    val baseEnterVolume: Double =
        enterTrades.nullWhenEmpty()?.sumOf { it.order.counterVolume / it.order.basePrice } ?: 0.0
    val baseExitVolume: Double =
        exitTrades.nullWhenEmpty()?.sumOf { it.order.counterVolume / it.order.basePrice } ?: 0.0
    val baseDiff: Double = baseEnterVolume - baseExitVolume

    fun exitBaseVolume(price: Double): Long = ((counterEnterVolume - counterExitVolume) / price).toLong()
}

operator fun Position.plus(trade: Trade): Position = copy(enterTrades = enterTrades + trade)
operator fun Position.minus(trade: Trade): Position = copy(exitTrades = exitTrades + trade)