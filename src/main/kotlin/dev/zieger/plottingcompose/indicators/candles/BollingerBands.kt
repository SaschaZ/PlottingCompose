package dev.zieger.plottingcompose.indicators.candles

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.with
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.processor.ProcessingScope

data class BollingerBandsParameter(
    val length: Int = 20,
    val stdDevFactor: Double = 2.0,
    val averageType: AverageType = AverageType.SMA
)

data class BollingerBands(
    val param: BollingerBandsParameter,
) : Indicator<ICandle>(
    key(param), listOf(HIGH, MID, LOW),
    StdDev.key(param.length), param.averageType(param.length)
) {

    companion object : IndicatorDefinition<BollingerBandsParameter>() {

        override fun key(param: BollingerBandsParameter) =
            Key("BollingerBands", param) { BollingerBands(param) }

        fun key(length: Int, stdDevFactor: Double, averageType: AverageType) =
            key(BollingerBandsParameter(length, stdDevFactor, averageType))

        val HIGH = Port<Output.Scalar>("High")
        val MID = Port<Output.Scalar>("Mid")
        val LOW = Port<Output.Scalar>("Low")
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        (StdDev.key(param.length) with StdDev.STD_DEV).value(data)?.scalar?.toDouble()?.let { stdDev ->
            (param.averageType(param.length) with when (param.averageType) {
                AverageType.SMA -> Sma.SMA
                AverageType.EMA -> Ema.EMA
            }).value(data)?.scalar?.toDouble()?.let { avg ->
                val width = stdDev * param.stdDevFactor
                set(HIGH, Output.Scalar(input.x, avg + width))
                set(MID, Output.Scalar(input.x, avg))
                set(LOW, Output.Scalar(input.x, avg - width))
            }
        }
    }
}