@file:Suppress("FunctionName", "unused")

package dev.zieger.plottingcompose.processor

import dev.zieger.plottingcompose.InputContainer
import dev.zieger.plottingcompose.definition.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.reflect.KClass

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

inline fun <I : Input, reified O : Output> processorOf(
    idx: Int = 0,
    noinline selector: (I) -> O,
) = SingleDataPlot(idx, O::class, selector)

open class SingleDataPlot<I : Input, O : Output>(
    val idx: Int = 0,
    private val outputKlass: KClass<O>,
    private val selector: (I) -> O,
) : ProcessingUnit<I>(key(idx, outputKlass, selector), listOf(SingleDataValue<O>(idx, outputKlass))) {

    companion object {
        fun <I : Input, O : Output> key(
            idx: Int,
            klass: KClass<O>,
            selector: (I) -> O
        ) = Key<I, Any>("SingleDataPlot$idx", Unit) {
            SingleDataPlot(idx, klass, selector)
        }

        fun <O : Output> SingleDataValue(idx: Int, klass: KClass<O>) = Port(klass, "SingleDataValue$idx")
    }

    fun valueSlot() = key with SingleDataValue(idx, outputKlass)

    override suspend fun ProcessingScope<I>.process() {
        set(SingleDataValue(idx, outputKlass), selector(input))
    }
}