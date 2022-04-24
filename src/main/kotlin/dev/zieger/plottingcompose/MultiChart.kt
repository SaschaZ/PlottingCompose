@file:OptIn(ExperimentalComposeUiApi::class)
@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED", "MemberVisibilityCanBePrivate", "LeakingThis", "FunctionName")

package dev.zieger.plottingcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
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
import dev.zieger.utils.time.TimeFormat
import dev.zieger.utils.time.toTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DecimalFormat

data class InputContainer<I : Input>(val input: I, val idx: Long)

@Composable
fun <T : Input> MultiChart(
    definition: ChartDefinition<T>,
    input: State<Flow<T>>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scopes = remember { mutableStateListOf<Pair<Long, ProcessingScope<T>>>() }
    var collectJob by remember { mutableStateOf<Job?>(null) }
    var collectFlow by remember { mutableStateOf<Flow<T>?>(null) }

    val globalChartEnvironment = remember { GlobalChartEnvironment() }
    val chartEnvironment = remember { definition.charts.associateWith { ChartEnvironment(globalChartEnvironment) } }

    fun Flow<T>.collect(): Job = scope.launch {
        chartEnvironment.values.forEach { it.reset() }
        scopes.clear()
        collectFlow = this@collect

        var lastX: Double? = null
        var lastIdx = 0L
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

    if (input.value != collectFlow) {
        collectJob?.cancel()
        collectJob = input.value.collect()
    }

    Canvas(
        modifier
            .fillMaxSize()
            .fillEnvironment(globalChartEnvironment)
    ) {
        ChartDrawScope(
            definition,
            this@Canvas,
            ArrayList(scopes).toMap(),
            chartEnvironment
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
                PlotDrawScope(chart, this, this@run, rect).draw()
            }
            top += height
        }
    }
}

fun <T : Input> IPlotDrawScope<T>.draw() {
    drawRect(chart.backgroundColor, plotBorderRect)
    drawRect(chart.borderColor, plotBorderRect, Stroke(1f))

    if (chart.drawYLabels) {
        drawYLabels()
        drawHoverYLabel()
    }
    drawHorizontalGrid()
    drawYTicks()

    if (chart.drawXLabels) {
        drawXLabels()
        drawHoverXLabel()
    }
    drawVerticalGrid()

    drawXTicks(x1TickRect)
    drawXTicks(x2TickRect)

    drawPlot()
}

private fun <T : Input> IPlotDrawScope<T>.drawPlot() {
    clipRect(plotRect) {
        translate(finalTranslation) {
            chart.plots.forEach { style ->
                style.run {
                    drawSeries(chartData
                        .map { (k, v) -> k to v }
                        .sortedBy { (k, _) -> k.input.x.toDouble() })
                }
            }
        }
    }
}

private fun <T : Input> IPlotDrawScope<T>.drawXTicks(rect: Rect) {
    clipRect(rect) {
        translate(finalTranslation.copy(y = 0f)) {
            xTicks.ticks.forEach { (value, _) ->
                val x = xLabelRect.left + value.toFloat() / widthDivisor
                drawLine(
                    chart.tickColor, Offset(x.toFloat(), rect.top),
                    Offset(x.toFloat(), rect.bottom), 1f
                )
            }
        }
    }
}

