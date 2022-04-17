package dev.zieger.plottingcompose.strategy

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.candles.ICandle
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.strategy.dto.*
import dev.zieger.utils.misc.format
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

data class StrategyParameter(
    val initialCash: Long = 10_000L,
    val leverage: Double = 100.0,
    val maxOrdersPerSide: Int = 20
)

abstract class Strategy<I : ICandle>(
    param: StrategyParameter,
    private val internalExchange: Exchange<I>,
    key: Key<I, *>,
    produces: List<Port<*>> = emptyList(),
    vararg dependsOn: Key<I, *>
) : Indicator<I>(key, produces + STRATEGY_RESULTS, *dependsOn),
    Exchange<I> by internalExchange {

    companion object {

        data class StrategyOrders(
            val bullBuys: Map<Int, Order<BullBuy>?>,
            val bullSells: Map<Int, Order<BullSell>?>,
            val bearBuys: Map<Int, Order<BearBuy>?>,
            val bearSells: Map<Int, Order<BearSell>?>,
        )

        class StrategyResults(
            val input: ICandle,
            val orders: StrategyOrders,
            val trades: List<Trade>,
            val position: Position?,
            val closedPosition: Position?,
            val cash: Double,
            val equity: Double,
            val availableBalance: Double
        ) : Output.Scalar(input.x, cash) {

            override fun toString(): String =
                "${
                    position?.let {
                        "Active: ${it.counterDiff.format(2)} - " +
                                it.averageEnterCounterPrice.format(2)
                    } ?: ""
                }${closedPosition?.let { "\nClosed: ${it.counterDiff.format(2)}$" } ?: ""}"
        }

        val STRATEGY_RESULTS = Port<StrategyResults>("StrategyResult", false)
    }

    private lateinit var currentInput: I

    private val resultMutex = Mutex()

    protected val bullBuys = HashMap<Int, RemoteOrder<BullBuy>?>(param.maxOrdersPerSide)
    protected val bullSells = HashMap<Int, RemoteOrder<BullSell>?>(param.maxOrdersPerSide)
    protected val bearBuys = HashMap<Int, RemoteOrder<BearBuy>?>(param.maxOrdersPerSide)
    protected val bearSells = HashMap<Int, RemoteOrder<BearSell>?>(param.maxOrdersPerSide)

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Order<T>> setOrder(order: RemoteOrder<T>) = resultMutex.withLock {
        when (order.order::class) {
            BullBuy::class -> bullBuys[order.order.slot] = order as RemoteOrder<BullBuy>
            BullSell::class -> bullSells[order.order.slot] = order as RemoteOrder<BullSell>
            BearBuy::class -> bearBuys[order.order.slot] = order as RemoteOrder<BearBuy>
            BearSell::class -> bearSells[order.order.slot] = order as RemoteOrder<BearSell>
            else -> throw IllegalArgumentException("Invalid order type ${order.order::class}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Order<T>> clearOrder(order: Order<T>) = resultMutex.withLock {
        when (order::class) {
            BullBuy::class -> bullBuys[order.slot] = null
            BullSell::class -> bullSells[order.slot] = null
            BearBuy::class -> bearBuys[order.slot] = null
            BearSell::class -> bearSells[order.slot] = null
            else -> throw IllegalArgumentException("Invalid order type ${order::class}")
        }
    }

    private val trades = LinkedList<Trade>()
    private var sendClosedPosition: Int? = null

    abstract suspend fun ProcessingScope<I>.placeOrders()

    override suspend fun ProcessingScope<I>.process() {
        currentInput = input
        placeOrders()

        val orders = resultMutex.withLock {
            trades.clear()
            StrategyOrders(
                HashMap(bullBuys.map { (k, v) -> k to v?.order?.change() }.toMap()),
                HashMap(bullSells.map { (k, v) -> k to v?.order?.change() }.toMap()),
                HashMap(bearBuys.map { (k, v) -> k to v?.order?.change() }.toMap()),
                HashMap(bearSells.map { (k, v) -> k to v?.order?.change() }.toMap())
            )
        }

        internalExchange.processCandle(this)

        resultMutex.withLock {
            set(
                STRATEGY_RESULTS, StrategyResults(
                    input,
                    orders,
                    ArrayList(trades),
                    position?.copy(),
                    closedPosition
                        ?.takeIf { it.exitTrades.maxOf { m -> m.x.toDouble() } == input.x.toDouble() },
                    cash, equity, availableBalance
                )
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <O : Order<O>> order(order: O): RemoteOrder<O> =
        internalExchange.order(order).apply {
            if (isCancelled) return@apply

            setOrder(this)

            onExecuted {
                clearOrder(it.order)
                with(resultMutex) {
                    trades += it
                }
                true
            }
            onCancelled {
                clearOrder(it.order)
                true
            }
        }
}