@file:OptIn(ExperimentalComposeUiApi::class) @file:Suppress(
    "EXPERIMENTAL_IS_NOT_ENABLED",
    "MemberVisibilityCanBePrivate",
    "LeakingThis",
    "FunctionName"
)

package dev.zieger.plottingcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import dev.zieger.exchange.dto.DataSource
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.definition.ChartDefinition
import dev.zieger.plottingcompose.di.*
import dev.zieger.utils.time.TimeFormat
import dev.zieger.utils.time.toTime
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope
import java.text.DecimalFormat

data class InputContainer<out I : Input>(val input: I, val idx: Long)

@Composable
fun <I : Input> MultiChart(
    definition: ChartDefinition<in I>, candleSource: DataSource<in I>, modifier: Modifier = Modifier
) {
    initDi(definition, candleSource)

    val scopes = remember {
        koin.get<ChartDefinition<*>>(CHART_DEFINITION).charts.map { chart ->
            koin.createScope(chart)
        }
    }
    Canvas(
        modifier.fillMaxSize().let { koin.get<TransformationHolder>().buildModifier(it) }
    ) {
        koin.get<GlobalScope>(parameters = { parametersOf(this@Canvas) }).draw(scopes)
    }

    remember {
        koin.get<ProcessingController>().control()
    }
}

private operator fun Offset.div(value: Pair<Float, Float>): Offset = copy(x / value.x, y / value.y)

fun GlobalScope.draw(scopes: List<Scope>) {
    drawRect(definition.backgroundColor, rootRect)

    scopes.forEach {
        it.get<ChartScope>(parameters = { parametersOf(this@draw) }).draw()
    }
}

fun ChartScope.draw() {
    drawRect(chart.backgroundColor, rootBorderRect)
    drawRect(chart.borderColor, rootBorderRect, Stroke(1f))

    drawHorizontalGrid()
    drawVerticalGrid()

    if (chart.drawYLabels) {
        drawYLabels()
        drawHoverYLabel()
    }
    if (chart.drawXLabels) {
        drawXLabels()
        drawHoverXLabel()
    }

    drawYTicks()
    drawXTicks(x1TickRect)
    drawXTicks(x2TickRect)

    drawPlot()
}

private fun ChartScope.drawPlot() {
    clipRect(plotRect) {
        translate(finalTranslation) {
            chart.plots.forEach { style ->
                style.run {
                    drawSeries(chartData.map { (k, v) -> k to v }.sortedBy { (k, _) -> k.input.x.toDouble() })
                }
            }
        }
    }
}

private fun ChartScope.drawXTicks(rect: Rect) {
    clipRect(rect) {
        translate(finalTranslation.copy(y = 0f)) {
            xTicks.value.ticks.forEach { (value, _) ->
                val x = xLabelRect.left + value.toFloat() / finalScale.x
                drawLine(
                    chart.tickColor, Offset(x.toFloat(), chartRect.top), Offset(x.toFloat(), chartRect.bottom), 1f
                )
            }
        }
    }
}

private fun ChartScope.drawVerticalGrid() {
    clipRect(rootBorderRect) {
        translate(finalTranslation.copy(y = 0f)) {
            xTicks.value.ticks.entries.toList().forEachIndexed { idx, (value, _) ->
                val x = xLabelRect.left + (value + xTicks.value.originIdx).toFloat() / finalScale.x
                drawLine(
                    chart.gridColor,
                    Offset(x.toFloat(), rootBorderRect.top),
                    Offset(x.toFloat(), rootBorderRect.bottom),
                    1f
                )
                if (idx > 0) {
                    val (v, _) = xTicks.value.ticks.entries.toList()[idx - 1]
                    val step = (value - v) / 4
                    (1..3).map {
                        v + it * step
                    }.map { (xLabelRect.left + (it + xTicks.value.originIdx) / finalScale.x).toFloat() }.forEach { x1 ->
                        drawLine(
                            chart.gridColor, Offset(x1, rootBorderRect.top), Offset(x1, rootBorderRect.bottom), 1f
                        )
                    }
                }
            }
        }
    }
}

