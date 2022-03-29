package dev.zieger.plottingcompose.strategy.dto

import dev.zieger.plottingcompose.strategy.Exchange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RemoteOrder(
    private val scope: CoroutineScope,
    val order: Order,
    internal val exchange: Exchange<*>,
    val listener: MutableList<RemoteOrderListener>
) {

    constructor(scope: CoroutineScope, order: Order, exchange: Exchange<*>, listener: RemoteOrderListener?) :
            this(scope, order, exchange, listener?.let { mutableListOf(listener) } ?: mutableListOf())

    var isPlaced = false
        private set
    var isExecuted = false
        private set
    var isCancelled = false
        private set

    internal fun placed() {
        isPlaced = true
        listener.forEach { it.onOrderPlaced(this) }
    }

    internal fun executed(trade: Trade) {
        isPlaced = false
        isExecuted = true
        listener.forEach { it.onOrderExecuted(trade) }
    }

    internal fun cancelled() {
        isPlaced = false
        isCancelled = true
        listener.forEach { it.onOrderCancelled(order) }
    }

    fun change(counterPrice: Double = order.counterPrice, counterVolume: Double = order.counterVolume) {
        order.counterPrice = counterPrice
        order.counterVolume = counterVolume
        scope.launch {
            exchange.changeOrder(order)
            listener.forEach { it.onOrderChanged(this@RemoteOrder) }
        }
    }

    fun cancel() {
        scope.launch {
            exchange.cancelOrder(order)
            cancelled()
            listener.clear()
        }
    }
}

interface RemoteOrderListener {
    fun onOrderPlaced(order: RemoteOrder)
    fun onOrderExecuted(order: Trade)
    fun onOrderCancelled(order: Order)
    fun onOrderChanged(order: RemoteOrder)
}
