package dev.zieger.plottingcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import dev.zieger.plottingcompose.scopes.*
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine
import kotlin.math.absoluteValue
import kotlin.math.sqrt

@Composable
fun MultiPlot(
    modifier: Modifier = Modifier,
    parameter: PlotParameter = PlotParameter(),
    colors: IPlotColors = PlotColors(),
    block: MultiPlotHandler.(PlotParameter) -> Unit
): MultiPlotHandler {
    val plots = remember { mutableListOf<MultiPlotItem>() }
    val handler = remember {
        MultiPlotHandler { item -> plots.add(item) }.apply { block(parameter) }
    }

    var plotHandler: List<PlotHandler> = emptyList()
    Column(modifier) {
        plotHandler = plots.map { (weight, params, col, handler) ->
            Plot(Modifier.weight(weight), params ?: parameter, col ?: colors, handler)
        }
    }
    plotHandler.syncPlots()

    return handler
}

@Composable
private fun List<PlotHandler>.syncPlots() {
    val scale = remember { mutableStateOf(1f) }
    forEach { it.scaleListener { s -> scale.value = s } }
    forEach { it.scale(scale.value) }

    val relativeScaleCenter = remember { mutableStateOf(Offset.Zero) }
    forEach { it.relativeScaleCenterListener { sC -> relativeScaleCenter.value = sC } }
    forEach { it.relativeScaleCenter(relativeScaleCenter.value) }

    val translation = remember { mutableStateOf(Offset.Zero) }
    forEach { it.translateListener { t -> translation.value = t } }
    forEach { it.translate(translation.value) }
}

data class MultiPlotHandler(
    private val plotInternal: (MultiPlotItem) -> Unit
) {
    fun plot(
        verticalWeight: Float = 1f,
        parameter: IParameter? = null,
        colors: IPlotColors? = null,
        block: PlotHandler.() -> Unit
    ) = plotInternal(MultiPlotItem(verticalWeight, parameter, colors, block))
}

data class MultiPlotItem(
    val verticalWeight: Float,
    val parameter: IParameter?,
    val colors: IPlotColors?,
    val handler: PlotHandler.() -> Unit
)

@Composable
fun Plot(
    modifier: Modifier = Modifier,
    parameter: IParameter = PlotParameter(),
    colors: IPlotColors = PlotColors(),
    block: PlotHandler.() -> Unit
): PlotHandler = Plot(modifier, parameter, colors, PlotScope(parameter), block)

@Composable
private fun Plot(
    modifier: Modifier,
    parameter: IParameter,
    colors: IPlotColors,
    plotScope: IPlotScope,
    block: PlotHandler.() -> Unit
): PlotHandler = PlotParameterScope(plotScope, parameter, colors).run {
    val allSeries = remember { mutableStateListOf<PlotSeries<*>>() }
    val handler = remember {
        PlotHandler({ series ->
            if (series !in allSeries || allSeries.size != 1) {
                allSeries.clear()
                allSeries.add(series)
                applyTranslationOffset.value = true
            }
        }, { series ->
            if (series !in allSeries) {
                allSeries.add(series)
                applyTranslationOffset.value = true
            }
        }, { scale.value = it },
            { listener -> scaleListener.add(listener) },
            {
                scaleCenter.value =
                    it.copy(it.x * plotSize.value.width.coerceAtLeast(1), it.y * plotSize.value.height.coerceAtLeast(1))
            },
            { listener ->
                scaleCenterListener.add {
                    listener(
                        it.copy(
                            it.x / plotSize.value.width.coerceAtLeast(1),
                            it.y / plotSize.value.height.coerceAtLeast(1)
                        )
                    )
                }
            },
            { translation.value = it },
            { listener -> translationListener.add(listener) },
            {
                scale.value = 1f
                translation.value = Offset.Zero
                scaleCenter.value = Offset.Zero
            }).apply(block)
    }

    Canvas(modifier.fillMaxSize().onSizeChanged {
        plotSize.value = it
        applyTranslationOffset.value = true
    }.plotInputs(this)) {
        PlotDrawScope(this@run, this, allSeries).draw()
    }

    handler
}

private operator fun IntSize.div(fl: Float): Offset {
    return Offset((width / fl), (height / fl))
}

fun SinglePlot.applyTranslationOffset(allSeries: SnapshotStateList<PlotSeries<*>>) = scope.run {
    val items = allSeries.flatMap { it.items }.filterNot { it.data.position.isYEmpty }
    if (items.isEmpty()) return

    val yTicks = plotYTicks(items)
    if (yTicks.isEmpty()) return
    val font = Font(null, plotLabelFontSize())
    val yWidth = yTicks.maxOf { TextLine.make(it.second, font).width }
    val xTicks = plotXTicks(items)
    if (xTicks.isEmpty()) return
    val xHeight = xTicks.maxOf { TextLine.make(it.second, font).height }

    val widthFactor = allSeries.xWidth / plot.height
    val heightFactor = allSeries.yHeight / plot.height

    translationOffset.value = Offset(-allSeries.xRange.start / widthFactor, allSeries.yRange.start / heightFactor)
    println("transOff=${translationOffset.value}; yRangeStart=${allSeries.yRange.start}; heightFactor=$heightFactor")
    applyTranslationOffset.value = false
}

