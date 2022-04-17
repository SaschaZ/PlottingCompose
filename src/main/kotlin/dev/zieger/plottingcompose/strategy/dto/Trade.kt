package dev.zieger.plottingcompose.strategy.dto

import dev.zieger.plottingcompose.definition.Output

data class Trade(
    val t: Number,
    val order: Order<*>
) : Output.Scalar(t, order.counterPrice) {

    fun copyDeep(t: Number = this.t, order: Order<*> = this.order) = Trade(t, order.copy())
}