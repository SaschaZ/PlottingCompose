package dev.zieger.plottingcompose.di

import dev.zieger.candleproxy.dto.CandleSource
import dev.zieger.plottingcompose.definition.ChartDefinition
import dev.zieger.plottingcompose.indicators.candles.IndicatorCandle
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

private lateinit var di: KoinApplication
val koin: Koin get() = di.koin

fun initDi(
    definition: ChartDefinition<IndicatorCandle>,
    candleSource: CandleSource
) = startKoin {
    modules(
        coroutineModule(),
        definitionModule(definition),
        indicatorModule(),
        inputModule(candleSource),
        strategyModule()
    )
    modules(
        *definition.charts.map { chartModule(it) }.toTypedArray()
    )
}.also { di = it }
