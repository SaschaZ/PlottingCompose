package dev.zieger.plottingcompose.strategy.dto

import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.scopes.nullWhenEmpty

data class Position(
    val t: Number,
    val id: Int,
    val enterTrades: List<Trade> = emptyList(),
    val exitTrades: List<Trade> = emptyList()
) : Output.Scalar(t, id) {
    val direction: Direction? = enterTrades.firstOrNull()?.order?.direction

    val counterEnterVolume: Double = enterTrades.nullWhenEmpty()?.sumOf { it.order.counterVolume } ?: 0.0
    val counterExitVolume: Double = exitTrades.nullWhenEmpty()?.sumOf { it.order.counterVolume } ?: 0.0
    val counterDiff: Double = counterEnterVolume - counterExitVolume
    val baseEnterVolume: Double = enterTrades.nullWhenEmpty()?.sumOf { it.order.baseVolume } ?: 0.0
    val baseExitVolume: Double = exitTrades.nullWhenEmpty()?.sumOf { it.order.baseVolume } ?: 0.0
    val baseDiff: Double = baseEnterVolume - baseExitVolume
    val isClosed: Boolean = baseDiff < 0.0000000009

    val averageCounterPrice: Double
        get() = enterTrades.nullWhenEmpty()
            ?.run { sumOf { it.order.counterVolume } / sumOf { it.order.baseVolume } } ?: -1.0

    fun exitBaseVolumeDiff(counterPrice: Double): Double = counterDiff / counterPrice
    fun exitCounterVolumeDiff(counterPrice: Double): Double = baseDiff * counterPrice

    fun baseMargin(counterPrice: Double, leverage: Double): Double =
        (exitBaseVolumeDiff(counterPrice) * (leverage - 1) / leverage).coerceAtMost(0.0)

    fun counterMargin(counterPrice: Double, leverage: Double): Double =
        (exitCounterVolumeDiff(counterPrice) * (leverage - 1) / leverage).coerceAtMost(0.0)

    override fun toString(): String =
        "Position(enterTrades=$enterTrades; exitTrades=$exitTrades; counterDiff=$counterDiff, averageCounterPrice=$averageCounterPrice)"
}

operator fun Position.plus(trade: Trade): Position = copy(enterTrades = enterTrades + trade)
operator fun Position.minus(trade: Trade): Position = copy(exitTrades = exitTrades + trade)
