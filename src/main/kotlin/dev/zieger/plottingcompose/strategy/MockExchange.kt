package dev.zieger.plottingcompose.strategy

import dev.zieger.plottingcompose.indicators.candles.IndicatorCandle
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.strategy.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.absoluteValue
import kotlin.properties.Delegates

data class MockExchangeParameter(
    val initialCash: Double = 1_000.0, val leverage: Double = 10.0
)

class MockExchange(
    private val param: MockExchangeParameter = MockExchangeParameter()
) : Exchange<IndicatorCandle> {

    private val scope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    private lateinit var currentInput: IndicatorCandle

    override val cash: Double
        get() = param.initialCash +
                closedPositions.sumOf { it.counterDiff }

    override val walletBalance: Double
        get() = cash * param.leverage

    override val availableBalance: Double
        get() = walletBalance - usedMargin // minus position closing fee

    override val equity: Double
        get() = availableBalance - (position?.exitCounterVolumeDiff(currentInput.close) ?: 0.0)

    override val usedMargin: Double
        get() = orderMargin + positionMargin

    override val orderMargin: Double
        get() = ArrayList(remoteOrders)
            .filterNotNull()
            .filter { it.isPlaced && !it.isExecuted }
            .sumOf { it.order.counterVolume }

    override val positionMargin: Double
        get() = position?.counterMargin(currentInput.close, param.leverage) ?: 0.0

    override val closedPositions = LinkedList<Position>()
    override var closedPosition by Delegates.observable<Position?>(null) { _, _, pos ->
        pos?.let { closedPositions += pos }
    }
        private set

    override var position: Position? = null
        private set

    private val remoteOrdersMutex = Mutex()
    private val remoteOrders = LinkedList<RemoteOrder<*>>()

    override fun toString(): String = stringBlock {
        starLine()
        -"Exchange"
        +"Cash: %d$".format(cash.toInt())
        +"Wallet Balance: %d$".format(walletBalance.toInt())
        +"Available Balance: %d$".format(availableBalance.toInt())
        +"Order Margin: %d$".format(orderMargin.toInt())
        +"Closed Position Diff: %d$".format(closedPositions.sumOf { it.counterDiff }.toInt())
        starLine()
    }

    override suspend fun <O : Order<O>> order(order: O): RemoteOrder<O> =
        RemoteOrder(scope, order, this).also {
            if (it.order.counterVolume > availableBalance) {
                it.cancelled()
                return@also
            }

            remoteOrdersMutex.withLock {
                remoteOrders += it
            }
            it.placed()
            it.onExecuted {
                println(this@MockExchange)
                println(position)
                true
            }
        }

    override suspend fun changeOrder(order: Order<*>): Boolean = remoteOrdersMutex.withLock {
        remoteOrders.any { it.order == order }
    }

    override suspend fun cancelOrder(order: Order<*>): Boolean = remoteOrdersMutex.withLock {
        remoteOrders.removeIf { it.order == order }
    }

    override suspend fun processCandle(scope: ProcessingScope<IndicatorCandle>): Unit = scope.run {
        currentInput = input

        remoteOrdersMutex.withLock {
            fun List<RemoteOrder<*>>.sortNearestToClose() =
                sortedBy { (input.close - it.order.counterPrice).absoluteValue }

            val toRemove = remoteOrders
                .groupBy { it.order.direction == position?.direction }
                .flatMap { (_, ro) ->
                    ro.sortNearestToClose().processOrders(input)
                }.toSet()

            remoteOrders.removeAll(toRemove)
        }
    }

    private suspend fun List<RemoteOrder<*>>.processOrders(candle: IndicatorCandle): Set<RemoteOrder<*>> =
        mapNotNull { ro ->
            ro.takeIf {
                ro.order.run {
                    position = when (position?.direction) {
                        Direction.BUY -> when (direction) {
                            Direction.BUY -> {
                                if (counterPrice >= candle.low) {
                                    val trade = Trade(candle.x, this)
                                    ro.executed(trade)
                                    position!!.copy(enterTrades = position!!.enterTrades + trade)
                                        .also { println("Bull buy $trade; pos closed=${it.isClosed}") }
                            } else null
                        }
                        Direction.SELL -> {
                            if (counterPrice <= candle.high) {
                                val trade = Trade(candle.x, this)
                                ro.executed(trade)
                                position!!.copy(exitTrades = position!!.exitTrades + trade)
                                    .also { println("Bull sell $trade; pos closed=${it.isClosed}") }
                            } else null
                        }
                    } ?: position
                    Direction.SELL -> when (direction) {
                        Direction.SELL -> {
                            if (counterPrice <= candle.high) {
                                val trade = Trade(candle.x, this)
                                ro.executed(trade)
                                position!!.copy(enterTrades = position!!.enterTrades + trade)
                                    .also { println("Bear sell $trade; pos closed=${it.isClosed}") }
                            } else null
                        }
                        Direction.BUY -> {
                            if (counterPrice >= candle.low) {
                                val trade = Trade(candle.x, this)
                                ro.executed(trade)
                                position!!.copy(exitTrades = position!!.exitTrades + trade)
                                    .also { println("Bear buy $trade; pos closed=${it.isClosed}") }
                            } else null
                        }
                    } ?: position
                    else -> when (direction) {
                        Direction.BUY -> {
                            if (counterPrice >= candle.low) {
                                val trade = Trade(candle.x, this)
                                ro.executed(trade)
                                Position(
                                    candle.x,
                                    closedPositions.size,
                                    listOf(trade)
                                ).also { println("Bull buy $trade; pos closed=${it.isClosed}") }
                            } else null
                        }
                        Direction.SELL -> {
                            if (counterPrice <= candle.high) {
                                val trade = Trade(candle.x, this)
                                ro.executed(trade)
                                Position(
                                    candle.x,
                                    closedPositions.size,
                                    listOf(trade)
                                ).also { println("Bear sell $trade; pos closed=${it.isClosed}") }
                            } else null
                        }
                    }
                }

                if (position?.isClosed == true) {
                    closedPosition = position!!.copyDeep()
                    println("position closed\n$closedPosition")
                    position = null
                }

                ro.isExecuted
            }
        }
    }.toSet()
}