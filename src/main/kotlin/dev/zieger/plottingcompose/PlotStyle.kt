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
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

abstract class PlotStyle<out T : PlotItem<Position.Raw>> {
    open val z: List<Int> = listOf(0)

    open fun IPlotDrawScope.draw(items: List<@UnsafeVariance T>, requestedZ: Int, plot: SinglePlot) {
        if (requestedZ !in z) return
        drawScene(items.filterNot { it.position.isYEmpty }.map { it.toScene(plot) })
    }

    abstract fun IPlotDrawScope.drawScene(items: List<PlotItem<Position.Scene>>)
}

class EmtpyPlotStyle<T : PlotItem<Position.Raw>> : PlotStyle<T>() {
    override fun IPlotDrawScope.drawScene(items: List<PlotItem<Position.Scene>>) = Unit
}

open class Group<T : PlotItem<Position.Raw>>(vararg styles: PlotStyle<T>) : PlotStyle<T>() {

    private val styles = styles.toList()

    override val z: List<Int> = styles.flatMap { it.z }

    override fun IPlotDrawScope.draw(items: List<T>, requestedZ: Int, plot: SinglePlot) {
        styles.filter { requestedZ in it.z }
            .forEach { it.run { draw(items, requestedZ, plot) } }
    }

    override fun IPlotDrawScope.drawScene(items: List<PlotItem<Position.Scene>>) = Unit
}

open class Single(
    val draw: IPlotDrawScope.(Offset, Boolean) -> Unit = Dot()
) : PlotStyle<PlotItem<Position.Raw>>() {
    override fun IPlotDrawScope.drawScene(items: List<PlotItem<Position.Scene>>) {
        if (items.isEmpty()) return

        items.forEach { item ->
            item.run { draw(item.position.offset, item.hasFocus) }
        }
    }
}

@Suppress("FunctionName")
fun Dot(
    color: Color = Color.Black, width: Float = 1f, strokeWidth: Float? = null,
    focusColor: Color = color, focusWidth: Float = width, focusedStrokeWidth: Float? = strokeWidth
): IPlotDrawScope.(Offset, Boolean) -> Unit = { offset, hasFocus ->
    drawCircle(
        if (hasFocus) focusColor else color,
        (if (hasFocus) focusWidth else width) / 2,
        offset,
        if (hasFocus) focusColor.alpha else color.alpha,
        if (hasFocus) focusedStrokeWidth?.let { Stroke(it) } ?: Fill else strokeWidth?.let { Stroke(it) } ?: Fill
    )
}

data class Line(
    val draw: IPlotDrawScope.(Path, Boolean) -> Unit = SimpleLine()
) : PlotStyle<PlotItem<Position.Raw>>() {

    override val z: List<Int> = listOf(1)

    override fun IPlotDrawScope.drawScene(items: List<PlotItem<Position.Scene>>) {
        if (items.isEmpty()) return

        var hasFocus = false
        draw(Path().apply {
            items.filterNot { it.position.isYEmpty }
                .forEach { item ->
                    val off = item.position.offset
                    hasFocus = hasFocus || item.hasFocus
                    when {
                        isEmpty -> moveTo(off.x, off.y)
                        else -> lineTo(off.x, off.y)
                    }
                }
        }, hasFocus)
    }
}

@Suppress("FunctionName")
fun SimpleLine(
    color: Color = Color.Black, width: Float = 1f,
    focusedColor: Color = color, focusedWidth: Float = width
): IPlotDrawScope.(Path, Boolean) -> Unit = { path, hasFocus ->
    drawPath(
        path, if (hasFocus) focusedColor else color, if (hasFocus) focusedColor.alpha else color.alpha,
        Stroke(if (hasFocus) focusedWidth else width)
    )
}

class Filled<T : PlotItem<Position.Raw>>(
    private val color: Color,
    private val upWards: Boolean = false
) : PlotStyle<T>() {
    override fun IPlotDrawScope.drawScene(items: List<PlotItem<Position.Scene>>) {
        val hasY = items.filterNot { it.position.isYEmpty }
        if (hasY.isEmpty()) return

        val path = Path()
        val startY = if (upWards) hasY.maxOf { it.yMax } else hasY.minOf { it.yMin }
        path.moveTo(hasY.first().position.offset.x, startY)
        hasY.forEach { path.lineTo(it.position.offset) }
        path.lineTo(items.last().position.offset.x, startY)

        drawPath(path, color, color.alpha)
    }
}

private fun Path.lineTo(offset: Offset) = lineTo(offset.x, offset.y)

data class CandleSticks(
    private val positiveColor: Color = Color.Green,
    private val negativeColor: Color = Color.Red,
    private val focusedPositiveColor: Color = Color.Cyan,
    private val focusedNegativeColor: Color = Color.Magenta,
    private val lineColor: Color = Color.Black,
    private val lineWidth: Float = 1f
) : PlotStyle<Ohcl>() {

    override fun IPlotDrawScope.draw(items: List<Ohcl>, requestedZ: Int, plot: SinglePlot) {
        val wF = plot.widthFactor
        val hF = plot.heightFactor

        items.forEach { item ->
            val bodySize = Size(50f * wF * widthFactor.value, (item.open - item.close).absoluteValue * hF)
            val topLeft = plot.toScene(Offset((item.time - bodySize.width / 2), max(item.open, item.close)))

            val color = if (item.open <= item.close) if (item.hasFocus) focusedPositiveColor else positiveColor else
                if (item.hasFocus) focusedNegativeColor else negativeColor
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

    override fun IPlotDrawScope.drawScene(items: List<PlotItem<Position.Scene>>) = Unit
}

data class Label<T : PlotItem<Position.Raw>>(
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

    override val z: List<Int> = listOf(100)

    override fun IPlotDrawScope.drawScene(items: List<PlotItem<Position.Scene>>) = Unit

    override fun IPlotDrawScope.draw(items: List<T>, requestedZ: Int, plot: SinglePlot) {
        items.filter { it.hasFocus }
            .forEach { item ->
                val c = content(item)
                val font = Font(null, fontSize)
                val lines = c.split('\n').map { it to TextLine.make(it, font) }
                val labelWidth = lines.maxOf { it.second.width } / scale.value
                val labelHeight = lines.sumOf { it.second.height.toInt() } / scale.value
                val plotRect = plot.main

                val position = mousePosition.value ?: item.position.offset

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

