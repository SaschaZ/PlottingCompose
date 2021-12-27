package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.style.TextAlign

import dev.zieger.plottingcompose.SinglePlot
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.definition.Value
import dev.zieger.plottingcompose.drawText
import dev.zieger.plottingcompose.mappedOffset
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine

data class Label(
    val ySlot: Slot<Float>,
    val contentSlot: Slot<String>,
    val fontSize: Float = 18f,
    val contentColor: Color = Color.Black,
    val backgroundColor: Color = Color.White,
    val borderColor: Color = Color.Black,
    val borderWidth: Float = 0.1f,
    val borderRoundCorner: Float = 5f,
    val fontScale: Float = 1f,
    val padding: Float = 5f,
    val mouseIsPositionSource: Boolean = true,
    val position: IPlotDrawScope.(String, Offset, SinglePlot) -> Offset = { c, pos, plot ->
        val font = Font(null, fontSize)
        val lines = c.split('\n').map { it to TextLine.make(it, font) }
        val labelWidth = lines.maxOf { it.second.width } / scale.value
        val labelHeight = lines.sumOf { it.second.height.toInt() } / scale.value
        val plotRect = plot.main
        when {
            pos.x < plotRect.left + plotRect.width * 0.5f -> Offset(
                pos.x - padding,
                pos.y - padding
            )
            else -> Offset(
                pos.x - labelWidth * scale.value - padding,
                pos.y - padding
            )
        }.let {
            when {
                pos.y > plotRect.bottom - plotRect.height * 0.5f ->
                    it.copy(y = it.y - labelHeight * scale.value * 2.05f)
                else ->
                    it.copy(y = it.y + labelHeight * scale.value * 1.2f)
            }
        }
    },
    val size: IPlotDrawScope.(String) -> Size = { c ->
        val font = Font(null, fontSize)
        val lines = c.split('\n').map { it to TextLine.make(it, font) }
        val labelWidth = lines.maxOf { it.second.width } / scale.value
        val labelHeight = lines.sumOf { it.second.height.toInt() } / scale.value
        Size(labelWidth + padding * 2 / scale.value, labelHeight + padding * 2 / scale.value)
    }
) : PlotStyle(ySlot, contentSlot) {

    override fun IPlotDrawScope.drawSingle(x: Long, data: Map<Key, Map<Port<*>, Value?>>, plot: SinglePlot) {
        ySlot.float(data)?.let { y ->
            contentSlot.string(data)?.let { c ->
                val font = Font(null, fontSize)
                val lines = c.split('\n').map { it to TextLine.make(it, font) }

                val position =
                    if (mouseIsPositionSource) mousePosition.value ?: return else Offset(x.toFloat(), y)
                val (startTop, size) = position(c, position, plot).let {
                    if (mouseIsPositionSource) mappedOffset(it) else plot.toScene(it)
                } to size(c)

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
    }
}