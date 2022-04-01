package dev.zieger.plottingcompose.processor

import dev.zieger.plottingcompose.InputContainer
import dev.zieger.plottingcompose.definition.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class Processor<I : Input>(private val keys: List<Key<I, *>>) {

    constructor(vararg unit: Key<I, *>) : this(unit.toList())

    fun process(input: Flow<InputContainer<I>>): Flow<Pair<Long, ProcessingScope<I>>> = flow {
        val units = HashMap<Key<I, *>, ProcessingUnit<I>>()
        fun Key<I, *>.buildUnits() {
            units.getOrPut(this) { invoke().also { it.dependsOn.forEach { d -> d.buildUnits() } } }
        }
        keys.forEach { it.buildUnits() }

        input.collect { (input, idx) ->
            emit(idx to ProcessingScope(input).apply {
                units.doProcess()
            })
        }
    }
}

//fun Processor<ICandle>.processInterpolated(input: Flow<TradeCandle>): Flow<ProcessingScope<ICandle>> =
//    flow {
//        input.collect { candle ->
//            var newVolume = 0L
//            val lastIndex = candle.trades.lastIndex
//            candle.trades.forEachIndexed { idx, trade ->
//                val newClose = trade.price
//                newVolume += trade.baseVolume.toLong()
//                val newCandle: ICandle = candle.run {
//                    Candle(
//                        Symbol.BTCUSD, interval, trade.time.timeStamp, open,
//                        high.coerceAtLeast(newClose), newClose,
//                        low.coerceAtMost(newClose), newVolume, "",
//                        isUpdate = idx < lastIndex, isComplete = idx == lastIndex
//                    )
//            }
//            emit(ProcessingScope(newCandle, units).apply {
//                units.doProcess()
//            })
//        }
//    }
//}

fun <I : Input> processorOf(
    idx: Int = 0,
    label: ((I) -> Output.Label)? = null,
    selector: (I) -> Number,
) = SingleDataPlot(idx, label, selector)

open class SingleDataPlot<I : Input>(
    val idx: Int = 0,
    label: ((I) -> Output.Label)? = null,
    private val selector: (I) -> Number,
) : ProcessingUnit<I>(key(idx, label, selector), listOf(SingleDataValue(idx))) {

    companion object {
        fun <I : Input> key(
            idx: Int,
            label: ((I) -> Output.Label)? = null,
            selector: (I) -> Number
        ) = Key<I, Any>("SingleDataPlot$idx", Unit) {
            SingleDataPlot(idx, label, selector)
        }

        fun SingleDataValue(idx: Int) = Port<Output.Scalar>("SingleDataValue$idx")
        fun SingleDataLabel(idx: Int) = Port<Output.Label>("SingleDataLabel$idx")
    }

    fun valueSlot() = key with SingleDataValue(idx)
    fun labelSlot() = key with SingleDataLabel(idx)

    override suspend fun ProcessingScope<I>.process() {
        set(SingleDataValue(idx), Output.Scalar(input.x, selector(input)))
    }
}