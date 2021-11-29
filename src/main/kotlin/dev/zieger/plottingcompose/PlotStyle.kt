@file:Suppress("unused")

package dev.zieger.plottingcompose

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.style.TextAlign
import dev.zieger.plottingcompose.PlotStyle.SeriesPlotStyle
import dev.zieger.plottingcompose.PlotStyle.SinglePlotStyle
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

sealed class PlotStyle<out T : PlotItem> {
    abstract val z: List<Int>

    fun IPlotDrawScope.draw(items: List<PlotSeriesItem<@UnsafeVariance T>>, requestedZ: Int, plot: SinglePlot) {
        if (requestedZ !in z || items.isEmpty()) return
        val mapped = items.map { item -> item.item.map(plot) }
        if (mapped.isEmpty()) return

        when (this@PlotStyle) {
            is SeriesPlotStyle<*> -> drawScene(mapped, requestedZ, plot)
            is SinglePlotStyle<*> -> mapped.sortedBy { it.x }.forEach { item ->
                drawScene(item, requestedZ, plot)
            }
        }
    }

    abstract class SeriesPlotStyle<out T : PlotItem>(override val z: List<Int> = listOf(0)) :
        PlotStyle<T>() {

        constructor(z: Int) : this(listOf(z))

        companion object {
            object Empty : SeriesPlotStyle<PlotItem>(emptyList()) {
                override fun IPlotDrawScope.drawScene(
                    items: List<PlotItem>,
                    requestedZ: Int,
                    plot: SinglePlot
                ) = Unit
            }
        }

        abstract fun IPlotDrawScope.drawScene(items: List<@UnsafeVariance T>, requestedZ: Int, plot: SinglePlot)
    }

    abstract class SinglePlotStyle<out T : PlotItem>(override val z: List<Int> = listOf(0)) :
        PlotStyle<T>() {

        constructor(z: Int) : this(listOf(z))

        companion object {
            object Empty : SinglePlotStyle<PlotItem>(emptyList()) {
                override fun IPlotDrawScope.drawScene(
                    item: PlotItem,
                    requestedZ: Int,
                    plot: SinglePlot
                ) = Unit
            }
        }

        abstract fun IPlotDrawScope.drawScene(
            item: @UnsafeVariance T,
            requestedZ: Int,
            plot: SinglePlot
        )
    }
}

open class SingleGroup<out T : PlotItem>(vararg styles: SinglePlotStyle<T>) :
    SinglePlotStyle<T>(styles.flatMap { it.z }.distinct()) {

    private val styles = styles.toList()

    override fun IPlotDrawScope.drawScene(
        item: @UnsafeVariance T,
        requestedZ: Int,
        plot: SinglePlot
    ) {
        if (requestedZ !in z) return

        styles.filter { requestedZ in it.z }
            .forEach { it.run { drawScene(item, requestedZ, plot) } }
    }
}

open class SingleFocusable<out T : PlotItem>(
    val unfocused: SinglePlotStyle<T>,
    val focused: SinglePlotStyle<T>
) : SingleGroup<T>(*arrayOf(unfocused, focused)) {
    override fun IPlotDrawScope.drawScene(
        item: @UnsafeVariance T,
        requestedZ: Int,
        plot: SinglePlot
    ) {
        if (requestedZ !in z) return

        when (item.hasFocus) {
            true -> focused.run { drawScene(item, requestedZ, plot) }
            false -> unfocused.run { drawScene(item, requestedZ, plot) }
        }
    }
}

open class Dot<out T : PlotItem>(
    val color: Color = Color.Black,
    val width: Float = 1f,
    val strokeWidth: Float? = null,
    z: Int = 0
) : SinglePlotStyle<T>(listOf(z)) {

    override fun IPlotDrawScope.drawScene(
        item: @UnsafeVariance T,
        requestedZ: Int,
        plot: SinglePlot
    ) {
        if (requestedZ !in z) return

        item.y.values.firstOrNull()?.toFloat()?.let {
            drawCircle(
                color,
                width / 2,
                Offset(item.x.toFloat(), it),
                color.alpha,
                strokeWidth?.let { s -> Stroke(s) } ?: Fill)
        }
    }
}

