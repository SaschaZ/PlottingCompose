@file:OptIn(ExperimentalComposeUiApi::class)
@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED", "MemberVisibilityCanBePrivate", "LeakingThis", "FunctionName")

package dev.zieger.plottingcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.mouse.MouseScrollOrientation
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import dev.zieger.plottingcompose.definition.ChartDefinition
import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.keys
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.processor.Processor
import dev.zieger.plottingcompose.scopes.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow


data class InputContainer<I : Input>(val input: I, val idx: Long)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : Input> MultiChart(
    definition: ChartDefinition<T>,
    input: Flow<T>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scopes = remember { mutableStateListOf<Pair<Long, ProcessingScope<T>>>() }
    var collectJob by remember { mutableStateOf<Job?>(null) }
    var collectFlow by remember { mutableStateOf<Flow<T>?>(null) }

    fun Flow<T>.collect(): Job = scope.launch {
        var lastX: Double? = null
        var lastIdx: Long = -1L
        Processor(definition.keys()).process(mapNotNull {
            val x = it.x.toDouble()
            when {
                lastX == x || lastX == null -> {
                    lastX = x
                    InputContainer(it, lastIdx)
                }
                lastX?.let { lx -> lx < x } == true -> {
                    lastX = x
                    InputContainer(it, ++lastIdx)
                }
                else -> null
            }
        }).collect last@{ (idx, s) ->
            if (!isActive) return@last
            scopes.removeIf { (i, _) -> i == idx }
            scopes.add(idx to s)
        }
    }

    val globalChartEnvironment = remember { GlobalChartEnvironment() }
    val chartEnvironment = remember { definition.charts.associateWith { ChartEnvironment(globalChartEnvironment) } }
    val globalStates = remember { GlobalStates() }
    val states = remember { definition.charts.associateWith { States(globalStates) } }

    if (input != collectFlow) {
        collectJob?.cancel()
        chartEnvironment.values.forEach { it.reset() }
        scopes.clear()
        collectFlow = input
        collectJob = input.collect()
    }

    Canvas(
        modifier
            .fillMaxSize()
            .fillEnvironment(globalStates, globalChartEnvironment)
    ) {
        ChartDrawScope(
            definition,
            this@Canvas,
            ArrayList(scopes).toMap(),
            chartEnvironment,
            states,
        ).draw()
    }
}

private operator fun Offset.div(value: Pair<Float, Float>): Offset = copy(x / value.x, y / value.y)

fun <T : Input> IChartDrawScope<T>.draw() {
    drawRect(definition.backgroundColor, chartEnvironment[definition.charts.first()]!!.rootRect)
    var top = 0f
    definition.charts.forEach { chart ->
        chartEnvironment[chart]!!.run {
            val height = chartRect.height * chart.verticalWeight
            val rect = chartRect.copy(
                top = chartRect.top + top,
                bottom = chartRect.top + top + height
            )
            clipRect(rect) {
                PlotDrawScope(chart, this, this@run, states[chart]!!, rect).draw()
            }
            top += height
        }
    }
}

