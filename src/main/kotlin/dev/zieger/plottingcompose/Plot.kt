package dev.zieger.plottingcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.zieger.plottingcompose.scopes.*
import org.jetbrains.skia.Font
import kotlin.math.absoluteValue
import kotlin.math.sqrt

@Composable
fun Plot(
    modifier: Modifier = Modifier,
    parameter: IParameter = PlotParameter(),
    colors: IPlotColors = PlotColors(),
    block: ItemScope.() -> Unit
) {
    Plot(modifier, parameter, colors, PlotScope(), block)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Plot(
    modifier: Modifier,
    parameter: IParameter,
    colors: IPlotColors,
    plotScope: IPlotScope,
    block: ItemScope.() -> Unit
) {
    PlotParameterScope(plotScope, parameter.withPlotScope(plotScope), colors).apply {
        val allSeries = remember { mutableStateListOf<Series<*>>() }
        Canvas(modifier.fillMaxSize().onSizeChanged { plotSize.value = it }
            .mouseScrollFilter { event, _ ->
                if (!enableScale) return@mouseScrollFilter false

                if (event.orientation == MouseScrollOrientation.Vertical)
                    ((event.delta as? MouseScrollUnit.Line)?.value
                        ?: (event.delta as? MouseScrollUnit.Page)?.value)?.also { delta ->
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
                true
            }, onExit = {
                mousePosition.value = null
                false
            })
        ) {
            PlotDrawScope(this@apply, this, allSeries).draw()
        }

        fun IPlotParameterScope.applyTranslationOffset() {
            val items = allSeries.flatMap { it.items }
            if (items.isEmpty()) return

            val yRange = items.minOf { it.yMin.toFloat() }..items.maxOf { it.yMax.toFloat() }
            val xRange = items.minOf { it.x.toFloat() }..items.maxOf { it.x.toFloat() }

            val plotWidth =
                plotSize.value.width - horizontalPadding.value * 2 - horizontalPlotPadding.value * 2 - plotYLabelWidth.value
            val plotHeight =
                plotSize.value.height - verticalPadding.value * 2 - verticalPlotPadding.value * 2 - plotXLabelHeight.value

            val widthFactor = xRange.run { endInclusive - start } / plotWidth
            val heightFactor = yRange.run { endInclusive - start } / plotHeight

            translationOffset.value = Offset(-xRange.start / widthFactor, yRange.start / heightFactor)
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
        applyTranslationOffset()
    }
}

private operator fun IntSize.div(fl: Float): Offset {
    return Offset((width / fl), (height / fl))
}

fun IntSize.toFloat(): Size = Size(width.toFloat(), height.toFloat())

private fun IPlotDrawScope.draw() {
    if (allSeries.isEmpty()) return

    val plotWidth =
        plotSize.value.width.dp - horizontalPadding * 2 - horizontalPlotPadding * 2 - plotYLabelWidth.value.dp
    val plotHeight =
        plotSize.value.height.dp - verticalPadding * 2 - verticalPlotPadding * 2 - plotXLabelHeight.value.dp

    val items = allSeries.flatMap { it.items }
    val valueXRange = items.minOf { it.x.toFloat() }..items.maxOf { it.x.toFloat() }
    val valueYRange = items.minOf { it.yMin.toFloat() }..items.maxOf { it.yMax.toFloat() }

    val widthFactor = plotWidth / valueXRange.run { endInclusive - start }
    val heightFactor = plotHeight / valueYRange.run { endInclusive - start }

    drawPlotBackground()
    if (drawGrid) drawGrid()

    val offsets = allSeries.flatMap { drawPlot(it, plotHeight, widthFactor, heightFactor)?.entries ?: emptyList() }
        .associate { it.key to it.value }

    if (drawChartBorder) drawPlotBorder()
    applyFocusedFlag(focusAxis, offsets)

    drawAxis()
}

private fun IPlotDrawScope.drawPlot(
    series: Series<*>,
    plotHeight: Dp,
    widthFactor: Dp,
    heightFactor: Dp
): Map<SeriesItem<*>, Offset>? {
    var offsets: Map<SeriesItem<*>, Offset> = emptyMap()
    val start = horizontalPadding.value
    val top = verticalPadding.value
    val end = plotSize.value.width.toFloat() - horizontalPadding.value - plotYLabelWidth.value
    val bottom = plotSize.value.height.toFloat() - verticalPlotPadding.value - plotXLabelHeight.value
    if (start > end || top > bottom) return null

    clipRect(start, top, end, bottom) {
        translate(this@drawPlot.finalTranslation.x, this@drawPlot.finalTranslation.y) {
            scale(
                this@drawPlot.scale.value,
                this@drawPlot.scaleCenter.value - this@drawPlot.translationOffset.value
            ) {
                this@drawPlot.run {
                    val map: Offset.() -> Offset = {
                        Offset(
                            (horizontalPadding + horizontalPlotPadding + widthFactor * x).toPx(),
                            (verticalPadding + verticalPlotPadding + plotHeight - heightFactor * y).toPx()
                        )
                    }
                    offsets = series.items.associateWith { item ->
                        Offset(item.x.toFloat(), item.y.value.toFloat()).map()
                    }

                    series.run { preDrawerDraw(offsets) }

                    series.z.sorted().forEach { z ->
                        series.items.forEachIndexed { idx, item ->
                            item.run {
                                draw(
                                    offsets[item]!!,
                                    offsets.values.toList().getOrNull(idx - 1),
                                    z,
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

enum class Axis { X, Y, BOTH }

private fun IPlotDrawScope.applyFocusedFlag(axis: Axis, offsets: Map<SeriesItem<*>, Offset>) {
    offsets.entries.sortedBy { (_, offset) ->
        distanceToMouse(axis, offset)
    }.forEachIndexed { idx, entry ->
        entry.key.isFocused.value = idx == 0 &&
                distanceToMouse(axis, entry.value) < 100f
    }
}

internal fun IPlotParameterScope.mappedOffset(offset: Offset): Offset =
    (scaleCenter.value + (offset - scaleCenter.value) / scale.value) - translationOffset.value - translation.value / scale.value

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

private fun IPlotDrawScope.drawGrid() {
    val gridWidth = plotSize.value.width.dp - horizontalPadding * 2 - plotYLabelWidth.value.dp
    val gridHeight = plotSize.value.height.dp - verticalPadding * 2 - plotXLabelHeight.value.dp
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

private fun IPlotDrawScope.drawPlotBackground() {
    val gridWidth = plotSize.value.width.dp - horizontalPadding * 2 - plotYLabelWidth.value.dp
    val gridHeight = plotSize.value.height.dp - verticalPadding * 2 - plotXLabelHeight.value.dp
    if (gridWidth < 0.dp || gridHeight < 0.dp) return

    drawRect(
        background, Offset(horizontalPadding.value, verticalPadding.value),
        Size(gridWidth.toPx(), gridHeight.toPx()), background.alpha, Fill
    )
}

private fun IPlotDrawScope.drawPlotBorder() {
    val gridWidth = plotSize.value.width.dp - horizontalPadding * 2 - plotYLabelWidth.value.dp
    val gridHeight = plotSize.value.height.dp - verticalPadding * 2 - plotXLabelHeight.value.dp
    if (gridWidth < 0.dp || gridHeight < 0.dp) return

    drawRect(
        border, Offset(horizontalPadding.value, verticalPadding.value),
        Size(gridWidth.toPx(), gridHeight.toPx()), border.alpha, Stroke(1f)
    )
}

private fun IPlotDrawScope.drawAxis() {
    drawYAxis()
    drawXAxis()
}

private fun IPlotDrawScope.drawYAxis() {
    val items = allSeries.flatMap { it.items }
    val top = verticalPadding.value
    val labelHeight =
        plotSize.value.height - top * 2 - verticalPlotPadding.value * 2 - plotXLabelHeight.value
    val valueYRange = items.minOf { it.yMin.toFloat() }..items.maxOf { it.yMax.toFloat() }
    val heightFactor = labelHeight / valueYRange.run { endInclusive - start }

    val left = 0f
    val right = plotSize.value.width.toFloat()
    val bottom = plotSize.value.height - top - plotXLabelHeight.value
    if (left > right || top > bottom) return

    clipRect(
        left, top, right,
        bottom
    ) {
        translate(left, this@drawYAxis.finalTranslation.y) {
            scale(
                1f,
                this@drawYAxis.scale.value,
                this@drawYAxis.scaleCenter.value - this@drawYAxis.translationOffset.value
            ) {
                this@drawYAxis.run {
                    val x = plotSize.value.width - horizontalPadding.value - plotYLabelWidth.value
                    plotYTicks(items).forEach { (y, str) ->
                        val yScene = plotSize.value.height - y.toFloat() * heightFactor -
                                verticalPadding.value - verticalPlotPadding.value -
                                plotXLabelHeight.value

                        if (drawYTicks)
                            drawLine(
                                grid,
                                Offset((x - plotTickLength.value / 2), yScene),
                                Offset((x + plotTickLength.value / 2), yScene)
                            )

                        if (drawYLabels)
                            drawText(
                                str,
                                Offset(
                                    (x + plotTickLength.value),
                                    yScene + plotLabelFontSize / 3 / scale.value
                                ),
                                plotLabelFontSize / scale.value,
                                axisLabels,
                                scale.value
                            )
                    }
                }
            }
        }
    }
}

private fun IPlotDrawScope.drawXAxis() {
    val items = allSeries.flatMap { it.items }
    val left = horizontalPadding.value
    val labelWidth = plotSize.value.width - left * 2 - horizontalPlotPadding.value * 2
    val valueXRange = items.minOf { it.x.toLong() }..items.maxOf { it.x.toLong() }
    val widthFactor = labelWidth / valueXRange.run { endInclusive - start }

    val top = 0f
    val right = plotSize.value.width.toFloat() - left - plotYLabelWidth.value
    val bottom = plotSize.value.height.toFloat()
    if (left > right || top > bottom) return

    clipRect(
        left, top,
        right,
        bottom
    ) {
        translate(this@drawXAxis.finalTranslation.x, top) {
            scale(
                this@drawXAxis.scale.value,
                1f,
                this@drawXAxis.scaleCenter.value - this@drawXAxis.translationOffset.value
            ) {
                val y =
                    this@drawXAxis.plotSize.value.height - this@drawXAxis.verticalPadding.value - this@drawXAxis.plotXLabelHeight.value
                this@drawXAxis.run { plotXTicks(items) }.forEach { (x, str) ->
                    val xScene = x.toFloat() * widthFactor +
                            left + this@drawXAxis.horizontalPlotPadding.value
                    if (this@drawXAxis.drawXTicks)
                        drawLine(
                            this@drawXAxis.axisTicks,
                            Offset(xScene, y - this@drawXAxis.plotTickLength.value / 2f),
                            Offset(xScene, y + this@drawXAxis.plotTickLength.value / 2f)
                        )

                    if (this@drawXAxis.drawXLabels)
                        drawText(
                            str,
                            Offset(
                                xScene - (this@drawXAxis.plotLabelFontSize * str.length * 0.25f) / this@drawXAxis.scale.value,
                                (y + this@drawXAxis.plotTickLength.value + this@drawXAxis.plotLabelFontSize)
                            ),
                            this@drawXAxis.plotLabelFontSize,
                            this@drawXAxis.axisLabels,
                            1 / this@drawXAxis.scale.value
                        )
                }
            }
        }
    }
}

fun DrawScope.drawText(text: String, offset: Offset, size: Float, color: Color, scale: Float = 1f) {
    drawIntoCanvas {
        it.nativeCanvas.drawString(
            text, offset.x, offset.y,
            Font(null, size, scale, 0f), Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = color.toArgb()
            }
        )
    }
}