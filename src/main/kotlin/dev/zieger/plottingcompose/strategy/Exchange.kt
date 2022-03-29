package dev.zieger.plottingcompose.strategy

import dev.zieger.plottingcompose.indicators.candles.ICandle
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.strategy.dto.Order
import dev.zieger.plottingcompose.strategy.dto.Position
import dev.zieger.plottingcompose.strategy.dto.RemoteOrder
import dev.zieger.plottingcompose.strategy.dto.RemoteOrderListener

interface Exchange<I : ICandle> {

    val cash: Double
    val walletBalance: Double
    val closedPositions: List<Position>
    val position: Position?
    val availableBalance: Double
    val equity: Double
    val usedMargin: Double
    val orderMargin: Double
    val positionMargin: Double

    fun addPositionListener(onPositionChanged: (Position) -> Unit): () -> Unit

    fun order(order: Order, listener: RemoteOrderListener? = null): RemoteOrder
    suspend fun changeOrder(order: Order): Boolean
    suspend fun cancelOrder(order: Order): Boolean
    fun process(scope: ProcessingScope<I>)
}