fun <T : Input> IPlotDrawScope<T>.draw() {
    drawRect(chart.backgroundColor, plotBorderRect)
    drawRect(chart.borderColor, plotBorderRect, Stroke(1f))

    if (chart.drawYLabels)
        clipRect(yLabelRect) {
            scale(1f to scale.value.y, scaleCenter.copy(x = 0f)) {
                translate(finalTranslation.copy(x = 0f)) {
                    yTicks.forEach { (value, label) ->
                        val y = yLabelRect.bottom - value.toFloat() / heightDivisor.value.toFloat() + yLabelHeight / 3
                        drawText(label, Offset(yLabelRect.left, y), 20f, chart.tickLabelColor)
                    }
                }
            }
        }
    clipRect(plotBorderRect) {
        scale(1f to scale.value.y, scaleCenter.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                yTicks.forEach { (value, _) ->
                    val y = yLabelRect.bottom - value.toFloat() / heightDivisor.value.toFloat()
                    drawLine(
                        chart.gridColor, Offset(plotBorderRect.left, y),
                        Offset(plotBorderRect.right, y), 1f
                    )
                }
            }
        }
    }
    clipRect(yTickRect) {
        scale(1f to scale.value.y, scaleCenter.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                yTicks.forEach { (value, _) ->
                    val y = yLabelRect.bottom - value.toFloat() / heightDivisor.value.toFloat()
                    drawLine(
                        chart.tickColor, Offset(yTickRect.left, y),
                        Offset(yTickRect.right, y), 1f
                    )
                }
            }
        }
    }

    if (chart.drawXLabels)
        clipRect(xLabelRect) {
            translate(finalTranslation.copy(y = 0f)) {
                xTicks.forEach { (value, label) ->
                    val x = xLabelRect.left + value.toFloat() / widthDivisor
                    drawText(
                        label,
                        Offset(x, xLabelRect.bottom),
                        20f,
                        chart.tickLabelColor,
                        1f,
                        TextAlign.Center
                    )
                }
            }
        }
    clipRect(plotBorderRect) {
        translate(finalTranslation.copy(y = 0f)) {
            xTicks.entries.toList().forEachIndexed { idx, (value, _) ->
                val x = xLabelRect.left + value.toFloat() / widthDivisor
                drawLine(
                    chart.gridColor, Offset(x, plotBorderRect.top),
                    Offset(x, plotBorderRect.bottom), 1f
                )
                if (idx > 0) {
                    val (v, _) = xTicks.entries.toList()[idx - 1]
                    val step = (value - v) / 4
                    (1..3).map {
                        v + it * step
                    }.map { xLabelRect.left + it.toFloat() / widthDivisor }
                        .forEach { x1 ->
                            drawLine(
                                chart.gridColor, Offset(x1, plotBorderRect.top),
                                Offset(x1, plotBorderRect.bottom), 1f
                            )
                        }
                }
            }
        }
    }

    fun drawXTicks(rect: Rect) {
        clipRect(rect) {
            translate(finalTranslation.copy(y = 0f)) {
                xTicks.forEach { (value, _) ->
                    val x = xLabelRect.left + value.toFloat() / widthDivisor
                    drawLine(
                        chart.tickColor, Offset(x, rect.top),
                        Offset(x, rect.bottom), 1f
                    )
                }
            }
        }
    }

    drawXTicks(x1TickRect)
    drawXTicks(x2TickRect)

    clipRect(plotRect) {
        translate(finalTranslation) {
            chart.plots.forEach { style ->
                style.run {
                    drawSeries(chartData)
                }
            }
        }
    }

//    println(
//        "visibleXPixelRange=$visibleXPixelRange; visibleXValueRange=$xValueRange\n" +
//                "visibleYPixelRange=$visibleYPixelRange; visibleYValueRange=$yValueRange\n" +
//                "translationOffset=${translationOffset.value}; finalTranslation=$finalTranslation\n" +
//                "scaleCenter=${scaleCenter.value}; scale=${scale.value}\n" +
//                "heightDivisor=${heightDivisor.value.toFloat()}; widthDivisor=$widthDivisor"
//    )
}

fun Modifier.fillEnvironment(
    states: GlobalStates,
    chartEnvironment: GlobalChartEnvironment
): Modifier = onSizeChanged { chartEnvironment.chartSize.value = it }
    .mouseScrollFilter { event, _ ->
        ((event.delta as? MouseScrollUnit.Line)?.value
            ?: (event.delta as? MouseScrollUnit.Page)?.value)?.let { delta ->
            when (event.orientation) {
                MouseScrollOrientation.Vertical -> {
                    chartEnvironment.scale.value =
                        (chartEnvironment.scale.value.x.let { it - it * 0.033f * delta }).coerceAtLeast(0.0001f) to 1f

                    chartEnvironment.mousePosition.value?.also { mp ->
                        chartEnvironment.scaleCenterRelative.value = mp / chartEnvironment.chartSize.value
                    }
                }
                MouseScrollOrientation.Horizontal -> {
                    chartEnvironment.translation.value =
                        chartEnvironment.translation.value.run {
                            copy(
                                x + delta * chartEnvironment.scale.value.x.pow(
                                    0.01f
                                ) * 100
                            )
                        }
                }
            }
        }
        false
    }.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consumeAllChanges()
            chartEnvironment.translation.value =
                chartEnvironment.translation.value.run { copy(x + dragAmount.x/*, y + dragAmount.y*/) }
        }
    }.pointerMoveFilter(onExit = {
        chartEnvironment.mousePosition.value = null
        false
    }, onMove = {
        chartEnvironment.mousePosition.value = it
        false
    })

private operator fun Offset.div(other: IntSize): Offset =
    Offset(x / other.width, y / other.height)
