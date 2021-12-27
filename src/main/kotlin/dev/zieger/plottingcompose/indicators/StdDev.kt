package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.extractValue
import dev.zieger.plottingcompose.definition.with
import dev.zieger.plottingcompose.processor.ProcessingScope
import kotlin.math.pow
import kotlin.math.sqrt

data class StdDev(
    val length: Int,
    private val singles: Singles = Singles(length)
) : Indicator(
    key(length), listOf(STD_DEV),
    listOf(singles)
) {
    companion object {

        fun key(length: Int) = "StdDev$length"

        val STD_DEV = Port<Float>("StdDev")
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        (singles.key with Singles.CLOSES).extractValue(data)?.let { closes ->
            val mean = closes.average()
            val stdDev = sqrt(closes.map { it - mean }.map { it.pow(2) }.average())
            set(STD_DEV, stdDev)
        }
    }
}