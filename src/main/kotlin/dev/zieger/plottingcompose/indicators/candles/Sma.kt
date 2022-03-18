@file:Suppress("MemberVisibilityCanBePrivate")

package dev.zieger.plottingcompose.indicators.candles

import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.processor.ProcessingScope
import java.util.*

data class SmaParameter(
    val length: Int,
    val source: Slot<ICandle, Output.Scalar> = Single.key() with Single.CLOSE
)

data class Sma(
    val params: SmaParameter
) : Indicator<ICandle>(
    key(params), listOf(SMA), Single.key()
) {

    companion object : IndicatorDefinition<SmaParameter>() {

        override fun key(param: SmaParameter) = Key("Sma", param) { Sma(param) }
        fun key(
            length: Int, source: Slot<ICandle, Output.Scalar> = Single.key() with Single.CLOSE
        ) = key(SmaParameter(length, source))

        val SMA = Port<Output.Scalar>("Sma")
    }

    private var average = 0f
    private var items = LinkedList<Float>()

    override suspend fun ProcessingScope<ICandle>.process() {
        params.source.value(data)?.scalar?.toFloat()?.let { close ->
            if (items.size == params.length)
                average -= items.removeFirst() / params.length

            average += close / params.length
            items += close

            if (items.size == params.length)
                set(SMA, Output.Scalar(input.x, average))
            else
                set(SMA, Output.Scalar(input.x, close))

        }
    }
}