package dev.zieger.plottingcompose.strategy

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.candles.ICandle
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.scopes.nullWhenEmpty
import dev.zieger.plottingcompose.strategy.dto.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class StrategyParameter(
    val initialCash: Double = 10_000.0,
    val leverage: Double = 10.0,
    val maxOrdersPerSide: Int = 20
)

abstract class Strategy<I : ICandle>(
    param: StrategyParameter,
    private val internalExchange: Exchange<I>,
    key: Key<I, *>,
    produces: List<Port<*>> = emptyList(),
    vararg dependsOn: Key<I, *>
) : Indicator<I>(key, produces + ports(param), *dependsOn),
    Exchange<I> by internalExchange {

    companion object {

        fun ports(param: StrategyParameter) = listOf(
            *(0..param.maxOrdersPerSide).map { BULL_BUY_ORDERS(it) }.toTypedArray(),
            BULL_SELL_ORDERS(0),
            *(0..param.maxOrdersPerSide).map { BEAR_SELL_ORDERS(it) }.toTypedArray(),
            BEAR_BUY_ORDERS(0),
            *(0..param.maxOrdersPerSide).map { BULL_SELL_TRADES(it) }.toTypedArray(),
            BULL_BUY_TRADES(0),
            *(0..param.maxOrdersPerSide).map { BEAR_SELL_TRADES(it) }.toTypedArray(),
            BEAR_BUY_TRADES(0),
            POSITION, POSITION_AVERAGE_PRICE, POSITION_DIFF
        )

        fun BULL_BUY_ORDERS(idx: Int) = Port<Order<BullBuy>>("BullBuyOrders$idx", false)
        fun BULL_SELL_ORDERS(idx: Int) = Port<Order<BullSell>>("BullSellOrders$idx", false)
        fun BEAR_BUY_ORDERS(idx: Int) = Port<Order<BearBuy>>("BearBuyOrders$idx", false)
        fun BEAR_SELL_ORDERS(idx: Int) = Port<Order<BearSell>>("BearSellOrders$idx", false)

        fun BULL_BUY_TRADES(idx: Int) = Port<Trade>("BullBuyTrades$idx", false)
        fun BULL_SELL_TRADES(idx: Int) = Port<Trade>("BullSellTrades$idx", false)
        fun BEAR_BUY_TRADES(idx: Int) = Port<Trade>("BearBuyTrades$idx", false)
        fun BEAR_SELL_TRADES(idx: Int) = Port<Trade>("BearSellTrades$idx", false)

        val POSITION = Port<Position>("Position", false)
        val POSITION_AVERAGE_PRICE = Port<Output.Scalar>("PositionAveragePrice")
        val POSITION_DIFF = Port<Output.Scalar>("PositionDiff", false)
    }

    private lateinit var currentInput: I

    private val orderMutex = Mutex()

    private val bullBuys = HashMap<Int, Order<BullBuy>?>(param.maxOrdersPerSide)
    private val bullSells = HashMap<Int, Order<BullSell>?>(param.maxOrdersPerSide)
    private val bearBuys = HashMap<Int, Order<BearBuy>?>(param.maxOrdersPerSide)
    private val bearSells = HashMap<Int, Order<BearSell>?>(param.maxOrdersPerSide)

    private var prevClosed: Position? = null

    abstract suspend fun ProcessingScope<I>.placeOrders()

    override suspend fun ProcessingScope<I>.process() {
        currentInput = input
        placeOrders()
        internalExchange.processCandle(this)

        orderMutex.withLock {
            bullBuys.values.filterNotNull().nullWhenEmpty()?.sortedByDescending { it.counterPrice }
                ?.forEach { order -> set(BULL_BUY_ORDERS(order.slot), order) }
            bullSells.values.filterNotNull().nullWhenEmpty()?.sortedBy { it.counterPrice }
                ?.forEach { order -> set(BULL_SELL_ORDERS(order.slot), order) }
            bearBuys.values.filterNotNull().nullWhenEmpty()?.sortedBy { it.counterPrice }
                ?.forEach { order -> set(BEAR_BUY_ORDERS(order.slot), order) }
            bearSells.values.filterNotNull().nullWhenEmpty()?.sortedByDescending { it.counterPrice }
                ?.forEach { order -> set(BEAR_SELL_ORDERS(order.slot), order) }
            closedPositions.lastOrNull()?.takeIf { it.isClosed && it != prevClosed }?.also { closed ->
                prevClosed = closed
                set(
                    POSITION_DIFF, Output.Scalar(
                        input.x, closed.counterDiff * when (closed.direction) {
                            Direction.BUY -> -1
                            Direction.SELL -> 1
                            else -> throw IllegalStateException("Direction of closed position can not be null")
                        }
                    )
                )
            }
            position?.also {
                set(POSITION, it)
                set(POSITION_AVERAGE_PRICE, Output.Scalar(input.x, it.averageCounterPrice))
            }
        }
    }

    override suspend fun <O : Order<O>> order(order: O, listener: RemoteOrderListener<O>?): RemoteOrder<O> =
        internalExchange.order(order, listener).apply {
            onPlaced {
                orderMutex.withLock {
                    when (val o = it.order) {
                        is BullBuy -> bullBuys[o.slot] = o
                        is BullSell -> bullSells[o.slot] = o
                        is BearBuy -> bearBuys[o.slot] = o
                        is BearSell -> bearSells[o.slot] = o
                    }
                }

                false
            }
            onExecuted {
                orderMutex.withLock {
                    when (val o = it.order) {
                        is BullBuy -> bullBuys[o.slot] = null
                        is BullSell -> bullSells[o.slot] = null
                        is BearBuy -> bearBuys[o.slot] = null
                        is BearSell -> bearSells[o.slot] = null
                    }
                }
                true
            }
            onChanged { order ->
                orderMutex.withLock {
                    when (val o = order.order) {
                        is BullBuy -> bullBuys[o.slot] = o
                        is BullSell -> bullSells[o.slot] = o
                        is BearBuy -> bearBuys[o.slot] = o
                        is BearSell -> bearSells[o.slot] = o
                        else -> Unit
                    }
                }
                println("order #${order.order.slot} ${order.order.type} changed to ${order.order.counterPrice}")
                false
            }
            onCancelled {
                orderMutex.withLock {
                    when (val o = it.order) {
                        is BullBuy -> bullBuys[o.slot] = null
                        is BullSell -> bullSells[o.slot] = null
                        is BearBuy -> bearBuys[o.slot] = null
                        is BearSell -> bearSells[o.slot] = null
                        else -> Unit
                    }
                }

                true
            }
        }
}