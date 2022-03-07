@file:OptIn(ExperimentalComposeUiApi::class)

package dev.zieger.plottingcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.mouse.MouseScrollOrientation
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import dev.zieger.plottingcompose.definition.ChartDefinition
import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.keys
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.processor.Processor
import dev.zieger.plottingcompose.scopes.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : Input> MultiChart(
    definition: ChartDefinition<T>,
    input: Flow<T>,
    modifier: Modifier = Modifier
) {
    val scopes = remember { mutableStateListOf<ProcessingScope<T>>() }
    val scope = rememberCoroutineScope()
    remember {
        scope.launch {
            scopes.clear()
            Processor(definition.keys()).process(input).collect { s ->
                scopes += s
            }
        }
    }

    val chartEnvironment = remember { ChartEnvironment() }
    Canvas(modifier
        .fillMaxSize()
        .onSizeChanged { chartEnvironment.chartSize.value = it }
        .mouseScrollFilter { event, _ ->
            ((event.delta as? MouseScrollUnit.Line)?.value
                ?: (event.delta as? MouseScrollUnit.Page)?.value)?.let { delta ->
                when (event.orientation) {
                    MouseScrollOrientation.Vertical -> {
                        chartEnvironment.scale.value =
                            (chartEnvironment.scale.value.x + 0.5f / delta).coerceAtLeast(1f) to 1f
                        chartEnvironment.mousePosition.value?.also { mp ->
                            val diff = (chartEnvironment.scaleCenter.value - mp) / chartEnvironment.scale.value
                            chartEnvironment.scaleCenter.value =
                                Offset(chartEnvironment.scaleCenter.value.x - diff.x, 0f)
                        }
                    }
                    MouseScrollOrientation.Horizontal -> {
                        chartEnvironment.translation.value =
                            chartEnvironment.translation.value.run { copy(x + delta * chartEnvironment.scale.value.x * 2) }
                    }
                }
            }
            false
        }.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consumeAllChanges()
                chartEnvironment.translation.value = chartEnvironment.translation.value.run { copy(x + dragAmount.x) }
            }
        }.pointerMoveFilter(onExit = {
            chartEnvironment.mousePosition.value = null
            false
        }, onMove = {
            chartEnvironment.mousePosition.value = it
            false
        })
    ) {
        ChartDrawScope(definition, this, scopes, chartEnvironment).draw()
    }
}

private operator fun Offset.div(value: Pair<Float, Float>): Offset = copy(x / value.x, y / value.y)

fun <T : Input> IChartDrawScope<T>.draw() {
    drawRect(definition.backgroundColor, rootRect)
    definition.charts.forEach { chart ->
        PlotDrawScope(chart, this).draw()
    }
}

fun <T : Input> IPlotDrawScope<T>.draw() {
    drawRect(chart.backgroundColor, plotBorderRect)
    drawRect(chart.borderColor, plotBorderRect, Stroke(1f))

    clipRect(yLabelRect) {
        scale(1f to finalScale.y, scaleCenter.value.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                yTicks.forEach { (value, label) ->
                    val y =
                        yLabelRect.bottom - value.toFloat() / heightDivisor + yLabelHeight / 3
                    drawText(label, Offset(yLabelRect.left, y), 20f, chart.tickLabelColor)
                }
            }
        }
    }
    clipRect(plotBorderRect) {
        scale(1f to finalScale.y, scaleCenter.value.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                yTicks.forEach { (value, _) ->
                    val y = yLabelRect.bottom - value.toFloat() / heightDivisor
                    drawLine(
                        chart.gridColor, Offset(plotBorderRect.left, y),
                        Offset(plotBorderRect.right, y), 1f
                    )
                }
            }
        }
    }
    clipRect(yTickRect) {
        scale(1f to finalScale.y, scaleCenter.value.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                yTicks.forEach { (value, _) ->
                    val y = yLabelRect.bottom - value.toFloat() / heightDivisor
                    drawLine(
                        chart.tickColor, Offset(yTickRect.left, y),
                        Offset(yTickRect.right, y), 1f
                    )
                }
            }
        }
    }

    clipRect(xLabelRect) {
        scale(finalScale.x to 1f, scaleCenter.value.copy(y = 0f)) {
            translate(finalTranslation.copy(y = 0f)) {
                xTicks.forEach { (value, label) ->
                    val x = xLabelRect.left + value.toFloat() / widthDivisor
                    drawText(
                        label,
                        Offset(x, xLabelRect.bottom),
                        20f,
                        chart.tickLabelColor,
                        1 / finalScale.x,
                        TextAlign.Center
                    )
                }
            }
        }
    }
    clipRect(plotBorderRect) {
        scale(finalScale.x to 1f, scaleCenter.value.copy(y = 0f)) {
            translate(finalTranslation.copy(y = 0f)) {
                xTicks.forEach { (value, _) ->
                    val x = xLabelRect.left + value.toFloat() / widthDivisor
                    drawLine(
                        chart.gridColor, Offset(x, plotBorderRect.top),
                        Offset(x, plotBorderRect.bottom), 1f / finalScale.x
                    )
                }
            }
        }
    }
    clipRect(xTickRect) {
        scale(finalScale.x to 1f, scaleCenter.value.copy(y = 0f)) {
            translate(finalTranslation.copy(y = 0f)) {
                xTicks.forEach { (value, _) ->
                    val x = xLabelRect.left + value.toFloat() / widthDivisor
                    drawLine(
                        chart.tickColor, Offset(x, xTickRect.top),
                        Offset(x, xTickRect.bottom), 1f / finalScale.x
                    )
                }
            }
        }
    }

    clipRect(plotRect) {
        scale(finalScale, scaleCenter.value) {
            translate(finalTranslation) {
                chart.plots.forEach { style ->
                    style.run {
                        drawSeries(chartData)
                    }
                }
            }
        }
    }

    translationOffset.value = Offset(0f, visibleYPixelRange.start)

    println(
        "visibleXPixelRange=$visibleXPixelRange; visibleXValueRange=$xValueRange\n" +
                "visibleYPixelRange=$visibleYPixelRange; visibleYValueRange=$yValueRange\n" +
                "translationOffset=${translationOffset.value}; finalTranslation=$finalTranslation\n" +
                "scaleCenter=${scaleCenter.value}; scale=${scale.value}\n" +
                "heightDivisor=$heightDivisor(${System.identityHashCode(this@draw)})"
    )
}
