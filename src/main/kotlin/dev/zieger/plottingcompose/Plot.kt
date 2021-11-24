package dev.zieger.plottingcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.mouse.MouseScrollOrientation
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import dev.zieger.plottingcompose.scopes.*
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine
import kotlin.math.absoluteValue
import kotlin.math.sqrt

@Composable
fun Plot(
    modifier: Modifier = Modifier,
    parameter: IParameter = PlotParameter(),
    colors: IPlotColors = PlotColors(),
    block: ItemScope.() -> Unit
): PlotHandler = Plot(modifier, parameter, colors, PlotScope(), block)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Plot(
    modifier: Modifier,
    parameter: IParameter,
    colors: IPlotColors,
    plotScope: IPlotScope,
    block: ItemScope.() -> Unit
): PlotHandler = PlotParameterScope(plotScope, parameter.withPlotScope(plotScope), colors).run {
    val allSeries = remember { mutableStateListOf<Series<*>>() }
    Canvas(modifier.fillMaxSize().onSizeChanged { plotSize.value = it }
        .mouseScrollFilter { event, _ ->
            when (event.orientation) {
                MouseScrollOrientation.Vertical -> ((event.delta as? MouseScrollUnit.Line)?.value
                    ?: (event.delta as? MouseScrollUnit.Page)?.value)?.also { delta ->
                    when (scrollAction) {
                        ScrollAction.X_TRANSLATION ->
                            translation.value = translation.value.let { it.copy(it.x + delta) }
                        ScrollAction.WIDTH_FACTOR -> {
                            widthFactor.value = (widthFactor.value + widthFactor.value * delta / 20).coerceAtLeast(1f)
                            mousePosition.value?.also { pos ->
                                val prev = widthFactorCenter.value
                                widthFactorCenter.value =
                                    (widthFactorCenter.value + (pos - widthFactorCenter.value) / widthFactor.value) - translation.value / widthFactor.value
                                println("mouse: $pos; widthFactorCenter=$prev -> ${widthFactorCenter.value}; factor=${widthFactor.value}")
                            }
                        }
                        ScrollAction.SCALE -> {
                            if (!enableScale) return@mouseScrollFilter false
                            val newScale = (scale.value + 1 / delta).coerceAtLeast(1f)
                            when {
                                newScale < scale.value -> {
                                    val diff = scale.value - newScale
                                    val percent = diff / (scale.value - 1f)
                                    translation.value = translation.value * (1f - percent)
                                }
                            }
                            scale.value = newScale
                            mousePosition.value?.also { pos ->
                                val prev = scaleCenter.value
                                scaleCenter.value =
                                    (scaleCenter.value + (pos - scaleCenter.value) / scale.value) - translation.value / scale.value
                                println("mouse: $pos; scaleCenter=$prev -> ${scaleCenter.value}; scale=${scale.value}")
                            }
                        }
                    }
                }
                MouseScrollOrientation.Horizontal -> ((event.delta as? MouseScrollUnit.Line)?.value
                    ?: (event.delta as? MouseScrollUnit.Page)?.value)?.also { delta ->
                    println("horizontal delta=$delta")
                    translation.value = translation.value.let { it.copy(it.x + delta) }
                }
            }
            true
        }
        .pointerInput(Unit) {
            detectDragGestures { _, dragAmount ->
                if (!enableTranslation) return@detectDragGestures
                translation.value = (translation.value + dragAmount)
            }
        }
        .pointerMoveFilter(onMove = {
            mousePosition.value = it
            false
        }, onExit = {
            mousePosition.value = null
            false
        })
    ) {
        PlotDrawScope(this@run, this, allSeries).draw()
    }

    ItemScope({ series ->
        if (series !in allSeries || allSeries.size != 1) {
            allSeries.clear()
            allSeries.add(series)
        }
    }, { series ->
        if (series !in allSeries) {
            allSeries.add(series)
        }
    }).block()
    applyTranslationOffset(allSeries)

    PlotHandler({ scale.value = it }, { translation.value = it },
        {
            scale.value = 1f
            translation.value = Offset.Zero
            scaleCenter.value = Offset.Zero
        })
}

private operator fun IntSize.div(fl: Float): Offset {
    return Offset((width / fl), (height / fl))
}