fun IntSize.toFloat(): Size = Size(width.toFloat(), height.toFloat())

private fun IPlotDrawScope.draw() {
    if (allSeries.isEmpty()) return

    SinglePlot(this)?.apply {
        if (applyTranslationOffset.value)
            applyTranslationOffset(allSeries)
        drawPlotBackground()
        if (drawGrid) drawGrid()

        drawPlot(allSeries)

        if (drawChartBorder) drawPlotBorder()
        applyFocusedFlag(focusAxis)

        drawAxis()
    }
}

private fun SinglePlot.drawPlot(
    allSeries: List<PlotSeries<*>>
) = scope.run {
    clipRect(main) {
        translate(
            this@run.finalTranslation(this@run).x,
            this@run.finalTranslation(this@run).y
        ) {
            scale(
                this@run.scale.value,
                this@run.scaleCenter.value - this@run.translationOffset.value
            ) {
                this@run.run {
                    allSeries.flatMap { i -> i.styles.flatMap { s -> s.z.map { z -> Triple(i.items, s, z) } } }
                        .sortedBy { it.third }
                        .forEach { (items, style, z) ->
                            style.run { draw(items.map { it.data }, z, this@drawPlot) }
                        }
                }
            }
        }
    }
}

fun IPlotDrawScope.clipRect(rect: Rect, block: DrawScope.() -> Unit) = rect.run {
    clipRect(left, top, right, bottom, ClipOp.Intersect, block)
}

private fun SinglePlot.applyFocusedFlag(axis: Axis) {
    scope.allItems.map { it to it.data.toScene(this) }
        .sortedBy { (_, offset) ->
            scope.run { distanceToMouse(axis, offset.position.offset) }
        }.forEachIndexed { idx, (item, offset) ->
            item.data.hasFocus = idx == 0 && scope.run { distanceToMouse(axis, offset.position.offset) } < 100f
        }
}

internal fun IPlotParameterScope.mappedOffset(offset: Offset): Offset =
    try {
        scaleCenter.value + (offset - scaleCenter.value - finalTranslation(this) + translationOffset.value) / scale.value - translationOffset.value
    } catch (t: Throwable) {
        Offset.Zero
    }

internal fun IPlotParameterScope.mappedMousePosition(): Offset =
    mousePosition.value?.let { mappedOffset(it) } ?: Offset.Infinite

private fun IPlotDrawScope.distanceToMouse(axis: Axis, a: Offset): Float {
    return mousePosition.value?.let {
        when (axis) {
            Axis.X -> (a.x - mappedMousePosition().x).absoluteValue
            Axis.Y -> (a.y - mappedMousePosition().y).absoluteValue
            Axis.BOTH -> (a - mappedMousePosition()).length
        }
    } ?: Float.MAX_VALUE
}

private operator fun Offset.times(offset: Offset): Offset {
    return Offset(x * offset.x, y * offset.y)
}

private operator fun Offset.times(size: IntSize): Offset = Offset(x * size.width, y * size.height)

private operator fun Offset.div(offset: Offset): Offset = Offset(x / offset.x, y / offset.y)

private val Offset.length: Float get() = sqrt(x * x + y * y)

private fun SinglePlot.drawGrid() = scope.run {
    clipRect(main) {
        translate(this@run.finalTranslation(scope).x, this@run.finalTranslation(scope).y) {
            scale(this@run.scale.value, this@run.scaleCenter.value - this@run.translationOffset.value) {
                this@run.run {
                    xTicks.flatMapIndexed { idx, (x, _) ->
                        listOf(
                            x.toFloat(),
                            x.toFloat() + ((xTicks.getOrNull(idx + 1)?.first?.toFloat()
                                ?: x.toFloat()) - x.toFloat()) / 2
                        )
                    }
                        .map { it * this@drawGrid.widthFactor * widthFactor.value + horizontalPadding().value + horizontalPlotPadding().value }
                        .forEach { x ->
                            drawLine(
                                grid,
                                Offset(x, -plot.height * 100),
                                Offset(x, plot.height * 100),
                                gridStrokeWidth(),
                                alpha = grid.alpha
                            )
                        }

                    yTicks.map { plotSize.value.height - it.first.toFloat() * this@drawGrid.heightFactor - verticalPadding().value - verticalPlotPadding().value - xLabel.height }
                        .forEach { y ->
                            drawLine(
                                grid,
                                Offset(
                                    -plot.width * 100 * widthFactor.value,
                                    y
                                ),
                                Offset(
                                    plot.width * 100 * widthFactor.value,
                                    y
                                ),
                                gridStrokeWidth(),
                                alpha = grid.alpha
                            )
                        }
                }
            }
        }
    }
}