private fun ChartScope.drawXLabels() {
    clipRect(xLabelRect) {
        translate(finalTranslation.copy(y = 0f)) {
            xTicks.value.ticks.forEach { (value, lines) ->
                val x = (xLabelRect.left + (value + xTicks.value.originIdx) / finalScale.x).toFloat()
                lines.reversed().forEachIndexed { idx, label ->
                    val y = xLabelRect.bottom - idx * xLabelFontSize / lines.size
                    drawText(
                        label, Offset(x, y), xLabelFontSize, chart.tickLabelColor, 1f, TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun ChartScope.drawHoverXLabel() {
    clipRect(xLabelRect) {
        translate(finalTranslation.copy(y = 0f)) {
            focusedItem.value?.let { (idx, _) ->
                val y = xLabelRect.top + 23
                val x = (xLabelRect.left + idx / finalScale.x).toFloat()
                val time = chartData.keys.first { it.idx == idx }.input.x.toDouble().toTime()
                    .formatTime(TimeFormat.CUSTOM("dd-MM-yy HH:mm"))
                val rectWidth = time.size(xLabelFontSize + 4f).width * 1.05f
                drawRoundRect(
                    Color.White, Offset(x - rectWidth / 2, y - 22.5f), Size(rectWidth, 26f), CornerRadius(5f, 5f), Fill
                )
                drawText(
                    time, Offset(x, y), xLabelFontSize + 4f, Color.Black, 1f, TextAlign.Center
                )
            }
        }
    }
}

private fun ChartScope.drawYTicks() {
    clipRect(yTicksRect) {
        scale(finalScale.copy(x = 1f), scaleCenter.value.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                yTicks.value.ticks.forEach { (value, _) ->
                    val y = yLabelRect.bottom - value.toFloat() / finalScale.y
                    drawLine(
                        chart.tickColor, Offset(yTicksRect.left, y), Offset(yTicksRect.right, y), 1f
                    )
                }
            }
        }
    }
}

private fun ChartScope.drawHorizontalGrid() {
    clipRect(rootBorderRect) {
        scale(finalScale.copy(x = 1f), scaleCenter.value.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                yTicks.value.ticks.forEach { (value, _) ->
                    val y = yLabelRect.bottom - value.toFloat() / finalScale.y
                    drawLine(
                        chart.gridColor, Offset(rootBorderRect.left, y), Offset(rootBorderRect.right, y), 1f
                    )
                }
            }
        }
    }
}

private fun ChartScope.drawYLabels() {
    clipRect(yLabelRect) {
        scale(finalScale.copy(x = 1f), scaleCenter.value.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                yTicks.value.ticks.forEach { (value, labels) ->
                    labels.forEachIndexed { _, label ->
                        val y =
                            yLabelRect.bottom - value.toFloat() / finalScale.y + yLabelFontSize - chart.margin.bottom(
                                chartSize.value
                            ).value
                        drawText(label, Offset(yLabelRect.left, y), 20f, chart.tickLabelColor)
                    }
                }
            }
        }
    }
}

private fun ChartScope.drawHoverYLabel() {
    clipRect(yLabelRect) {
        scale(finalScale.copy(x = 1f), scaleCenter.value.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                focusedItem.value?.let { (idx, _) ->
                    val yValue = chartData.entries.first { (key, _) -> key.idx == idx }.value.map { (key, portValues) ->
                        key to portValues.filter {
                            chart.slot(
                                key,
                                it.port
                            )?.scale != null || it.port.includeIntoScaling
                        }
                    }.flatMap { it.second }
                        .run { sumOf { it.value.yRange.run { start + (endInclusive - start) / 2 } }.toFloat() / size }

                    val y = ((yLabelRect.bottom - yValue / finalScale.y) - 13f).toFloat()

//                    println("x=$x; y=$y; yValue=$yValue")
                    val text = DecimalFormat("##,###.###").format(yValue)
                    val rectWidth = text.size(20f).width * 1.05f
                    val x = yLabelRect.left + rectWidth / 2
                    drawRoundRect(
                        Color.White, Offset((x - rectWidth / 2), y), Size(rectWidth, 26f), CornerRadius(5f, 5f), Fill
                    )
                    drawText(
                        text, Offset(x, y + xLabelFontSize), 20f, Color.Black, 1f, TextAlign.Center
                    )
                }
            }
        }
    }
}

operator fun Offset.div(other: IntSize): Offset = Offset(x / other.width, y / other.height)

fun String.size(fontSize: Float): TextLine {
    val font = Font(null, fontSize)
    return TextLine.make(this, font)
}

fun <T, C : Collection<T>> C.nullWhenEmpty(): C? = ifEmpty { null }