fun IPlotParameterScope.applyTranslationOffset(allSeries: SnapshotStateList<Series<*>>) {
    val items = allSeries.flatMap { it.items }
    if (items.isEmpty()) return

    val yTicks = plotYTicks(items)
    if (yTicks.isEmpty()) return
    val font = Font(null, plotLabelFontSize)
    val yWidth = yTicks.maxOf { TextLine.make(it.second, font).width }
    val xTicks = plotXTicks(items)
    if (xTicks.isEmpty()) return
    val xHeight = xTicks.maxOf { TextLine.make(it.second, font).height }


    val plotWidth =
        plotSize.value.width - horizontalPadding.value * 2 - horizontalPlotPadding.value * 2 - yWidth
    val plotHeight =
        plotSize.value.height - verticalPadding.value * 2 - verticalPlotPadding.value * 2 - xHeight

    val widthFactor = allSeries.xWidth / plotWidth
    val heightFactor = allSeries.yHeight / plotHeight

    translationOffset.value = Offset(-allSeries.xRange.start / widthFactor, allSeries.yRange.start / heightFactor)
}

fun IntSize.toFloat(): Size = Size(width.toFloat(), height.toFloat())

private fun IPlotDrawScope.draw() {
    if (allSeries.isEmpty()) return

    val yTicks = plotYTicks(allItems)
    if (yTicks.isEmpty()) return
    val font = Font(null, plotLabelFontSize)
    val yWidth = yTicks.maxOf { TextLine.make(it.second, font).width }
    val xTicks = plotXTicks(allItems)
    if (xTicks.isEmpty()) return
    val xHeight = xTicks.maxOf { TextLine.make(it.second, font).height }

    drawPlotBackground(yWidth, xHeight)
    if (drawGrid) drawGrid(yWidth, xHeight)

    val offsets = allSeries.flatMap { drawPlot(it, yWidth, xHeight)?.entries ?: emptyList() }
        .associate { it.key to it.value }

    if (drawChartBorder) drawPlotBorder(yWidth, xHeight)
    applyFocusedFlag(focusAxis, offsets)

    drawAxis(yTicks, yWidth, xTicks, xHeight)
}

private fun IPlotDrawScope.drawPlot(
    series: Series<*>,
    yWidth: Float, xHeight: Float
): Map<SeriesItem<*>, Offset>? {
    val plotRect = DpRect(
        horizontalPadding + horizontalPlotPadding,
        verticalPadding + verticalPlotPadding,
        plotSize.value.width.dp - horizontalPadding - horizontalPlotPadding - yWidth.dp,
        plotSize.value.height.dp - verticalPadding - verticalPlotPadding - xHeight.dp
    )

    val widthFactor = plotRect.width / allSeries.xWidth
    val heightFactor = plotRect.height / allSeries.yHeight

    var offsets: Map<SeriesItem<*>, Offset> = emptyMap()
    val start = horizontalPadding.value
    val top = verticalPadding.value
    val end = plotSize.value.width.toFloat() - horizontalPadding.value - yWidth
    val bottom = plotSize.value.height.toFloat() - verticalPadding.value - xHeight
    if (start > end || top > bottom) return null

    clipRect(start, top, end, bottom) {
        translate(
            this@drawPlot.finalTranslation.x,// - (this@drawPlot.widthFactorCenter.value.x - this@drawPlot.horizontalPadding.value * 1.3f) * this@drawPlot.widthFactor.value + (this@drawPlot.widthFactorCenter.value.x - this@drawPlot.horizontalPadding.value * 1.5f),
            this@drawPlot.finalTranslation.y
        ) {
            scale(
                this@drawPlot.scale.value,
                this@drawPlot.scaleCenter.value - this@drawPlot.translationOffset.value
            ) {
                this@drawPlot.run {
                    val map: Offset.() -> Offset = {
                        Offset(
                            (horizontalPadding + horizontalPlotPadding + widthFactor * x).toPx(),
                            (verticalPadding + verticalPlotPadding + plotRect.height - heightFactor * y).toPx()
                        )
                    }
                    offsets = series.items.associateWith { item ->
                        Offset(item.x.toFloat(), item.y.value.toFloat()).map()
                    }

                    series.run { preDrawerDraw(offsets) }

                    series.z.sorted().flatMap { z ->
                        series.items.mapIndexed { idx, item ->
                            item.run {
                                draw(
                                    offsets[item]!!,
                                    offsets.values.toList().getOrNull(idx - 1),
                                    z,
                                    plotRect,
                                    map
                                )
                            }
                        }
                    }

                    series.run { postDrawerDraw(offsets) }
                }
            }
        }
    }
    return offsets
}

