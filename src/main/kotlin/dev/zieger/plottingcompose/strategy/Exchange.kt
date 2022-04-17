package dev.zieger.plottingcompose.strategy

import dev.zieger.plottingcompose.indicators.candles.ICandle
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.strategy.dto.Order
import dev.zieger.plottingcompose.strategy.dto.Position
import dev.zieger.plottingcompose.strategy.dto.RemoteOrder

interface Exchange<I : ICandle> {

    val baseTickSize: Double
        get() = 0.00000001

    fun counterTickSize(currentPrice: Double): Double =
        baseTickSize * currentPrice

    val cash: Double
    val walletBalance: Double
    val closedPositions: List<Position>
    val closedPosition: Position?
    val position: Position?
    val availableBalance: Double
    val equity: Double
    val usedMargin: Double
    val orderMargin: Double
    val positionMargin: Double

    suspend fun <O : Order<O>> order(order: O): RemoteOrder<O>
    suspend fun changeOrder(order: Order<*>): Boolean
    suspend fun cancelOrder(order: Order<*>): Boolean
    suspend fun processCandle(scope: ProcessingScope<I>)
}