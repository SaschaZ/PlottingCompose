@file:Suppress("MemberVisibilityCanBePrivate")

package dev.zieger.plottingcompose.indicators.candles

import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.processor.ProcessingScope

data class SmaParameter(
    val length: Int,
    val singles: Slot<ICandle, Output.Container<Output.Scalar>> = Singles.key(SinglesParameter(length)) with Singles.CLOSES
)

data class Sma(
    val params: SmaParameter
) : Indicator<ICandle>(
    key(params), listOf(SMA), Single.key()
) {

    companion object : IndicatorDefinition<SmaParameter>() {

        override fun key(param: SmaParameter) = Key("Sma", param) { Sma(param) }
        fun key(
            length: Int,
            source: Slot<ICandle, Output.Container<Output.Scalar>> = Singles.key(SinglesParameter(length)) with Singles.CLOSES
        ) = key(SmaParameter(length, source))

        val SMA = Port<Output.Scalar>("Sma")
    }

    private var average = 0f
    private var items = HashMap<Long, Float>()

    override suspend fun ProcessingScope<ICandle>.process() {
        params.singles.value(data)?.items?.also { closes ->
            set(SMA, Output.Scalar(input.x, closes.map { it.scalar.toFloat() }.average()))
        }
    }
}