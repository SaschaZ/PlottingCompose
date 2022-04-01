package dev.zieger.plottingcompose.strategy.dto

import dev.zieger.plottingcompose.definition.Output

enum class OrderType { BULL_BUY, BULL_SELL, BEAR_BUY, BEAR_SELL }

class BullBuy(slot: Int, pair: Pair, t: Number, counterPrice: Double, counterVolume: Double, direction: Direction) :
    Order<BullBuy>(slot, pair, t, counterPrice, counterVolume, direction, OrderType.BULL_BUY) {
    override fun change(counterPrice: Double, counterVolume: Double): BullBuy =
        BullBuy(slot, pair, t, counterPrice, counterVolume, direction)
}

class BullSell(slot: Int, pair: Pair, t: Number, counterPrice: Double, counterVolume: Double, direction: Direction) :
    Order<BullSell>(slot, pair, t, counterPrice, counterVolume, direction, OrderType.BULL_SELL) {
    override fun change(counterPrice: Double, counterVolume: Double): BullSell =
        BullSell(slot, pair, t, counterPrice, counterVolume, direction)
}

class BearBuy(slot: Int, pair: Pair, t: Number, counterPrice: Double, counterVolume: Double, direction: Direction) :
    Order<BearBuy>(slot, pair, t, counterPrice, counterVolume, direction, OrderType.BEAR_BUY) {
    override fun change(counterPrice: Double, counterVolume: Double): BearBuy =
        BearBuy(slot, pair, t, counterPrice, counterVolume, direction)
}

class BearSell(slot: Int, pair: Pair, t: Number, counterPrice: Double, counterVolume: Double, direction: Direction) :
    Order<BearSell>(slot, pair, t, counterPrice, counterVolume, direction, OrderType.BEAR_SELL) {
    override fun change(counterPrice: Double, counterVolume: Double): BearSell =
        BearSell(slot, pair, t, counterPrice, counterVolume, direction)
}


sealed class Order<O : Order<O>>(
    val slot: Int,
    val pair: Pair,
    val t: Number,
    val counterPrice: Double,
    val counterVolume: Double,
    val direction: Direction,
    val type: OrderType
) : Output.Scalar(t, counterPrice) {
    val baseVolume: Double get() = counterVolume / counterPrice

    abstract fun change(counterPrice: Double = this.counterPrice, counterVolume: Double = this.counterVolume): O

    override fun toString(): String = "Order(slot=$slot; pair=$pair; t=$t; counterPrice=$counterPrice; " +
            "counterVolume=$counterVolume; direction=$direction)"
}