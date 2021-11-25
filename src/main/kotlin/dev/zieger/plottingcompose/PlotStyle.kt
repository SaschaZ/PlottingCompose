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
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

interface PlotItem {
    val x: Float
    val y: Float

    val xMin: Float get() = x
    val xMax: Float get() = x
    val yMin: Float get() = y
    val yMax: Float get() = y

    var hasFocus: Boolean
}

data class SimplePlotItem(override val x: Float, override val y: Float) : PlotItem {
    override var hasFocus = false
}

val PlotItem.offset get() = Offset(x, y)

abstract class PlotStyle<out T : PlotItem> {
    open val z: Int = 0

    abstract fun IPlotDrawScope.draw(items: List<@UnsafeVariance T>, rect: PlotRect)

    open fun reset() = Unit
}

class EmtpyPlotStyle<T : PlotItem> : PlotStyle<T>() {
    override fun IPlotDrawScope.draw(items: List<T>, rect: PlotRect) = Unit
}

open class Group<T : PlotItem>(vararg styles: PlotStyle<T>) : PlotStyle<T>() {

    private val styles = styles.toList()

    override val z: Int = styles.firstOrNull()?.z
        ?: throw IllegalArgumentException("at least one PlotStyle needs to be provided")

    override fun IPlotDrawScope.draw(items: List<T>, rect: PlotRect) {
        styles.forEach { it.run { draw(items, rect) } }
    }
}

data class Dot<T : PlotItem>(
    val color: Color = Color.Black,
    val width: Float = 2f
) : PlotStyle<T>() {
    override fun IPlotDrawScope.draw(items: List<T>, rect: PlotRect) {
        items.forEach { item ->
            drawCircle(color, width / 2, item.offset, color.alpha)
        }
    }
}

data class Line<T : PlotItem>(
    val color: Color = Color.Black,
    val width: Float = 1f
) : PlotStyle<T>() {

    override val z: Int = 1

    override fun IPlotDrawScope.draw(items: List<T>, rect: PlotRect) {
        drawPath(Path().apply {
            items.forEach { item ->
                val off = rect.map(item.offset)
                when {
                    isEmpty -> moveTo(off.x, off.y)
                    else -> lineTo(off.x, off.y)
                }
            }
        }, color, color.alpha, Stroke(width))
    }
}


abstract class PlotDrawer<T : PlotItem> {
    abstract fun IPlotDrawScope.draw(offsets: Map<SeriesItem<T>, Offset>)
}

class Filled<T : PlotItem>(
    private val color: Color,
    upWards: Boolean = false,
    private val baseLine: (Map<SeriesItem<T>, Offset>) -> Pair<Float, Float> =
        {
            (if (upWards) it.maxOf { i2 -> i2.value.y } else it.minOf { i2 -> i2.value.y })
                .run { this to this }
        }
) : PlotDrawer<T>() {
    override fun IPlotDrawScope.draw(offsets: Map<SeriesItem<T>, Offset>) {
        if (offsets.isEmpty()) return

        val path = Path()
        val startY = baseLine(offsets)
        path.moveTo(offsets.entries.first().value.x, startY.first)
        offsets.forEach { path.lineTo(it.value.x, it.value.y) }
        path.lineTo(offsets.entries.last().value.x, startY.second)

        drawPath(path, color, color.alpha)
    }
}

data class CandleSticks(
    private val positiveColor: Color = Color.Green,
    private val negativeColor: Color = Color.Red,
    private val lineColor: Color = Color.Black,
    private val lineWidth: Float = 1f
) : PlotStyle<Ohcl>() {
    override fun IPlotDrawScope.draw(items: List<Ohcl>, rect: PlotRect) {
        val wF = rect.widthFactor
        val hF = rect.heightFactor

        items.forEach { item ->
            val bodySize = Offset(50f * wF, (item.open - item.close).absoluteValue * hF).toSize()
            val topLeft =
                Offset((item.time - bodySize.width / 2) * this@draw.widthFactor.value, max(item.open, item.close)).let {
                    Offset(rect.plot.left + wF * it.x, rect.plot.bottom - hF * it.y)
                }

            drawRect(
                if (item.open <= item.close) positiveColor else negativeColor,
                topLeft,
                bodySize,
                alpha = if (item.open <= item.close) positiveColor.alpha else negativeColor.alpha
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
                topMid.copy(y = topMid.y - (item.high - max(item.close, item.open)) * hF),
                lineWidth / scale.value, alpha = lineColor.alpha
            )
            val bottomMid = topLeft.copy(
                topLeft.x + bodySize.width / 2,
                topLeft.y + bodySize.height
            )
            drawLine(
                lineColor,
                bottomMid,
                bottomMid.copy(y = bottomMid.y + (min(item.open, item.close) - item.low) * hF),
                lineWidth / scale.value, alpha = lineColor.alpha
            )
        }
    }
}

data class Label<T : PlotItem>(
    val fontSize: Float = 18f,
    val contentColor: Color = Color.Black,
    val backgroundColor: Color = Color.White,
    val borderColor: Color = Color.Black,
    val borderWidth: Float = 0.1f,
    val borderRoundCorner: Float = 5f,
    val fontScale: Float = 1f,
    val padding: Float = 5f,
    val content: (T) -> String
) : PlotStyle<T>() {

    override val z: Int = 100

    override fun IPlotDrawScope.draw(items: List<T>, rect: PlotRect) {
        items.filter { it.hasFocus }
            .forEach { item ->
                val c = content(item)
                val font = Font(null, fontSize)
                val lines = c.split('\n').map { it to TextLine.make(it, font) }
                val labelWidth = lines.maxOf { it.second.width } / scale.value
                val labelHeight = lines.sumOf { it.second.height.toInt() } / scale.value
                val plotRect = rect.main

                val position = mousePosition.value ?: item.offset

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
                    val scale = this@draw.scale.value
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
}

private fun Offset.toSize(): Size = Size(x, y)
