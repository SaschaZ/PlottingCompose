@file:OptIn(ExperimentalComposeUiApi::class)
@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED", "MemberVisibilityCanBePrivate")

package dev.zieger.plottingcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
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
import dev.zieger.utils.time.ITimeSpan
import dev.zieger.utils.time.TimeStamp
import dev.zieger.utils.time.millis
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlin.math.pow


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
    val states = remember { States(scope) }
    Canvas(modifier
        .fillMaxSize()
        .onSizeChanged { chartEnvironment.chartSize.value = it }
        .mouseScrollFilter { event, _ ->
            ((event.delta as? MouseScrollUnit.Line)?.value
                ?: (event.delta as? MouseScrollUnit.Page)?.value)?.let { delta ->
                when (event.orientation) {
                    MouseScrollOrientation.Vertical -> {
                        val prevScaleX = chartEnvironment.scale.value.x
                        chartEnvironment.scale.value =
                            (chartEnvironment.scale.value.x.let { it - it * 0.025f * delta }).coerceAtLeast(0.1f) to 1f
                        val scaleXDiffRel = chartEnvironment.scale.value.x / prevScaleX
                        chartEnvironment.translation.run {
                            val diff = states.focusedItemIdx.value?.run {
                                itemX - itemX / scaleXDiffRel
                            } ?: 0f
                            value = value.copy(value.x / scaleXDiffRel + diff)
                        }

                        chartEnvironment.mousePosition.value?.also { mp ->
                            chartEnvironment.scaleCenter.value = mp
                        }
                    }
                    MouseScrollOrientation.Horizontal -> {
                        chartEnvironment.translation.value =
                            chartEnvironment.translation.value.run {
                                copy(
                                    x + delta * chartEnvironment.scale.value.x.pow(
                                        0.01f
                                    ) * 20
                                )
                            }
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
        ChartDrawScope(
            definition, this@Canvas, ArrayList(scopes),
            chartEnvironment
        ).draw(states)
    }
}

private operator fun Offset.div(value: Pair<Float, Float>): Offset = copy(x / value.x, y / value.y)

fun <T : Input> IChartDrawScope<T>.draw(states: States) {
    drawRect(definition.backgroundColor, rootRect)
    definition.charts.forEach { chart ->
        PlotDrawScope(chart, this, states).draw()
    }
}

fun <T : Input> IPlotDrawScope<T>.draw() {
    drawRect(chart.backgroundColor, plotBorderRect)
    drawRect(chart.borderColor, plotBorderRect, Stroke(1f))

    clipRect(yLabelRect) {
        scale(1f to finalScale.y, scaleCenter.value.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                yTicks.forEach { (value, label) ->
                    val y = yLabelRect.bottom - value / heightDivisor.value.toFloat() + yLabelHeight / 3
                    drawText(label, Offset(yLabelRect.left, y), 20f, chart.tickLabelColor)
                }
            }
        }
    }
    clipRect(plotBorderRect) {
        scale(1f to finalScale.y, scaleCenter.value.copy(x = 0f)) {
            translate(finalTranslation.copy(x = 0f)) {
                yTicks.forEach { (value, _) ->
                    val y = yLabelRect.bottom - value / heightDivisor.value.toFloat()
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
                    val y = yLabelRect.bottom - value / heightDivisor.value.toFloat()
                    drawLine(
                        chart.tickColor, Offset(yTickRect.left, y),
                        Offset(yTickRect.right, y), 1f
                    )
                }
            }
        }
    }

    clipRect(xLabelRect) {
        translate(finalTranslation.copy(y = 0f)) {
            xTicks.forEach { (value, label) ->
                val x = xLabelRect.left + value / widthDivisor
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
                val x = xLabelRect.left + value / widthDivisor
                drawLine(
                    chart.gridColor, Offset(x, plotBorderRect.top),
                    Offset(x, plotBorderRect.bottom), 1f
                )
                if (idx > 0) {
                    val (v, _) = xTicks.entries.toList()[idx - 1]
                    val step = (value - v) / 4
                    (1..3).map {
                        v + it * step
                    }.map { xLabelRect.left + it / widthDivisor }
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
    clipRect(xTickRect) {
        translate(finalTranslation.copy(y = 0f)) {
            xTicks.forEach { (value, _) ->
                val x = xLabelRect.left + value / widthDivisor
                drawLine(
                    chart.tickColor, Offset(x, xTickRect.top),
                    Offset(x, xTickRect.bottom), 1f
                )
            }
        }
    }

    clipRect(plotRect) {
        translate(finalTranslation) {
            chart.plots.forEach { style ->
                style.run {
                    drawSeries(chartData)
                }
            }
        }
    }

    translationOffsetY.value = visibleYPixelRange.start.toDouble()
    translationOffset.value = Offset(translationOffset.value.x, translationOffsetY.value.toFloat())

    println(
        "visibleXPixelRange=$visibleXPixelRange; visibleXValueRange=$xValueRange\n" +
                "visibleYPixelRange=$visibleYPixelRange; visibleYValueRange=$yValueRange\n" +
                "translationOffset=${translationOffset.value}; finalTranslation=$finalTranslation\n" +
                "scaleCenter=${scaleCenter.value}; scale=${scale.value}\n" +
                "heightDivisor=${heightDivisor.value.toFloat()}; widthDivisor=$widthDivisor"
    )
}

open class DoubleAnimator(
    initial: Double,
    private val scope: CoroutineScope,
    protected val duration: ITimeSpan = 300.millis,
    private val interpolatorFactors: (ClosedRange<Double>) -> Interpolator = { LinearInterpolator(it) },
    private val block: AnimationScope.(Double) -> Unit
) {

    private var currentValue: Double = initial
    private var currentTaretValue: Double = initial
    private var currentAnimation: Job? = null

    fun animateTo(value: Double) {
        if (value == currentTaretValue) return

        currentTaretValue = value
        currentAnimation?.cancel()
        currentAnimation = scope.launch {
            val startedAt = TimeStamp()
            val interpolator = interpolatorFactors(currentValue..value)
            block(AnimationScope(0.0, duration, 0.millis), interpolator.interpolate(0.0))
            do {
                delay(33)
                val runtime = TimeStamp() - startedAt
                val relT = (runtime / duration).coerceIn(0.0..1.0)
                currentValue = interpolator.interpolate(relT)
                block(AnimationScope(relT, duration, runtime), currentValue)
            } while (runtime < duration && isActive)
        }
    }

    data class AnimationScope(val relT: Double, val duration: ITimeSpan, val runtime: ITimeSpan)
}

fun mutableDoubleStateAnimated(
    initial: Double,
    scope: CoroutineScope,
    duration: ITimeSpan = 600.millis,
    interpolator: (ClosedRange<Double>) -> Interpolator = { LinearInterpolator(it) }
): MutableState<Double> = object : MutableState<Double> {

    private val internal = mutableStateOf(initial)
    private val animator = DoubleAnimator(initial, scope, duration, interpolator) { internal.value = it }

    override var value: Double
        get() = internal.value
        set(value) = animator.animateTo(value)

    override fun component1(): Double = value
    override fun component2(): (Double) -> Unit = { value = it }
}

abstract class Interpolator(protected val valueRange: ClosedRange<Double>) {
    abstract fun interpolate(relT: Double): Double
}

open class LinearInterpolator(valueRange: ClosedRange<Double>) : Interpolator(valueRange) {
    override fun interpolate(relT: Double): Double = valueRange.start + valueRange.range() * relT
}