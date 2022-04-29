package dev.zieger.plottingcompose.di

import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.*
import dev.zieger.plottingcompose.definition.ChartDefinition
import dev.zieger.plottingcompose.di.Scope.MAIN
import org.koin.core.qualifier.named
import org.koin.dsl.module

val CHART_DEFINITION = named("CHART_DEFINITION")
val PROCESSING_SOURCE = named("PROCESSING_SOURCE")

fun definitionModule(definition: ChartDefinition<out Input>) = module {

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
    single { (drawScope: DrawScope) -> GlobalScope(drawScope, get(), get(), get(), get(CHART_DEFINITION)) }

    single {
        ProcessingController(
            get(), get(), get(), get(MAIN)
        )
    }
}

class GlobalScope(
    drawScope: DrawScope,
    th: TransformationHolder,
    sh: GlobalSizeHolder,
    ps: GlobalProcessingScopeHolder,
    val definition: ChartDefinition<Input>
) : DrawScope by drawScope, TransformationHolder by th, GlobalSizeHolder by sh, GlobalProcessingScopeHolder by ps