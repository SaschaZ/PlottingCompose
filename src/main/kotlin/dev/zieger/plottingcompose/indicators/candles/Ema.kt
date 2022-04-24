package dev.zieger.plottingcompose.indicators.candles

import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.processor.ProcessingScope

data class EmaParameter(
    val length: Int, val source: Slot<IndicatorCandle, Output.Scalar> =
        Single.key() with Single.CLOSE
)

class Ema(private val params: EmaParameter) : Indicator<IndicatorCandle>(
    key(params), listOf(EMA), params.source.key
) {

    companion object : IndicatorDefinition<EmaParameter>() {

        override fun key(param: EmaParameter) = Key("Ema", param) { Ema(param) }
        fun key(length: Int, source: Slot<IndicatorCandle, Output.Scalar> = Single.key() with Single.CLOSE) =
            key(EmaParameter(length, source))

        val EMA = Port<Output.Scalar>("Ema")
    }

    private val k = 2.0 / (params.length + 1)
    private var ema: Double = 0.0

    override suspend fun ProcessingScope<IndicatorCandle>.process() {
        params.source.value()?.let { v ->
            ema = v.scalar.toDouble() * k + ema * (1 - k)
            set(EMA, Output.Scalar(input.x, ema))
        }
    }
}