package dev.zieger.plottingcompose.di

import dev.zieger.plottingcompose.indicators.candles.*
import org.koin.dsl.module

fun indicatorModule() = module {

    factory { (param: BollingerBandsParameter) -> BollingerBands(param) }
    factory { (length: Int) -> Candles(length) }
    factory { (param: EmaParameter) -> Ema(param) }
    factory { Ohcl() }
    factory { Single() }
    factory { (param: SmaParameter) -> Sma(param) }
    factory { (param: StdDevParameter) -> StdDev(param) }
    factory { (param: SupResParameter) -> SupRes(param) }
    factory { Volume() }
}