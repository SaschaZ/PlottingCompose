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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.mouse.MouseScrollOrientation
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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

    val rect = PlotRect(this) ?: return

    drawPlotBackground(rect)
    if (drawGrid) drawGrid(rect)

    drawPlot(allSeries, rect)

    if (drawChartBorder) drawPlotBorder(rect)
    applyFocusedFlag(focusAxis, rect)

    drawAxis(rect)
}

data class PlotRect(
    val main: Rect,
    val plot: Rect,
    val yLabel: Rect,
    val xLabel: Rect,
    val widthFactor: Float,
    val heightFactor: Float,
    val yTicks: List<Pair<Number, String>>,
    val xTicks: List<Pair<Number, String>>
) {
    companion object {

        operator fun invoke(scope: IPlotDrawScope): PlotRect? {
            val yTicks = scope.run { plotYTicks(scope.allItems) }
            val xTicks = scope.run { plotXTicks(scope.allItems) }
            val yLabelWidth = if (scope.drawYLabels) yTicks.yLabelWidth(scope) else 0.dp
            val xLabelHeight = if (scope.drawXLabels) xTicks.xLabelHeight(scope) else 0.dp
            val plotSize = scope.plotSize.value

            val main = scope.run {
                val left = horizontalPadding.value
                val top = verticalPadding.value
                val right =
                    plotSize.width - horizontalPadding.value - yLabelWidth.value - if (scope.drawYLabels) plotTickLength.value else 0f
                val bottom =
                    plotSize.height - verticalPadding.value - xLabelHeight.value - if (scope.drawXLabels) plotTickLength.value else 0f
                if (left > right || top > bottom) return null
                Rect(left, top, right, bottom)
            }
            val plot = scope.run {
                val left = main.left + horizontalPlotPadding.value
                val top = main.top + verticalPlotPadding.value
                val right = main.right - horizontalPlotPadding.value
                val bottom = main.bottom - verticalPlotPadding.value
                if (left > right || top > bottom) return null
                Rect(left, top, right, bottom)
            }
            val yLabel = scope.run {
                val left = main.right - plotTickLength.value / 2
                val top = main.top
                val right = plotSize.width - horizontalPadding.value
                val bottom = main.bottom
                if (left > right || top > bottom) return null
                Rect(left, top, right, bottom)
            }
            val xLabel = scope.run {
                val left = main.left
                val top = main.bottom - plotTickLength.value / 2
                val right = main.right
                val bottom = plotSize.height - verticalPadding.value
                if (left > right || top > bottom) return null
                Rect(left, top, right, bottom)
            }

            return PlotRect(
                main, plot, yLabel, xLabel,
                plot.width / scope.allSeries.xWidth,
                plot.height / scope.allSeries.yHeight,
                yTicks, xTicks
            )
        }

        private fun List<Pair<Number, String>>.yLabelWidth(scope: IPlotDrawScope): Dp {
            val font = Font(null, scope.plotLabelFontSize)
            return maxOf { TextLine.make(it.second, font).width }.dp
        }

        private fun List<Pair<Number, String>>.xLabelHeight(scope: IPlotDrawScope): Dp {
            val font = Font(null, scope.plotLabelFontSize)
            return maxOf { TextLine.make(it.second, font).height }.dp
        }
    }

    fun map(offset: Offset): Offset = Offset(
        plot.left + widthFactor * offset.x,
        plot.bottom - heightFactor * offset.y
    )

    fun <T : PlotItem> offsets(items: List<SeriesItem<T>>) =
        items.associateWith { item -> map(Offset(item.data.x, item.data.y)) }
}

private fun IPlotDrawScope.drawPlot(
    allSeries: List<Series<*>>, rect: PlotRect
) {
    clipRect(rect.main) {
        translate(
            this@drawPlot.finalTranslation.x,// - (this@drawPlot.widthFactorCenter.value.x - this@drawPlot.horizontalPadding.value * 1.3f) * this@drawPlot.widthFactor.value + (this@drawPlot.widthFactorCenter.value.x - this@drawPlot.horizontalPadding.value * 1.5f),
            this@drawPlot.finalTranslation.y
        ) {
            scale(
                this@drawPlot.scale.value,
                this@drawPlot.scaleCenter.value - this@drawPlot.translationOffset.value
            ) {
                this@drawPlot.run {
                    allSeries.flatMap { i -> i.styles.map { s -> Triple(i.items, s, s.z) } }
                        .sortedBy { it.third }
                        .forEach { (items, style, z) ->
                            style.run { draw(items.map { it.data }, rect) }
                        }
                }
            }
        }
    }
}