private fun IPlotDrawScope.applyFocusedFlag(axis: Axis, offsets: Map<SeriesItem<*>, Offset>) {
    offsets.entries.sortedBy { (_, offset) ->
        distanceToMouse(axis, offset)
    }.forEachIndexed { idx, entry ->
        entry.key.isFocused.value = idx == 0 &&
                distanceToMouse(axis, entry.value) < 100f
    }
}

internal fun IPlotParameterScope.mappedOffset(offset: Offset): Offset =
    try {
        (scaleCenter.value + (offset - scaleCenter.value) / scale.value) - translationOffset.value - translation.value / scale.value
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

private fun IPlotDrawScope.drawGrid(yWidth: Float, xHeight: Float) {
    val gridWidth = plotSize.value.width.dp - horizontalPadding * 2 - yWidth.dp
    val gridHeight = plotSize.value.height.dp - verticalPadding * 2 - xHeight.dp
    if (gridWidth < 0.dp || gridHeight < 0.dp) return

    clipRect(
        horizontalPadding.value,
        verticalPadding.value,
        horizontalPadding.value + gridWidth.toPx(),
        verticalPadding.value + gridHeight.toPx()
    ) {
        translate(this@drawGrid.finalTranslation.x, this@drawGrid.finalTranslation.y) {
            scale(this@drawGrid.scale.value, this@drawGrid.scaleCenter.value - this@drawGrid.translationOffset.value) {
                this@drawGrid.run {
                    val gridSize = gridSize(allSeries.flatMap { it.items }).toPx().coerceAtLeast(1f)
                    val numX = (gridWidth.toPx() / gridSize).toInt()
                    (-numX..numX * 2).forEach { xIdx ->
                        drawLine(
                            grid,
                            Offset(xIdx * gridSize, -gridHeight.toPx()),
                            Offset(
                                xIdx * gridSize,
                                gridHeight.toPx() * 2
                            ),
                            gridStrokeWidth,
                            alpha = grid.alpha
                        )
                    }

                    val numY = (gridHeight.toPx() / gridSize).toInt()
                    (-numY..numY * 2).forEach { yIdx ->
                        drawLine(
                            grid,
                            Offset(
                                -gridWidth.toPx(),
                                verticalPadding.toPx() + verticalPlotPadding.toPx() + yIdx * gridSize
                            ),
                            Offset(
                                gridWidth.toPx() * 2,
                                verticalPadding.toPx() + verticalPlotPadding.toPx() + yIdx * gridSize
                            ),
                            gridStrokeWidth,
                            alpha = grid.alpha
                        )
                    }
                }
            }
        }
    }
}

private fun IPlotDrawScope.drawPlotBackground(yWidth: Float, xHeight: Float) {
    val gridWidth = plotSize.value.width.dp - horizontalPadding * 2 - yWidth.dp
    val gridHeight = plotSize.value.height.dp - verticalPadding * 2 - xHeight.dp
    if (gridWidth < 0.dp || gridHeight < 0.dp) return

    drawRect(
        background, Offset(horizontalPadding.value, verticalPadding.value),
        Size(gridWidth.toPx(), gridHeight.toPx()), background.alpha, Fill
    )
}

private fun IPlotDrawScope.drawPlotBorder(yWidth: Float, xHeight: Float) {
    val gridWidth = plotSize.value.width.dp - horizontalPadding * 2 - yWidth.dp
    val gridHeight = plotSize.value.height.dp - verticalPadding * 2 - xHeight.dp
    if (gridWidth < 0.dp || gridHeight < 0.dp) return

    drawRect(
        border, Offset(horizontalPadding.value, verticalPadding.value),
        Size(gridWidth.toPx(), gridHeight.toPx()), border.alpha, Stroke(1f)
    )
}

private fun IPlotDrawScope.drawAxis(
    yTicks: List<Pair<Number, String>>,
    yWidth: Float,
    xTicks: List<Pair<Number, String>>,
    xHeight: Float
) {
    drawYAxis(yTicks, yWidth, xHeight)
    drawXAxis(xTicks, xHeight, yWidth)
}

private fun IPlotDrawScope.drawYAxis(yTicks: List<Pair<Number, String>>, yWidth: Float, xHeight: Float) {
    val top = verticalPadding.value
    val labelHeight =
        plotSize.value.height - top * 2 - verticalPlotPadding.value * 2 - xHeight
    val heightFactor = labelHeight / allSeries.yHeight

    val left = 0f
    val right = plotSize.value.width.toFloat()
    val bottom = plotSize.value.height - verticalPadding.value - xHeight
    if (left > right || top > bottom) return

    clipRect(
        left, top, right, bottom
    ) {
        translate(0f, this@drawYAxis.finalTranslation.y) {
            scale(
                1f,
                this@drawYAxis.scale.value,
                this@drawYAxis.scaleCenter.value - this@drawYAxis.translationOffset.value
            ) {
                this@drawYAxis.run {
                    val x = plotSize.value.width - horizontalPadding.value - yWidth
                    yTicks.forEach { (y, str) ->
                        val yScene = plotSize.value.height - y.toFloat() * heightFactor -
                                verticalPadding.value - verticalPlotPadding.value -
                                xHeight

                        if (drawYTicks)
                            drawLine(
                                axisTicks,
                                Offset((x - plotTickLength.value / 2), yScene),
                                Offset((x + plotTickLength.value / 2), yScene)
                            )

                        if (drawYLabels) {
                            val offset = Offset(
                                (x + plotTickLength.value),
                                yScene + plotLabelFontSize / 3 / scale.value
                            )
                            scale(1 / scale.value, 1 / scale.value, offset) {
                                this@drawYAxis.run {
                                    drawText(
                                        str,
                                        offset,
                                        plotLabelFontSize,
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

private fun IPlotDrawScope.drawXAxis(xTicks: List<Pair<Number, String>>, xHeight: Float, yWidth: Float) {
    val labelWidth =
        plotSize.value.width - horizontalPadding.value * 2 - horizontalPlotPadding.value * 2 - yWidth
    val widthFactor = labelWidth / allSeries.xWidth

    val left = horizontalPadding.value
    val top = 0f
    val right = plotSize.value.width.toFloat() - horizontalPadding.value - yWidth
    val bottom = plotSize.value.height.toFloat()
    if (left > right || top > bottom) return

    clipRect(left, top, right, bottom) {
        translate(this@drawXAxis.finalTranslation.x, 0f) {
            scale(
                this@drawXAxis.scale.value,
                1f,
                this@drawXAxis.scaleCenter.value - this@drawXAxis.translationOffset.value
            ) {
                val y = this@drawXAxis.plotSize.value.height - this@drawXAxis.verticalPadding.value - xHeight
                this@drawXAxis.run { plotXTicks(allItems) }.forEach { (x, str) ->
                    val xScene = x.toFloat() * widthFactor + left + this@drawXAxis.horizontalPlotPadding.value
                    if (this@drawXAxis.drawXTicks)
                        drawLine(
                            this@drawXAxis.axisTicks,
                            Offset(xScene, y - this@drawXAxis.plotTickLength.value / 2f),
                            Offset(xScene, y + this@drawXAxis.plotTickLength.value / 2f)
                        )

                    if (this@drawXAxis.drawXLabels) {
                        val textLine = TextLine.make(str, Font(null, this@drawXAxis.plotLabelFontSize))
                        val offset = Offset(
                            xScene - textLine.width / 2 / this@drawXAxis.scale.value,
                            (y + this@drawXAxis.plotTickLength.value + textLine.height)
                        )
                        scale(1 / this@drawXAxis.scale.value, 1f, offset) {
                            this@drawXAxis.run {
                                drawText(
                                    str,
                                    offset,
                                    plotLabelFontSize,
                                    axisLabels
                                )
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

data class PlotHandler(
    val scale: (Float) -> Unit,
    val translate: (Offset) -> Unit,
    val resetTransformations: () -> Unit
)