package dev.zieger.plottingcompose.di

import dev.zieger.exchange.dto.DataSource
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.definition.ChartDefinition
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication

private lateinit var di: KoinApplication
val koin: Koin get() = di.koin

fun initDi(
    definition: ChartDefinition<out Input>,
    source: DataSource<out Input>
) = koinApplication {
    modules(
        coroutineModule(),
        inputModule(source),
        definitionModule(definition),
        indicatorModule(),
        strategyModule(),
        *definition.charts.map { chartModule(it) }.toTypedArray()
    )
}.also { di = it }
