package dev.zieger.plottingcompose.di

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.*
import dev.zieger.plottingcompose.definition.Chart
import dev.zieger.plottingcompose.indicators.candles.IndicatorCandle
import org.koin.core.qualifier.named
import org.koin.dsl.module

val CHART = named("CHART")

fun <I : Input> chartModule(chart: Chart<I>) = module {
    scope<Chart<I>> {
        scoped(CHART) { chart }
        scoped<SizeHolder> { SizeHolderImpl(get(CHART), get()) }
        scoped<ProcessingScopeHolder> { ProcessingScopeHolderImpl(get(CHART), get(), get(), get()) }
        scoped<FocusInfoHolder> { FocusInfoHolderImpl(get(), get(), get()) }
        scoped { (drawScope: DrawScope) -> ChartScope(get(CHART), get(), get(), get(), get(), drawScope) }
    }
}

class ChartScope(
    val chart: Chart<IndicatorCandle>,
    sh: SizeHolder,
    th: TransformationHolder,
    ps: ProcessingScopeHolder,
    fs: FocusInfoHolder,
    ds: DrawScope
) : TransformationHolder by th, DrawScope by ds, SizeHolder by sh, FocusInfoHolder by fs, ProcessingScopeHolder by ps {

    fun Offset.toScene(): Offset =
        Offset((plotRect.left + x / finalScale.x).toFloat(), plotRect.bottom - y / finalScale.y)
}