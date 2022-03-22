//package dev.zieger.plottingcompose.indicators.candles
//
//import dev.zieger.plottingcompose.definition.*
//import dev.zieger.plottingcompose.indicators.Indicator
//import dev.zieger.plottingcompose.indicators.IndicatorDefinition
//import dev.zieger.plottingcompose.processor.ProcessingScope
//import kotlin.math.absoluteValue
//
//data class CandlePatternParameter(
//    val candlesBack: Int = 20
//)
//
//class CandlePattern(
//    private val param: CandlePatternParameter,
//    private val candles: Slot<ICandle, Output.Container<Ohcl.Companion.Ohcl>> =
//        Candles.key(param.candlesBack) with Candles.CANDLES
//) : Indicator<ICandle>(key(param), listOf(PATTERN), candles.key) {
//    companion object : IndicatorDefinition<CandlePatternParameter>() {
//        override fun key(param: CandlePatternParameter): Key<ICandle> = Key("CandlePattern", param) {
//            CandlePattern(param)
//        }
//
//        val PATTERN = Port<Output.Container<Pattern>>("CandlePattern")
//    }
//
//    override suspend fun ProcessingScope<ICandle>.process() {
//        candles.value(data).let { candles ->
//            if (input.relBodyToWicks < 0.3f)
//                set()
//        }
//    }
//
//    val ICandle.relBodyToWicks: Double get() = (open - close).absoluteValue / (high - low).absoluteValue
//}
//
//sealed class Pattern : Output.Label() {
//
//}