package dev.zieger.plottingcompose.processor

import dev.zieger.plottingcompose.definition.InputContainer
import dev.zieger.plottingcompose.definition.Key
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class Processor<T : InputContainer>(private val keys: List<Key<T>>) {

    constructor(vararg unit: Key<T>) : this(unit.toList())

    fun process(input: Flow<T>): Flow<ProcessingScope<T>> = flow {
        val units = HashMap<Key<T>, ProcessingUnit<T>>()
        fun Key<T>.buildUnits(): Unit {
            units.getOrPut(this) { invoke().also { it.dependsOn.forEach { d -> d.buildUnits() } } }
        }
        keys.forEach { it.buildUnits() }

        input.collect { inp ->
            emit(ProcessingScope(inp).apply {
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