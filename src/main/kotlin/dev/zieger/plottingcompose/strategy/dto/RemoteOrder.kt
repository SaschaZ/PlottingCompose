@file:Suppress("unused")

package dev.zieger.plottingcompose.strategy.dto

import dev.zieger.plottingcompose.strategy.Exchange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

class RemoteOrder<O : Order<O>>(
    private val scope: CoroutineScope,
    order: O,
    internal val exchange: Exchange<*>,
    val listener: MutableList<RemoteOrderListener<O>>
) {

    constructor(scope: CoroutineScope, order: O, exchange: Exchange<*>, listener: RemoteOrderListener<O>?) :
            this(scope, order, exchange, listener?.let { mutableListOf(listener) } ?: mutableListOf())

    var order: O = order
        private set

    private var executedTrade: Trade? = null

    var isPlaced = false
        private set
    val isExecuted: Boolean
        get() = executedTrade != null
    var isCancelled = false
        private set

    private val onPlacedListener = LinkedList<suspend (RemoteOrder<O>) -> Boolean>()

    internal suspend fun placed() {
        isPlaced = true
        onPlacedListener.removeWhen { it(this) }
        onPlacedListener.clear()
        listener.forEach { it.onOrderPlaced(this) }
    }

    suspend fun onPlaced(onPlaced: suspend (RemoteOrder<O>) -> Boolean): RemoteOrder<O> {
        if (!isPlaced || !onPlaced(this))
            onPlacedListener += onPlaced

        return this
    }

    private val onExecutedListener = LinkedList<suspend (Trade) -> Boolean>()

    internal suspend fun executed(trade: Trade) {
        isPlaced = false
        executedTrade = trade
        onExecutedListener.removeWhen { it(trade) }
        onExecutedListener.clear()
        listener.forEach { it.onOrderExecuted(trade) }
    }

    suspend fun onExecuted(onExecuted: suspend (Trade) -> Boolean): RemoteOrder<O> {
        if (!isExecuted || !onExecuted(executedTrade!!))
            onExecutedListener += onExecuted

        return this
    }

    private val onCancelledListener = LinkedList<suspend (RemoteOrder<O>) -> Boolean>()

    internal suspend fun cancelled() {
        isPlaced = false
        isCancelled = true
        onCancelledListener.removeWhen { it(this) }
        onCancelledListener.clear()
        listener.forEach { it.onOrderCancelled(order) }
    }

    suspend fun onCancelled(onCancelled: suspend (RemoteOrder<O>) -> Boolean): RemoteOrder<O> {
        if (!isCancelled || !onCancelled(this))
            onCancelledListener += onCancelled

        return this
    }

    private val onChangedListener = LinkedList<suspend (RemoteOrder<O>) -> Boolean>()

    fun change(counterPrice: Double = order.counterPrice, counterVolume: Double = order.counterVolume) {
        if (counterPrice != order.counterPrice || counterVolume != order.counterVolume) {
            order = order.change(counterPrice, counterVolume)
            scope.launch {
                exchange.changeOrder(order)
                onChangedListener.removeWhen { it(this@RemoteOrder) }
                listener.forEach { it.onOrderChanged(this@RemoteOrder) }
            }
        }
    }

    fun onChanged(onChanged: suspend (RemoteOrder<O>) -> Boolean): RemoteOrder<O> {
        onChangedListener += onChanged

        return this
    }

    fun cancel() {
        scope.launch {
            exchange.cancelOrder(order)
            cancelled()
            listener.clear()
        }
    }
}

interface RemoteOrderListener<O : Order<O>> {
    fun onOrderPlaced(order: RemoteOrder<O>)
    fun onOrderExecuted(trade: Trade)
    fun onOrderCancelled(order: Order<*>)
    fun onOrderChanged(order: RemoteOrder<O>)
}

inline fun <T> MutableList<T>.removeWhen(selector: (T) -> Boolean) {
    val toRemove = LinkedList<T>()
    forEach { if (selector(it)) toRemove += it }
    removeAll(toRemove)
}