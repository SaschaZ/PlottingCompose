package dev.zieger.plottingcompose.indicators

import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Value
import dev.zieger.plottingcompose.processor.ProcessingScope

data class BollingerBands(
    val length: Int,
    val stdDevFactor: Double = 2.0,
    val averageType: AverageType = AverageType.SMA,
    private val stdDev: StdDev = StdDev(length),
    private val average: Indicator<Double, *> = averageType(length)
) : Indicator<Triple<Double, Double, Double>, Any>(
    key(length, stdDevFactor, averageType), listOf(HIGH, MID, LOW),
    listOf(stdDev, average)
) {

    companion object {

        fun key(length: Int, stdDevFactor: Double, averageType: AverageType) =
            "BollingerBands$length$stdDevFactor$averageType".replace(".", "")

        val HIGH = Port("High")
        val MID = Port("Mid")
        val LOW = Port("Low")

        private val Value.toBollingerValue: Double get() = (this as Number).toDouble()
    }

    override fun extractData(
        data: Map<Key, Map<Port, Value?>>,
        parameter: Any?
    ): Triple<Double, Double, Double>? = data[key]?.run {
        get(HIGH)?.toBollingerValue?.let { h ->
            get(MID)?.toBollingerValue?.let { m ->
                get(LOW)?.toBollingerValue?.let { l ->
                    Triple(l, m, h)
                }
            }
        }
    }

    override suspend fun ProcessingScope<ICandle>.process() {
        fun ProcessingScope<ICandle>.bollinger() {
            stdDev.extractData(data)?.let { stdDev ->
                average.extractData(data)?.let { avg ->
                    val width = stdDev * stdDevFactor
                    set(HIGH, avg + width)
                    set(MID, avg)
                    set(LOW, avg - width)
                }
            }
        }

        bollinger()
    }
}