package dev.zieger.plottingcompose.strategy

import dev.zieger.plottingcompose.indicators.candles.ICandle
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.strategy.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.absoluteValue

data class MockExchangeParameter(
    val initialCash: Double = 10_000.0,
    val leverage: Double = 10.0
)

class MockExchange(
    private val param: MockExchangeParameter = MockExchangeParameter()
) : Exchange<ICandle> {

    private val scope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    private lateinit var currentInput: ICandle

    override var cash: Double = param.initialCash
        private set

    override val walletBalance: Double
        get() = cash * param.leverage

    override val availableBalance: Double
        get() = cash * walletBalance - usedMargin // minus position closing fee

    override val equity: Double
        get() = availableBalance - (position?.exitCounterVolumeDiff(currentInput.close) ?: 0.0)

    override val usedMargin: Double
        get() = orderMargin + positionMargin
    override val orderMargin: Double
        get() = remoteOrders.filter { it.isPlaced && !it.isExecuted }
            .sumOf { it.order.counterVolume }

    override val positionMargin: Double
        get() = position?.counterMargin(currentInput.close, param.leverage) ?: 0.0

    override val closedPositions = LinkedList<Position>()
    override var position: Position? = null
        private set

    private val positionListener = LinkedList<(Position) -> Unit>()
    private val remoteOrders = LinkedList<RemoteOrder>()

    override fun addPositionListener(onPositionChanged: (Position) -> Unit): () -> Unit {
        positionListener += onPositionChanged
        return { positionListener -= onPositionChanged }
    }

    override fun order(order: Order, listener: RemoteOrderListener?): RemoteOrder =
        RemoteOrder(scope, order, this, listener).also {
            if (it.order.counterVolume > availableBalance) {
                it.cancelled()
                return@also
            }

            remoteOrders += it
            println("ro: ${remoteOrders.size}")
            cash -= order.counterVolume / param.leverage
            it.placed()
        }

    override suspend fun changeOrder(order: Order): Boolean = remoteOrders.any { it.order == order }
    override suspend fun cancelOrder(order: Order): Boolean = remoteOrders.removeIf { it.order == order }

    private var cnt = 0

    override fun process(scope: ProcessingScope<ICandle>) = scope.run {
        currentInput = input
        println(++cnt)

        remoteOrders.sortedBy { (input.close - it.order.counterPrice).absoluteValue }.forEach { ro ->
            ro.order.run {
                position = when (position?.direction) {
                    Direction.BUY -> when (direction) {
                        Direction.BUY -> {
                            if (counterPrice > input.low)
                                position!!.copy(enterTrades = position!!.enterTrades + Trade(input.x, this))
                                    .also { println("Bull buy") }
                            else null
                        }
                        Direction.SELL -> {
                            if (counterPrice < input.high)
                                position!!.copy(exitTrades = position!!.exitTrades + Trade(input.x, this))
                                    .also { println("Bull sell") }
                            else null
                        }
                    } ?: position
                    Direction.SELL -> when (direction) {
                        Direction.SELL -> {
                            if (counterPrice < input.high)
                                position!!.copy(enterTrades = position!!.enterTrades + Trade(input.x, this))
                                    .also { println("Bear sell") }
                            else null
                        }
                        Direction.BUY -> {
                            if (counterPrice > input.low)
                                position!!.copy(exitTrades = position!!.exitTrades + Trade(input.x, this))
                                    .also { println("Bear buy") }
                            else null
                        }
                    } ?: position
                    else -> when (direction) {
                        Direction.BUY -> {
                            if (counterPrice < input.high)
                                Position(input.x, closedPositions.size, listOf(Trade(input.x, this)))
                                    .also { println("Bull buy") }
                            else null
                        }
                        Direction.SELL -> {
                            if (counterPrice > input.low)
                                Position(input.x, closedPositions.size, listOf(Trade(input.x, this)))
                                    .also { println("Bear sell") }
                            else null
                        }
                    }
                }

                if (position?.isClosed == true) {
                    closedPositions += position!!
                    cash += position!!.exitCounterVolumeDiff(counterPrice)
                    println("position closed $position")
                    position = null
                }
            }
        }
    }
}