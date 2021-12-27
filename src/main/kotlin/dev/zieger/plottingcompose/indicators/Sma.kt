@file:Suppress("MemberVisibilityCanBePrivate")

package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.with
import dev.zieger.plottingcompose.indicators.Singles.Companion.CLOSES
import dev.zieger.plottingcompose.processor.ProcessingScope

data class Sma(
    val length: Int, val source: Port<List<Float>> = CLOSES,
    private val singles: Singles = Singles(length)
) : Indicator(
    key(length, source), listOf(SMA), listOf(singles)
) {

    companion object {

        fun key(length: Int, source: Port<List<Float>>) = "Sma$length$source"

        val SMA = Port<Float>("Sma")
    }

    private var average: Double = 0.0
    private var items: Int = 0

    override suspend fun ProcessingScope<ICandle>.process() {
        source.extractValue(data, key with source)?.let { closes ->
            set(SMA, closes.average())
//            if (items == length) {
//                average -= closes.first() / length
//                items--
//            }
//
//            average += closes.last() / length
//            items++
//
//            if (items == length)
//                set(SMA, average)
        }
    }
}