open class Line<out T : PlotItem>(
    val color: Color = Color.Black,
    val width: Float = 1f,
    z: Int = 1,
    private val y: (T) -> Float? = { it.y.values.filterNotNull().firstOrNull() }
) : SeriesPlotStyle<T>(z) {

    override fun IPlotDrawScope.drawScene(items: List<@UnsafeVariance T>, requestedZ: Int, plot: SinglePlot) {
        if (items.isEmpty() || requestedZ !in z) return

        drawPath(Path().apply {
            items.mapNotNull { y(it)?.let { it2 -> it.x to it2 } }
                .forEach { (x, y) ->
                    when {
                        isEmpty -> moveTo(x, y)
                        else -> lineTo(x, y)
                    }
                }
        }, color, color.alpha, Stroke(width))
    }
}

class FillBetween<out T : PlotItem>(
    private val color: Color = Color.Cyan.copy(alpha = 0.66f),
    z: Int = 1,
    private val between: (T) -> Pair<Float, Float>?
) : SeriesPlotStyle<T>(z) {
    override fun IPlotDrawScope.drawScene(items: List<@UnsafeVariance T>, requestedZ: Int, plot: SinglePlot) {
        val offsets = items.mapNotNull { between(it)?.run { Offset(it.x, first) to Offset(it.x, second) } }
        val path = Path().apply {
            offsets.forEach { (top, _) ->
                when {
                    isEmpty -> moveTo(top)
                    else -> lineTo(top)
                }
            }
            offsets.reversed().forEach { (_, bottom) ->
                when {
                    isEmpty -> moveTo(bottom)
                    else -> lineTo(bottom)
                }
            }
        }
        drawPath(path, color, color.alpha, Fill)
    }
}

class Fill<out T : PlotItem>(
    private val color: Color = Color.Black.copy(alpha = 0.25f),
    private val upWards: Boolean = false,
    z: Int = 0
) : SeriesPlotStyle<T>(listOf(z)) {

    override fun IPlotDrawScope.drawScene(items: List<@UnsafeVariance T>, requestedZ: Int, plot: SinglePlot) {
        val checked = items.map { item -> item.x to item.y.toList().mapNotNull { (i, y) -> y?.let { v -> i to v } } }
        if (checked.isEmpty()) return

        val path = Path()
        val startY = if (upWards) checked.maxOf { it.second.minOf { m -> m.second } } else
            checked.minOf { it.second.minOf { m -> m.second } }
        path.moveTo(checked.first().first.toFloat(), startY.toFloat())
        checked.forEach { path.lineTo(it.first.toFloat(), it.second.first().second.toFloat()) }
        path.lineTo(checked.last().first.toFloat(), startY.toFloat())

        drawPath(path, color, color.alpha, Fill)
    }
}

class Impulses<out T : PlotItem>(
    private val color: Color = Color.Cyan,
    z: Int = 0,
    private val impulse: (T) -> Float? = { it.y.values.filterNotNull().firstOrNull() }
) : SinglePlotStyle<T>(z) {
    override fun IPlotDrawScope.drawScene(item: @UnsafeVariance T, requestedZ: Int, plot: SinglePlot) {
        impulse(item)?.let { Offset(item.x, it) }?.let { offset ->
            val size = Size(50f * plot.widthFactor, offset.y)
            if (size.width < 0 || size.height < 0) return
            val topLeft = offset.copy(x = offset.x - size.width / 2, y = plot.plot.height - offset.y)
            drawRect(color, topLeft, size, color.alpha, Fill)
        }
    }
}

private fun Path.moveTo(offset: Offset) = moveTo(offset.x, offset.y)
private fun Path.lineTo(offset: Offset) = lineTo(offset.x, offset.y)