private fun <T : Input> IPlotDrawScope<T>.drawVerticalGrid() {
    clipRect(plotBorderRect) {
        translate(finalTranslation.copy(y = 0f)) {
            xTicks.ticks.entries.toList().forEachIndexed { idx, (value, _) ->
                val x = xLabelRect.left + (value + xTicks.originIdx).toFloat() / widthDivisor
                drawLine(
                    chart.gridColor, Offset(x.toFloat(), plotBorderRect.top),
                    Offset(x.toFloat(), plotBorderRect.bottom), 1f
                )
                if (idx > 0) {
                    val (v, _) = xTicks.ticks.entries.toList()[idx - 1]
                    val step = (value - v) / 4
                    (1..3).map {
                        v + it * step
                    }.map { (xLabelRect.left + (it + xTicks.originIdx) / widthDivisor).toFloat() }
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
}

private fun <T : Input> IPlotDrawScope<T>.drawXLabels() {
    clipRect(xLabelRect) {
        translate(finalTranslation.copy(y = 0f)) {
            xTicks.ticks.forEach { (value, lines) ->
                val x = (xLabelRect.left + (value + xTicks.originIdx) / widthDivisor).toFloat()
                lines.reversed().forEachIndexed { idx, label ->
                    val y = xLabelRect.bottom - idx * xLabelHeight.value.toFloat() / lines.size
                    drawText(
                        label,
                        Offset(x, y),
                        xLabelFontSize,
                        chart.tickLabelColor,
                        1f,
                        TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun <T : Input> IPlotDrawScope<T>.drawHoverXLabel() {
    clipRect(xLabelRect) {
        translate(finalTranslation.copy(y = 0f)) {
            focusedItemIdx.value?.let { (idx, _) ->
                val y = xLabelRect.top + 23
                val x = (xLabelRect.left + idx / widthDivisor).toFloat()
                val time = chartData.keys.first { it.idx == idx }.input.x.toDouble().toTime()
                    .formatTime(TimeFormat.CUSTOM("dd-MM-yy HH:mm"))
                val rectWidth = time.size(xLabelFontSize + 4f).width * 1.05f
                drawRoundRect(
                    Color.White, Offset(x - rectWidth / 2, y - 22.5f), Size(rectWidth, 26f),
                    CornerRadius(5f, 5f), Fill
                )
                drawText(
                    time,
                    Offset(x, y),
                    xLabelFontSize + 4f,
                    Color.Black,
                    1f,
                    TextAlign.Center
                )
            }
        }
    }
}

private fun <T : Input> IPlotDrawScope<T>.drawYTicks() {
    clipRect(yTickRect) {
        scale(1f to finalScale.y.toFloat(), scaleCenter.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                yTicks.ticks.forEach { (value, _) ->
                    val y = yLabelRect.bottom - value.toFloat() / heightDivisor.value.toFloat()
                    drawLine(
                        chart.tickColor, Offset(yTickRect.left, y),
                        Offset(yTickRect.right, y), 1f
                    )
                }
            }
        }
    }
}

private fun <T : Input> IPlotDrawScope<T>.drawHorizontalGrid() {
    clipRect(plotBorderRect) {
        scale(1f to finalScale.y.toFloat(), scaleCenter.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                yTicks.ticks.forEach { (value, _) ->
                    val y = yLabelRect.bottom - value.toFloat() / heightDivisor.value.toFloat()
                    drawLine(
                        chart.gridColor, Offset(plotBorderRect.left, y),
                        Offset(plotBorderRect.right, y), 1f
                    )
                }
            }
        }
    }
}

private fun <T : Input> IPlotDrawScope<T>.drawYLabels() {
    clipRect(yLabelRect) {
        scale(1f to finalScale.y.toFloat(), scaleCenter.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                yTicks.ticks.forEach { (value, labels) ->
                    labels.forEachIndexed { idx, label ->
                        val y = yLabelRect.bottom -
                                value.toFloat() / heightDivisor.value.toFloat() +
                                yLabelHeight -
                                chart.margin.bottom(chartSize.value).value
                        drawText(label, Offset(yLabelRect.left, y), 20f, chart.tickLabelColor)
                    }
                }
            }
        }
    }
}

private fun <T : Input> IPlotDrawScope<T>.drawHoverYLabel() {
    clipRect(yLabelRect) {
        scale(1f to finalScale.y.toFloat(), scaleCenter.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                focusedItemIdx.value?.let { (idx, _) ->
                    val yValue = chartData.entries.first { (key, _) -> key.idx == idx }
                        .value.map { (key, portValues) ->
                            key to portValues
                                .filter { chart.slot(key, it.port)?.scale != null || it.port.includeIntoScaling }
                        }
                        .flatMap { it.second }
                        .run { sumOf { it.value.yRange.run { start + range() / 2 } }.toFloat() / size }

                    val y = ((yLabelRect.bottom - yValue / heightDivisor.value) - 13f).toFloat()

//                    println("x=$x; y=$y; yValue=$yValue")
                    val text = DecimalFormat("##,###.###").format(yValue)
                    val rectWidth = text.size(20f).width * 1.05f
                    val x = yLabelRect.left + rectWidth / 2
                    drawRoundRect(
                        Color.White, Offset((x - rectWidth / 2), y),
                        Size(rectWidth, 26f),
                        CornerRadius(5f, 5f), Fill
                    )
                    drawText(
                        text,
                        Offset(x, y + xLabelFontSize),
                        20f,
                        Color.Black,
                        1f,
                        TextAlign.Center
                    )
                }
            }
        }
    }
}

fun Modifier.fillEnvironment(
    chartEnvironment: GlobalChartEnvironment
): Modifier = onSizeChanged { chartEnvironment.chartSize.value = it }
    .mouseScrollFilter { event, _ ->
        ((event.delta as? MouseScrollUnit.Line)?.value
            ?: (event.delta as? MouseScrollUnit.Page)?.value)?.let { delta ->
            when (event.orientation) {
                MouseScrollOrientation.Vertical -> chartEnvironment.processVerticalScrolling(delta)
                MouseScrollOrientation.Horizontal -> chartEnvironment.processHorizontalScroll(delta)
            }
        }
        false
    }.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consumeAllChanges()
            chartEnvironment.processDrag(dragAmount)
        }
    }.pointerMoveFilter(onExit = {
        chartEnvironment.mousePosition.value = null
        false
    }, onMove = {
        chartEnvironment.mousePosition.value = it
        false
    })

operator fun Offset.div(other: IntSize): Offset =
    Offset(x / other.width, y / other.height)