private fun SinglePlot.drawPlotBackground() = scope.run {
    drawRect(background, main.topLeft, main.size, background.alpha, Fill)
}

private fun SinglePlot.drawPlotBorder() = scope.run {
    drawRect(border, main.topLeft, main.size, border.alpha, Stroke(1f))
}

private fun SinglePlot.drawAxis() {
    drawYAxis()
    drawXAxis()
}

private fun SinglePlot.drawYAxis() = scope.run {
    clipRect(yLabel) {
        translate(0f, this@run.finalTranslation(scope).y) {
            scale(
                1f,
                this@run.scale.value,
                this@run.scaleCenter.value - this@run.translationOffset.value
            ) {
                this@run.run run2@{
                    val x =
                        plotSize.value.width - horizontalPadding().value - yLabel.width + plotTickLength().value / 2
                    yTicks.forEach { (y, str) ->
                        val yScene = plotSize.value.height - y.toFloat() * heightFactor -
                                verticalPadding().value - verticalPlotPadding().value -
                                xLabel.height

                        if (drawYTicks)
                            drawLine(
                                axisTicks,
                                Offset((x - plotTickLength().value / 2), yScene),
                                Offset((x + plotTickLength().value / 2), yScene)
                            )

                        if (drawYLabels) {
                            val offset = Offset(
                                (x + plotTickLength().value),
                                yScene + plotLabelFontSize() / 3 / scale.value
                            )
                            scale(1 / scale.value, 1 / scale.value, offset) {
                                this@run2.run {
                                    drawText(
                                        str,
                                        offset,
                                        plotLabelFontSize(),
                                        axisLabels,
                                        scale.value
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun SinglePlot.drawXAxis() = scope.run {
    clipRect(xTopTicks) {
        translate(this@run.finalTranslation(scope).x, 0f) {
            scale(
                this@run.scale.value,
                1f,
                this@run.scaleCenter.value - this@run.translationOffset.value
            ) {
                this@run.run run2@{
                    val y = xTopTicks.top + plotTickLength().value / 2
                    plotXTicks(allItems).forEach { (x, str) ->
                        val xScene =
                            x.toFloat() * this@drawXAxis.widthFactor * widthFactor.value + xLabel.left + horizontalPlotPadding().value
                        if (drawXTicks)
                            drawLine(
                                axisTicks,
                                Offset(xScene, verticalPlotPadding().value - plotTickLength().value / 2f),
                                Offset(xScene, verticalPlotPadding().value + plotTickLength().value / 2f)
                            )
                    }
                }
            }
        }
    }

    clipRect(xLabel) {
        translate(this@run.finalTranslation(scope).x, 0f) {
            scale(
                this@run.scale.value,
                1f,
                this@run.scaleCenter.value - this@run.translationOffset.value
            ) {
                this@run.run run2@{
                    val y = xLabel.top + plotTickLength().value / 2
                    plotXTicks(allItems).forEach { (x, str) ->
                        val xScene =
                            x.toFloat() * this@drawXAxis.widthFactor * widthFactor.value + xLabel.left + horizontalPlotPadding().value
                        if (drawXTicks)
                            drawLine(
                                axisTicks,
                                Offset(xScene, y - plotTickLength().value / 2f),
                                Offset(xScene, y + plotTickLength().value / 2f)
                            )

                        if (drawXLabels) {
                            val textLine = TextLine.make(str, Font(null, plotLabelFontSize()))
                            val offset = Offset(
                                xScene - textLine.width / 2 / scale.value,
                                (y + plotTickLength().value + textLine.height)
                            )
                            scale(1 / scale.value, 1f, offset) {
                                run {
                                    drawText(
                                        str,
                                        offset,
                                        this@run2.run { plotLabelFontSize() },
                                        this@run2.axisLabels
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun DrawScope.drawText(
    text: String,
    offset: Offset,
    size: Float,
    color: Color,
    scale: Float = 1f,
    align: TextAlign = TextAlign.Start
) {
    drawIntoCanvas {
        val font = Font(null, size, scale, 0f)
        val textLine = TextLine.make(text, font)
        val off = when (align) {
            TextAlign.Center, TextAlign.Justify -> offset - Offset(textLine.width / 2f, -textLine.height / 2f)
            TextAlign.End, TextAlign.Right -> offset + Offset(textLine.width / 2f, -textLine.height / 2f)
            else -> offset
        }
        it.nativeCanvas.drawString(
            text, off.x, off.y, font, Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = color.toArgb()
            }
        )
    }
}