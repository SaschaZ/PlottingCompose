package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.with
import dev.zieger.plottingcompose.processor.ProcessingScope

data class BollingerBandsParameter(
    val length: Int,
    val stdDevFactor: Double = 2.0,
    val averageType: AverageType = AverageType.SMA
)

data class BollingerBands(
    val param: BollingerBandsParameter,
) : Indicator(
    key(param), listOf(HIGH, MID, LOW),
    StdDev.key(param.length), param.averageType(param.length)
) {

    companion object : IndicatorDefinition<BollingerBandsParameter>() {

        override fun key(param: BollingerBandsParameter) =
            Key("BollingerBands", param) { BollingerBands(param) }

        fun key(length: Int, stdDevFactor: Double, averageType: AverageType) =
            key(BollingerBandsParameter(length, stdDevFactor, averageType))

        val HIGH = Port<Double>("High")
        val MID = Port<Double>("Mid")
        val LOW = Port<Double>("Low")
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        (StdDev.key(param.length) with StdDev.STD_DEV).value(data)?.let { stdDev ->
            (param.averageType(param.length) with when (param.averageType) {
                AverageType.SMA -> Sma.SMA
                AverageType.EMA -> Ema.EMA
            }).value(data)?.let { avg ->
                val width = stdDev * param.stdDevFactor
                set(HIGH, avg + width)
                set(MID, avg)
                set(LOW, avg - width)
            }
        }
    }
}