fun IPlotDrawScope.clipRect(rect: Rect, block: DrawScope.() -> Unit) {
    clipRect(rect.left, rect.top, rect.right, rect.bottom, ClipOp.Intersect, block)
}

private fun IPlotDrawScope.applyFocusedFlag(axis: Axis, rect: PlotRect) {
    allItems.map { it to rect.map(it.data.offset) }
        .sortedBy { (_, offset) ->
            distanceToMouse(axis, offset)
        }.forEachIndexed { idx, (item, offset) ->
            item.data.hasFocus = idx == 0 && distanceToMouse(axis, offset) < 100f
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

private fun IPlotDrawScope.drawGrid(rect: PlotRect) {
    clipRect(rect.main) {
        translate(this@drawGrid.finalTranslation.x, this@drawGrid.finalTranslation.y) {
            scale(this@drawGrid.scale.value, this@drawGrid.scaleCenter.value - this@drawGrid.translationOffset.value) {
                this@drawGrid.run {
                    val gridSize = gridSize(allSeries.flatMap { it.items }).toPx().coerceAtLeast(1f)
                    val numX = (rect.plot.width / gridSize).toInt()
                    (-numX..numX * 2).forEach { xIdx ->
                        drawLine(
                            grid,
                            Offset(xIdx * gridSize, -rect.plot.height),
                            Offset(
                                xIdx * gridSize,
                                rect.plot.height * 2
                            ),
                            gridStrokeWidth,
                            alpha = grid.alpha
                        )
                    }

                    val numY = (rect.plot.height / gridSize).toInt()
                    (-numY..numY * 2).forEach { yIdx ->
                        drawLine(
                            grid,
                            Offset(
                                -rect.plot.width,
                                verticalPadding.toPx() + verticalPlotPadding.toPx() + yIdx * gridSize
                            ),
                            Offset(
                                rect.plot.width * 2,
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

private fun IPlotDrawScope.drawPlotBackground(rect: PlotRect) {
    drawRect(background, rect.main.topLeft, rect.main.size, background.alpha, Fill)
}

private fun IPlotDrawScope.drawPlotBorder(rect: PlotRect) {
    drawRect(border, rect.main.topLeft, rect.main.size, border.alpha, Stroke(1f))
}

private fun IPlotDrawScope.drawAxis(rect: PlotRect) {
    drawYAxis(rect)
    drawXAxis(rect)
}

private fun IPlotDrawScope.drawYAxis(rect: PlotRect) {
    clipRect(rect.yLabel) {
        translate(0f, this@drawYAxis.finalTranslation.y) {
            scale(
                1f,
                this@drawYAxis.scale.value,
                this@drawYAxis.scaleCenter.value - this@drawYAxis.translationOffset.value
            ) {
                this@drawYAxis.run {
                    val x =
                        plotSize.value.width - horizontalPadding.value - rect.yLabel.width + plotTickLength.value / 2
                    rect.yTicks.forEach { (y, str) ->
                        val yScene = plotSize.value.height - y.toFloat() * rect.heightFactor -
                                verticalPadding.value - verticalPlotPadding.value -
                                rect.xLabel.height

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

private fun IPlotDrawScope.drawXAxis(rect: PlotRect) {
    clipRect(rect.xLabel) {
        translate(this@drawXAxis.finalTranslation.x, 0f) {
            scale(
                this@drawXAxis.scale.value,
                1f,
                this@drawXAxis.scaleCenter.value - this@drawXAxis.translationOffset.value
            ) {
                val y = rect.xLabel.top + this@drawXAxis.plotTickLength.value / 2
                this@drawXAxis.run { plotXTicks(allItems) }.forEach { (x, str) ->
                    val xScene =
                        x.toFloat() * rect.widthFactor + rect.xLabel.left + this@drawXAxis.horizontalPlotPadding.value
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