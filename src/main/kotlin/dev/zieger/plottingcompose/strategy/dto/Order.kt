package dev.zieger.plottingcompose.strategy.dto

import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.strategy.dto.Direction.BUY
import dev.zieger.plottingcompose.strategy.dto.Direction.SELL

enum class OrderType { BULL_BUY, BULL_SELL, BEAR_BUY, BEAR_SELL }

class BullBuy(slot: Int, pair: Pair, t: Number, counterPrice: Double, counterVolume: Double) :
    Order<BullBuy>(slot, pair, t, counterPrice, counterVolume, BUY, OrderType.BULL_BUY) {
    override fun change(counterPrice: Double, counterVolume: Double): BullBuy =
        BullBuy(slot, pair, t, counterPrice, counterVolume)
}

class BullSell(slot: Int, pair: Pair, t: Number, counterPrice: Double, counterVolume: Double) :
    Order<BullSell>(slot, pair, t, counterPrice, counterVolume, SELL, OrderType.BULL_SELL) {
    override fun change(counterPrice: Double, counterVolume: Double): BullSell =
        BullSell(slot, pair, t, counterPrice, counterVolume)
}

class BearBuy(slot: Int, pair: Pair, t: Number, counterPrice: Double, counterVolume: Double) :
    Order<BearBuy>(slot, pair, t, counterPrice, counterVolume, BUY, OrderType.BEAR_BUY) {
    override fun change(counterPrice: Double, counterVolume: Double): BearBuy =
        BearBuy(slot, pair, t, counterPrice, counterVolume)
}

class BearSell(slot: Int, pair: Pair, t: Number, counterPrice: Double, counterVolume: Double) :
    Order<BearSell>(slot, pair, t, counterPrice, counterVolume, SELL, OrderType.BEAR_SELL) {
    override fun change(counterPrice: Double, counterVolume: Double): BearSell =
        BearSell(slot, pair, t, counterPrice, counterVolume)
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
    fun copy(): O = change()

    override fun equals(other: Any?): Boolean = (other as? Order<*>)?.let { o ->
        o.slot == slot
                && o.counterVolume == counterVolume
                && o.counterPrice == counterPrice
                && o.t == t
                && o.direction == direction
    } ?: false

    override fun hashCode(): Int =
        slot.hashCode() + counterVolume.hashCode() + counterPrice.hashCode() + t.hashCode() + direction.hashCode()

    override fun toString(): String = "Order(slot=$slot; pair=$pair; t=$t; counterPrice=$counterPrice; " +
            "counterVolume=$counterVolume; direction=$direction)"
}