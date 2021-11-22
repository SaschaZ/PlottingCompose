package dev.zieger.plottingcompose

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

abstract class PlotStyle<T> {

    open val z: List<Int> = listOf(0)

    abstract fun IPlotDrawScope.draw(
        item: T,
        offset: Offset,
        previousOffset: Offset?,
        isFocused: Boolean,
        z: Int,
        map: Offset.() -> Offset
    )
}

val PlotStyle<*>.zMin get() = z.minOf { it }
val PlotStyle<*>.zMax get() = z.maxOf { it }

class EmtpyPlotStyle<T> : PlotStyle<T>() {
    override fun IPlotDrawScope.draw(
        item: T,
        offset: Offset,
        previousOffset: Offset?,
        isFocused: Boolean,
        z: Int,
        map: Offset.() -> Offset
    ) {

    }
}

class Group<T>(vararg styles: PlotStyle<T>) : PlotStyle<T>() {
    private val styles: List<PlotStyle<T>> = styles.toList()

    override val z: List<Int> = styles.flatMap { it.z }.distinct()

    override fun IPlotDrawScope.draw(
        item: T,
        offset: Offset,
        previousOffset: Offset?,
        isFocused: Boolean,
        z: Int,
        map: Offset.() -> Offset
    ) {
        styles.forEach { it.run { draw(item, offset, previousOffset, isFocused, z, map) } }
    }
}

data class Dot<T>(
    val color: Color = Color.Black,
    val width: Float = 2f
) : PlotStyle<T>() {
    override fun IPlotDrawScope.draw(
        item: T,
        offset: Offset,
        previousOffset: Offset?,
        isFocused: Boolean,
        z: Int,
        map: Offset.() -> Offset
    ) {
        drawCircle(color, width / 2, offset, color.alpha)
    }
}

data class Focusable<T>(
    val unfocused: PlotStyle<T>,
    val focused: PlotStyle<T>
) : PlotStyle<T>() {

    override val z: List<Int> = (unfocused.z + focused.z).distinct()

    override fun IPlotDrawScope.draw(
        item: T,
        offset: Offset,
        previousOffset: Offset?,
        isFocused: Boolean,
        z: Int,
        map: Offset.() -> Offset
    ) {
        when {
            isFocused && z in focused.z -> focused.run { draw(item, offset, previousOffset, isFocused, z, map) }
            !isFocused && z in unfocused.z -> unfocused.run { draw(item, offset, previousOffset, isFocused, z, map) }
        }
    }
}

data class Line<T>(
    val color: Color = Color.Black,
    val width: Float = 1f
) : PlotStyle<T>() {
    override fun IPlotDrawScope.draw(
        item: T,
        offset: Offset,
        previousOffset: Offset?,
        isFocused: Boolean,
        z: Int,
        map: Offset.() -> Offset
    ) {
        previousOffset?.also { pO -> drawLine(color, pO, offset, width, alpha = color.alpha) }
    }
}


abstract class PlotDrawer<T> {
    abstract fun IPlotDrawScope.draw(offsets: Map<SeriesItem<T>, Offset>)
}

class Filled<T>(
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
    override fun IPlotDrawScope.draw(
        item: Ohcl,
        offset: Offset,
        previousOffset: Offset?,
        isFocused: Boolean,
        z: Int,
        map: Offset.() -> Offset
    ) {
        val plotWidth =
            plotSize.value.width.dp - horizontalPadding * 2 - horizontalPlotPadding * 2 - plotYLabelWidth.value.dp
        val plotHeight =
            plotSize.value.height.dp - verticalPadding * 2 - verticalPlotPadding * 2 - plotXLabelHeight.value.dp

        val items = allSeries.flatMap { it.items }
        val valueXRange = items.minOf { it.x.toFloat() }..items.maxOf { it.x.toFloat() }
        val valueYRange = items.minOf { it.yMin.toFloat() }..items.maxOf { it.yMax.toFloat() }

        val widthFactor = plotWidth.value / valueXRange.run { endInclusive - start }
        val heightFactor = plotHeight.value / valueYRange.run { endInclusive - start }

        val bodySize = Offset(50f * widthFactor, (item.open - item.close).absoluteValue * heightFactor).toSize()
        val topLeft = Offset(item.time - bodySize.width / 2, max(item.open, item.close)).map()

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
            topMid.copy(y = topMid.y - (item.high - max(item.close, item.open)) * heightFactor),
            lineWidth / scale.value, alpha = lineColor.alpha
        )
        val bottomMid = topLeft.copy(
            topLeft.x + bodySize.width / 2,
            topLeft.y + bodySize.height
        )
        drawLine(
            lineColor,
            bottomMid,
            bottomMid.copy(y = bottomMid.y + (min(item.open, item.close) - item.low) * heightFactor),
            lineWidth / scale.value, alpha = lineColor.alpha
        )
    }
}

data class Label<T>(
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

    override val z: List<Int> = listOf(1)

    override fun IPlotDrawScope.draw(
        item: T,
        offset: Offset,
        previousOffset: Offset?,
        isFocused: Boolean,
        z: Int,
        map: Offset.() -> Offset
    ) {
        val c = content(item)
        val lines = c.count { it == '\n' }
        val labelWidth = c.split('\n').maxOf { it.length } * fontSize * 0.62f / ((scale.value - 1f) * 0.65f + 1f)
        val labelHeight = (lines + 1) * fontSize * 0.91f / scale.value

        val position = mousePosition.value ?: offset
        val widthRange = horizontalPadding.value + horizontalPlotPadding.value..
                plotSize.value.width - horizontalPlotPadding.value - horizontalPadding.value - plotYLabelWidth.value
        val widthLength = widthRange.run { endInclusive - start }
        val heightRange = verticalPadding.value + verticalPlotPadding.value..
                plotSize.value.height - verticalPlotPadding.value - verticalPadding.value
        val heightLength = heightRange.run { endInclusive - start }

        val (startTop, size) = when {
            position.x < widthRange.start + widthLength * 0.5f -> Offset(
                position.x - padding,
                position.y - padding
            )
            else -> Offset(
                position.x - labelWidth * scale.value - padding,
                position.y - padding
            )
        }.let {
            when {
                position.y > heightRange.endInclusive - heightLength * 0.5f ->
                    it.copy(y = it.y - labelHeight * scale.value * 2.05f)
                else ->
                    it.copy(y = it.y + labelHeight * scale.value * 1.2f)
            }
        }.let {
            mappedOffset(it)
        } to Size(labelWidth + padding * 2 / scale.value, labelHeight + padding * 2 / scale.value)

        drawRoundRect(backgroundColor, startTop, size, CornerRadius(borderRoundCorner / scale.value, borderRoundCorner / scale.value),
            Fill, backgroundColor.alpha)
        drawRoundRect(borderColor, startTop, size, CornerRadius(borderRoundCorner / scale.value, borderRoundCorner / scale.value),
            Stroke(borderWidth / scale.value), borderColor.alpha)
        drawText(
            c,
            startTop.copy(x = startTop.x + padding / scale.value, y = startTop.y + labelHeight + padding / scale.value),
            fontSize / scale.value,
            contentColor,
            fontScale
        )
    }
}

private fun Offset.toSize(): Size = Size(x, y)
