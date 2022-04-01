package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.style.TextAlign
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.drawText
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import dev.zieger.plottingcompose.scopes.x
import dev.zieger.plottingcompose.scopes.y
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine

data class Label<I : Input>(
    val ySlot: Slot<I, Output.Scalar>,
    val contentSlot: Slot<I, Output.Label>,
    val fontSize: Float = 18f,
    val contentColor: Color = Color.Black,
    val backgroundColor: Color = Color.White,
    val borderColor: Color = Color.Black,
    val borderWidth: Float = 0.1f,
    val borderRoundCorner: Float = 5f,
    val fontScale: Float = 1f,
    val padding: Float = 5f,
    val mouseIsPositionSource: Boolean = true,
    val position: IPlotDrawScope<*>.(String, Offset) -> Offset = { c, pos ->
        val font = Font(null, fontSize)
        val lines = c.split('\n').map { it to TextLine.make(it, font) }
        val labelWidth = lines.maxOf { it.second.width } / finalScale.x
        val labelHeight = lines.sumOf { it.second.height.toInt() } / finalScale.y
        when {
            pos.x < plotRect.left + plotRect.width * 0.5f -> Offset(
                pos.x - padding,
                pos.y - padding
            )
            else -> Offset(
                (pos.x - labelWidth * finalScale.x - padding).toFloat(),
                pos.y - padding
            )
        }.let {
            when {
                pos.y > plotRect.bottom - plotRect.height * 0.5f ->
                    it.copy(y = (it.y - labelHeight * finalScale.y * 2.05f).toFloat())
                else ->
                    it.copy(y = (it.y + labelHeight * finalScale.y * 1.2f).toFloat())
            }
        }
    },
    val size: IPlotDrawScope<*>.(String) -> Size = { c ->
        val font = Font(null, fontSize)
        val lines = c.split('\n').map { it to TextLine.make(it, font) }
        val labelWidth = lines.maxOf { it.second.width } / finalScale.x
        val labelHeight = lines.sumOf { it.second.height.toInt() } / finalScale.y
        Size((labelWidth + padding * 2 / finalScale.x).toFloat(), (labelHeight + padding * 2 / finalScale.y).toFloat())
    }
) : PlotStyle<I>(ySlot, contentSlot) {

    override fun IPlotDrawScope<I>.drawSingle(
        idx: Long,
        value: I,
        data: Map<Key<I, *>, List<PortValue<*>>>,
        isFocused: Boolean
    ) {
        ySlot.value()?.scalar?.toFloat()?.let { y ->
            contentSlot.value()?.label?.let { c ->
                val font = Font(null, fontSize)
                val lines = c.split('\n').map { it to TextLine.make(it, font) }

                val position =
                    if (mouseIsPositionSource) mousePosition.value ?: return else Offset(value.x.toFloat(), y)
                val (startTop, size) = position(this@drawSingle, c, position).let {
                    if (mouseIsPositionSource) it else it.toScene()
                } to size(c)

                drawRoundRect(
                    backgroundColor,
                    startTop,
                    size,
                    CornerRadius(
                        (borderRoundCorner / finalScale.x).toFloat(),
                        (borderRoundCorner / finalScale.y).toFloat()
                    ),
                    Fill,
                    backgroundColor.alpha
                )
                drawRoundRect(
                    borderColor,
                    startTop,
                    size,
                    CornerRadius(
                        (borderRoundCorner / finalScale.x).toFloat(),
                        (borderRoundCorner / finalScale.y).toFloat()
                    ),
                    Stroke((borderWidth / finalScale.x).toFloat()),
                    borderColor.alpha
                )
                lines.forEachIndexed { idx, line ->
                    val scale = finalScale
                    val off = startTop.copy(
                        x = (startTop.x + padding / scale.x).toFloat(),
                        y = (startTop.y + (lines.take(idx + 1).sumOf { it.second.height.toInt() }) / scale.y).toFloat()
                    )
                    scale((1 / scale.x).toFloat(), (1 / scale.y).toFloat(), off) {
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