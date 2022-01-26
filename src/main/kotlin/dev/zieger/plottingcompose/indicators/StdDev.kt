package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.definition.with
import dev.zieger.plottingcompose.processor.ProcessingScope
import kotlin.math.pow
import kotlin.math.sqrt

data class StdDevParameter(
    val length: Int,
    val singles: Slot<List<Double>, ICandle> = Singles.key(SinglesParameter(length)) with Singles.CLOSES
)

data class StdDev(
    val params: StdDevParameter
) : Indicator(
    key(params), listOf(STD_DEV),
    params.singles.key
) {
    companion object : IndicatorDefinition<StdDevParameter>() {

        override fun key(param: StdDevParameter) = Key("StdDev", param) { StdDev(param) }
        fun key(length: Int) = key(StdDevParameter(length))

        val STD_DEV = Port<Double>("StdDev")
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        params.singles.value(data)?.let { closes ->
            val mean = closes.average()
            val stdDev = sqrt(closes.map { it - mean }.map { it.pow(2) }.average())
            set(STD_DEV, stdDev)
        }
    }
}