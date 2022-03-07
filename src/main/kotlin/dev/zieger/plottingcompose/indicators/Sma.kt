@file:Suppress("MemberVisibilityCanBePrivate")

package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.indicators.Singles.Companion.CLOSES
import dev.zieger.plottingcompose.processor.ProcessingScope

data class SmaParameter(
    val length: Int,
    val source: Slot<ICandle, Output.Container<Output.Scalar>> = Singles.key(SinglesParameter(length)) with CLOSES
)

data class Sma(
    val params: SmaParameter
) : Indicator<ICandle>(
    key(params), listOf(SMA), Singles.key(SinglesParameter(params.length))
) {

    companion object : IndicatorDefinition<SmaParameter>() {

        override fun key(param: SmaParameter) = Key("Sma", param) { Sma(param) }
        fun key(
            length: Int, source: Slot<ICandle, Output.Container<Output.Scalar>> =
                Singles.key(SinglesParameter(length)) with CLOSES
        ) = key(SmaParameter(length, source))

        val SMA = Port<Output.Scalar>("Sma")
    }

    private var average: Double = 0.0
    private var items: Int = 0

    override suspend fun ProcessingScope<ICandle>.process() {
        params.source.value(data)?.let { closes ->
            set(SMA, Output.Scalar(input.x, closes.items.map { it.scalar.toDouble() }.average()))
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