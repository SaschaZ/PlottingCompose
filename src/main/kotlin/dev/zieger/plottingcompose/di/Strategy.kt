package dev.zieger.plottingcompose.di

import dev.zieger.plottingcompose.indicators.candles.IndicatorCandle
import dev.zieger.plottingcompose.strategy.*
import org.koin.dsl.module

fun strategyModule() = module {

    single<Exchange<IndicatorCandle>> { (param: MockExchangeParameter) -> MockExchange(param) }

    single { (param: BbStrategyOptions) -> BbStrategy(param, get()) }
}