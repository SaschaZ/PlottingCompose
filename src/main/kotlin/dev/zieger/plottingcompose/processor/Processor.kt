package dev.zieger.plottingcompose.processor

import dev.zieger.plottingcompose.InputContainer
import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.Key
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