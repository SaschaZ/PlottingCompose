package dev.zieger.plottingcompose.strategy.dto

import dev.zieger.plottingcompose.definition.Output

class Order(
    val pair: Pair,
    val t: Number,
    var counterPrice: Double,
    var counterVolume: Double,
    val direction: Direction
) : Output.Scalar(t, counterPrice) {
    val baseVolume: Double get() = counterVolume / counterPrice

    override fun toString(): String = "Order(pair=$pair; t=$t; counterPrice=$counterPrice; " +
            "counterVolume=$counterVolume; direction=$direction)"
}