open class CandleSticks(
    private val positiveColor: Color = Color.Green,
    private val negativeColor: Color = Color.Red,
    private val lineColor: Color = Color.Black,
    private val lineWidth: Float = 1f,
    z: Int = 0
) : SinglePlotStyle<Ohcl>(listOf(z)) {

    override fun IPlotDrawScope.drawScene(
        item: Ohcl,
        requestedZ: Int,
        plot: SinglePlot
    ) {
        val wF = plot.widthFactor
        val hF = plot.heightFactor

        val bodySize = Size(40 * wF * widthFactor.value, (item.open - item.close).absoluteValue)
        val topLeft = Offset((item.time - bodySize.width / 2 * wF), min(item.open, item.close))

        val color = if (item.open <= item.close) positiveColor else negativeColor
        drawRect(
            color,
            topLeft,
            bodySize,
            alpha = color.alpha
        )
        drawRect(
            lineColor,
            topLeft,
            bodySize,
            alpha = (lineColor.alpha * (scale.value - 1f)).coerceIn(0f..1f),
            style = Stroke(lineWidth / scale.value)
        )

        val topMid = topLeft.copy(topLeft.x + bodySize.width / 2)
        drawLine(
            lineColor,
            topMid,
            topMid.copy(y = item.high),
            lineWidth / scale.value, alpha = lineColor.alpha
        )
        val bottomMid = Offset(topMid.x, max(item.open, item.close))
        drawLine(
            lineColor,
            bottomMid,
            bottomMid.copy(y = item.low),
            lineWidth / scale.value, alpha = lineColor.alpha
        )
    }
}

data class Label<out T : PlotItem>(
    val fontSize: Float = 18f,
    val contentColor: Color = Color.Black,
    val backgroundColor: Color = Color.White,
    val borderColor: Color = Color.Black,
    val borderWidth: Float = 0.1f,
    val borderRoundCorner: Float = 5f,
    val fontScale: Float = 1f,
    val padding: Float = 5f,
    val content: (@UnsafeVariance T) -> String
) : SinglePlotStyle<T>(listOf(100)) {

    override fun IPlotDrawScope.drawScene(item: @UnsafeVariance T, requestedZ: Int, plot: SinglePlot) {
        if (!item.hasFocus) return
        val c = content(item)
        val font = Font(null, fontSize)
        val lines = c.split('\n').map { it to TextLine.make(it, font) }
        val labelWidth = lines.maxOf { it.second.width } / scale.value
        val labelHeight = lines.sumOf { it.second.height.toInt() } / scale.value
        val plotRect = plot.main

        val position = mousePosition.value ?: Offset(item.x, item.y.values.first()!!)

        val (startTop, size) = when {
            position.x < plotRect.left + plotRect.width * 0.5f -> Offset(
                position.x - padding,
                position.y - padding
            )
            else -> Offset(
                position.x - labelWidth * scale.value - padding,
                position.y - padding
            )
        }.let {
            when {
                position.y > plotRect.bottom - plotRect.height * 0.5f ->
                    it.copy(y = it.y - labelHeight * scale.value * 2.05f)
                else ->
                    it.copy(y = it.y + labelHeight * scale.value * 1.2f)
            }
        }.let {
            mappedOffset(it)
        } to Size(labelWidth + padding * 2 / scale.value, labelHeight + padding * 2 / scale.value)

        drawRoundRect(
            backgroundColor,
            startTop,
            size,
            CornerRadius(borderRoundCorner / scale.value, borderRoundCorner / scale.value),
            Fill,
            backgroundColor.alpha
        )
        drawRoundRect(
            borderColor,
            startTop,
            size,
            CornerRadius(borderRoundCorner / scale.value, borderRoundCorner / scale.value),
            Stroke(borderWidth / scale.value),
            borderColor.alpha
        )
        lines.forEachIndexed { idx, line ->
            val scale = scale.value
            val off = startTop.copy(
                x = startTop.x + padding / scale,
                y = startTop.y + (lines.take(idx + 1).sumOf { it.second.height.toInt() }) / scale
            )
            scale(1 / scale, 1 / scale, off) {
                drawText(
                    line.first,
                    off,
                    fontSize,
                    contentColor,
                    1f,
                    TextAlign.Start
                )
            }
        }
    }
}