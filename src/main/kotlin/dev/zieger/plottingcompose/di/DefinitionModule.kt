package dev.zieger.plottingcompose.di

import dev.zieger.plottingcompose.*
import dev.zieger.plottingcompose.definition.ChartDefinition
import dev.zieger.plottingcompose.di.Scope.MAIN
import dev.zieger.plottingcompose.indicators.candles.IndicatorCandle
import org.koin.core.qualifier.named
import org.koin.dsl.module

val CHART_DEFINITION = named("CHART_DEFINITION")
val PROCESSING_SOURCE = named("PROCESSING_SOURCE")

fun definitionModule(definition: ChartDefinition<IndicatorCandle>) = module {

    single(CHART_DEFINITION) { definition }

    single<TransformationHolder> { TransformationHolderImpl(get()) }
    single<GlobalSizeHolder> { GlobalSizeHolderImpl(get(CHART_DEFINITION)) }
    single<GlobalProcessingScopeHolder> {
        GlobalProcessingScopeHolderImpl(
            get(MAIN),
            get(CHART_DEFINITION),
            get(),
            get(PROCESSING_SOURCE),
            get()
        )
    }
}