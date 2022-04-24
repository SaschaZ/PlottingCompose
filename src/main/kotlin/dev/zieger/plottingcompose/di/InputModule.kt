package dev.zieger.plottingcompose.di

import dev.zieger.bybitapi.dto.enumerations.Interval.H1
import dev.zieger.bybitapi.dto.enumerations.Symbol.BTCUSD
import dev.zieger.candleproxy.dto.CandleSource
import dev.zieger.candleproxy.dto.ICandle
import dev.zieger.plottingcompose.InputContainer
import dev.zieger.plottingcompose.ProcessingSource
import dev.zieger.plottingcompose.indicators.candles.IndicatorCandle
import dev.zieger.plottingcompose.indicators.candles.IndicatorCandleImpl
import dev.zieger.utils.time.ITimeStamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.koin.dsl.module

fun inputModule(candleSource: CandleSource) = module {

    single { candleSource }
    single(PROCESSING_SOURCE) {
        object : ProcessingSource<IndicatorCandle> {
            val candleSource = get<CandleSource>()

            override fun input(range: ClosedRange<ITimeStamp>): Flow<InputContainer<IndicatorCandle>> =
                this.candleSource.candles(BTCUSD, H1, range).toInputContainer()

            private fun Flow<ICandle>.toInputContainer(): Flow<InputContainer<IndicatorCandle>> {
                var lastX: ICandle? = null
                var lastIdx = 0L

                return mapNotNull {
                    val x = it.time
                    when {
                        lastX?.time == x || lastX == null -> {
                            lastX = it
                            InputContainer(IndicatorCandleImpl(it), lastIdx)
                        }
                        lastX?.let { lx -> lx.time < x } == true -> {
                            lastX = it
                            InputContainer(IndicatorCandleImpl(it), ++lastIdx)
                        }
                        else -> null
                    }
                }
            }
        }
    }
}