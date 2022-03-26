package dev.zieger.plottingcompose.indicators.candles

import androidx.compose.ui.geometry.Offset
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.indicators.Indicator
import dev.zieger.plottingcompose.indicators.IndicatorDefinition
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.utils.misc.whenNotNull

data class SupResParameter(
    val minMaxThreshold: Int = 2,
    val num: Int = 2,
    val iter: Int = 1000,
    val threshDist: Double = 1.0,
    val inlierRatio: Double = 0.9,
    val bbParams: BollingerBandsParameter = BollingerBandsParameter()
)

class SupRes(
    private val param: SupResParameter,
    private val bbHigh: Slot<ICandle, Output.Scalar> = BollingerBands.key(param.bbParams) with BollingerBands.HIGH,
    private val bbLow: Slot<ICandle, Output.Scalar> = BollingerBands.key(param.bbParams) with BollingerBands.LOW
) : Indicator<ICandle>(
    key(param), listOf(MIN, MAX), bbHigh.key, bbLow.key
) {
    companion object : IndicatorDefinition<SupResParameter>() {
        override fun key(param: SupResParameter) = Key("SupRes", param) { SupRes(param) }

        val MIN = Port<Output.Scalar>("Min")
        val MAX = Port<Output.Scalar>("Max")

        val SUPPORT = Port<Output.Line>("Support")
        val RESISTANCE = Port<Output.Line>("Resistance ")
    }

    private val maxValues = HashSet<Offset>()
    private val minValues = HashSet<Offset>()

    private var increasing = 0
    private var decreasing = 0
    private var lastIncreased = false
    private var lastIncreasedPair: Offset = Offset.Zero
    private var lastDecreasedPair: Offset = Offset.Zero
    private var prev: ICandle? = null
    private var idx: Int = 1

    override suspend fun ProcessingScope<ICandle>.process() {
        prev?.also { p ->
            if (p.openTime > input.openTime)
                idx = 1

//            fillMaxMinValues(p)
            whenNotNull(
                bbLow.value()?.scalar?.toFloat(),
                bbHigh.value()?.scalar?.toFloat()
            ) { bbL, bbH ->
                when {
                    input.high > bbH -> set(MAX, Output.Scalar(input.x, input.high))
                    input.low < bbL -> set(MIN, Output.Scalar(input.x, input.low))
                }
            }

//            when {
//                input.close > p.close -> {
//                    increasing++
//                    if (increasing == 3) {
//                        lastIncreasedPair = Offset(idx.toFloat(), p.high.toFloat())
//                        if (lastDecreasedPair != Offset.Zero)
//                            set(SUPPORT, Output.Line(lastDecreasedPair, lastIncreasedPair))
//                                .also { println("support found ($lastDecreasedPair to $lastIncreasedPair)") }
//                        lastDecreasedPair = lastIncreasedPair
//                    }
//                    decreasing = 0
//                }
//                input.close < p.close -> {
//                    decreasing++
//                    if (decreasing == 3) {
//                        lastDecreasedPair = Offset(idx.toFloat(), p.low.toFloat())
//                        if (lastIncreasedPair != Offset.Zero)
//                            set(RESISTANCE, Output.Line(lastIncreasedPair, lastDecreasedPair))
//                                .also { println("resistance found ($lastIncreasedPair to $lastDecreasedPair)") }
//                        lastIncreasedPair = lastDecreasedPair
//                    }
//                    increasing = 0
//                }
//            }
        }
        prev = input
        idx++
    }

//    private fun ProcessingScope<ICandle>.fillMaxMinValues(
//        p: ICandle
//    ) {
//        when {
//            p.high < input.high -> {
//                when (lastIncreased) {
//                    true -> increasing++
//                    false -> increasing = 1
//                }
//                lastIncreasedPair = Offset(input.x.toFloat(), input.high.toFloat())
//                lastIncreased = true
//            }
//            p.low > input.low -> {
//                when (lastIncreased) {
//                    true -> decreasing = 1
//                    false -> decreasing++
//                }
//                lastDecreasedPair = Offset(input.x.toFloat(), input.low.toFloat())
//                lastIncreased = false
//            }
//        }
//
//        when {
//            increasing >= param.minMaxThreshold && decreasing >= param.minMaxThreshold -> {
//                when (lastIncreased) {
//                    true -> {
//                        println("min found $lastDecreasedPair")
//                        minValues.add(lastDecreasedPair)
//                        set(MIN, Output.Offset(lastDecreasedPair))
//                    }
//                    false -> {
//                        println("max found $lastIncreasedPair")
//                        maxValues.add(lastIncreasedPair)
//                        set(MAX, Output.Offset(lastIncreasedPair))
//                    }
//                }
//            }
//        }
//    }
}