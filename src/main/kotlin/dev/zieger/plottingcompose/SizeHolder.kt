package dev.zieger.plottingcompose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import dev.zieger.plottingcompose.definition.Chart
import dev.zieger.plottingcompose.definition.ChartDefinition
import dev.zieger.plottingcompose.indicators.candles.IndicatorCandle

interface GlobalSizeHolder {

    val chartSize: MutableState<IntSize>

    val rootRect: Rect
    val rootBorderRect: Rect
}

class GlobalSizeHolderImpl(
    private val definition: ChartDefinition<IndicatorCandle>
) : GlobalSizeHolder {

    override val chartSize = mutableStateOf(IntSize.Zero)

    override val rootRect: Rect
        get() = Rect(Offset.Zero, chartSize.value.toSize())
    override val rootBorderRect: Rect
        get() = rootRect.run {
            Rect(
                left + definition.margin.left.invoke(chartSize.value).value,
                top + definition.margin.top.invoke(chartSize.value).value,
                right - definition.margin.right.invoke(chartSize.value).value,
                bottom - definition.margin.bottom.invoke(chartSize.value).value
            )
        }
}

interface SizeHolder : GlobalSizeHolder, LabelSizeHolder {

    val chartRect: Rect
    val plotRect: Rect
}

class SizeHolderImpl(
    private val chart: Chart<IndicatorCandle>,
    globalSizeHolder: GlobalSizeHolder,
    labelSizeHolder: LabelSizeHolder
) : SizeHolder,
    GlobalSizeHolder by globalSizeHolder,
    LabelSizeHolder by labelSizeHolder {

    override val chartRect: Rect
        get() = rootBorderRect.run {
            val chartTop = top + chart.definition.charts.subList(0, chart.definition.charts.indexOf(chart))
                .sumOf { it.verticalWeight.toDouble() }.toFloat() * height
            copy(
                top = chartTop,
                right = right - (if (chart.drawYLabels) yLabelRect.width else 0f) -
                        (if (chart.drawYLabels) chart.tickLength(chartSize.value).value / 2f else 0f),
                bottom = chartTop - height * chart.verticalWeight - (if (chart.drawXLabels) xLabelRect.height else 0f) -
                        (if (chart.drawXLabels) chart.tickLength(chartSize.value).value / 2f else 0f)
            )
        }

    override val plotRect: Rect
        get() = chartRect.run {
            Rect(
                left + chart.margin.left.invoke(chartSize.value).value,
                top + chart.margin.top.invoke(chartSize.value).value,
                right - chart.margin.right.invoke(chartSize.value).value,
                bottom - chart.margin.bottom.invoke(chartSize.value).value
            )
        }
}