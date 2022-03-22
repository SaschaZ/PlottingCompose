package dev.zieger.plottingcompose.indicators.candles

import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.processor.ProcessingScope
import kotlin.math.pow
import kotlin.math.sqrt

data class StdDevParameter(
    val length: Int,
    val singles: Slot<ICandle, Output.Container<Output.Scalar>> = Singles.key(SinglesParameter(length)) with Singles.CLOSES
)

data class StdDev(
    val params: StdDevParameter
) : Indicator<ICandle>(
    key(params), listOf(STD_DEV),
    params.singles.key
) {
    companion object : IndicatorDefinition<StdDevParameter>() {

        override fun key(param: StdDevParameter) = Key("StdDev", param) { StdDev(param) }
        fun key(length: Int) = key(StdDevParameter(length))

        val STD_DEV = Port<Output.Scalar>("StdDev")
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        params.singles.value(data)?.items?.asFloats()?.let { closes ->
            val mean = closes.average()
            val stdDev = sqrt(closes.map { it.toDouble() - mean }.map { it.pow(2) }.average())
            set(STD_DEV, Output.Scalar(input.x, stdDev))
        }
    }
}