package dev.zieger.plottingcompose.strategy

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.candles.ICandle
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.strategy.dto.*

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
            POSITION
        )

        fun BULL_BUY_ORDERS(idx: Int) = Port<Order>("BullBuyOrders$idx", false)
        fun BULL_SELL_ORDERS(idx: Int) = Port<Order>("BullSellOrders$idx", false)
        fun BEAR_BUY_ORDERS(idx: Int) = Port<Order>("BearBuyOrders$idx", false)
        fun BEAR_SELL_ORDERS(idx: Int) = Port<Order>("BearSellOrders$idx", false)

        fun BULL_BUY_TRADES(idx: Int) = Port<Trade>("BullBuyTrades$idx", false)
        fun BULL_SELL_TRADES(idx: Int) = Port<Trade>("BullSellTrades$idx", false)
        fun BEAR_BUY_TRADES(idx: Int) = Port<Trade>("BearBuyTrades$idx", false)
        fun BEAR_SELL_TRADES(idx: Int) = Port<Trade>("BearSellTrades$idx", false)

        val POSITION = Port<Position>("Position", false)
    }

    private lateinit var currentInput: I

    private val bullBuys = HashMap<Int, Order>()
    private val bullSells = HashMap<Int, Order>()
    private val bearBuys = HashMap<Int, Order>()
    private val bearSells = HashMap<Int, Order>()

    protected fun processUnreferenced(scope: ProcessingScope<I>) = scope.run {
        currentInput = input
        internalExchange.process(scope)

        bullBuys.forEach { (idx, order) -> set(BULL_BUY_ORDERS(idx), order) }
        bullBuys.clear()
        bullSells.forEach { (idx, order) -> set(BULL_SELL_ORDERS(idx), order) }
        bullSells.clear()
        bearBuys.forEach { (idx, order) -> set(BEAR_BUY_ORDERS(idx), order) }
        bearBuys.clear()
        bearSells.forEach { (idx, order) -> set(BEAR_SELL_ORDERS(idx), order) }
        bearSells.clear()

//        position?.also { set(POSITION, it) }
    }

    override fun order(order: Order, listener: RemoteOrderListener?): RemoteOrder =
        internalExchange.order(order, listener).also {
            when (order.direction) {
                Direction.SELL -> when (order.counterPrice < currentInput.close) {
                    true -> bearSells[bearBuys.size] = order
                    false -> bullSells[bullSells.size] = order
                }
                Direction.BUY -> when (order.counterPrice > currentInput.close) {
                    true -> bearBuys[bearBuys.size] = order
                    false -> bullBuys[bullSells.size] = order
                }
            }

//            it.listener.add(object : RemoteOrderListener {
//                override fun onOrderPlaced(order: RemoteOrder) {
//                    TODO("Not yet implemented")
//                }
//
//                override fun onOrderExecuted(order: Trade) {
//                    TODO("Not yet implemented")
//                }
//
//                override fun onOrderCancelled(order: Order) {
//                    TODO("Not yet implemented")
//                }
//
//                override fun onOrderChanged(order: RemoteOrder) {
//                    TODO("Not yet implemented")
//                }
//            })
        }
}