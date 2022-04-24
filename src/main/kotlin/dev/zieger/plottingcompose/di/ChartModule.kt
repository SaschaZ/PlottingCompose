package dev.zieger.plottingcompose.di

import dev.zieger.plottingcompose.*
import dev.zieger.plottingcompose.definition.Chart
import dev.zieger.plottingcompose.indicators.candles.IndicatorCandle
import org.koin.core.qualifier.named
import org.koin.dsl.module

val CHART = named("CHART")

fun chartModule(chart: Chart<IndicatorCandle>) = module {
    scope<Chart<IndicatorCandle>> {
        scoped(CHART) { chart }
        scoped<SizeHolder> { SizeHolderImpl(get(CHART), get(), get()) }
        scoped<ProcessingScopeHolder> { ProcessingScopeHolderImpl(get(CHART), get(), get(), get()) }
        scoped<LabelSizeHolder> { LabelSizeHolderImpl(get(CHART)) }
    }
}