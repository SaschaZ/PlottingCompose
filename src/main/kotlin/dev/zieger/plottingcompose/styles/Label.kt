package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.style.TextAlign
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.di.ChartScope
import dev.zieger.plottingcompose.drawText
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine

data class Label<I : Input, O : Output>(
    val ySlot: Slot<I, O>,
    val fontSize: Float = 20f,
    val contentColor: Color = Color.Black,
    val backgroundColor: Color = Color.White,
    val borderColor: Color = Color.Black,
    val borderWidth: Float = 0.1f,
    val borderRoundCorner: Float = 5f,
    val fontScale: Float = 1f,
    val padding: Float = 5f,
    val mouseIsPositionSource: Boolean = false,
    val size: ChartScope.(String) -> Size = { c ->
        val font = Font(null, fontSize)
        val lines = c.split('\n').map { it to TextLine.make(it, font) }
        val labelWidth = lines.maxOf { it.second.width }
        val labelHeight = lines.sumOf { it.second.height.toDouble() }.toFloat()
        Size(labelWidth + padding * 2, labelHeight + padding * 2)
    },
    val selector: (O) -> Pair<Float, String>
) : PlotStyle<I>(ySlot) {

    override fun ChartScope.drawSingle(
        idx: Long,
        value: I,
        data: Map<Key<I, *>, List<PortValue<*>>>,
        isFocused: Boolean
    ) {
        ySlot.value()?.let { scalar ->
            val (y, c) = selector(scalar)
            val font = Font(null, fontSize)
            val lines = c.split('\n').map { it to TextLine.make(it, font) }

            val position =
                if (mouseIsPositionSource) mousePosition.value ?: return else Offset(idx.toFloat(), y)
            val size = size(c)
            val startTop = position.let {
                if (mouseIsPositionSource) it.copy(
                    x = -finalTranslation.x + it.x,
                    y = -finalTranslation.y + it.y
                ) else it.toScene() - size / 2f
            }

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
                    x = startTop.x + padding,
                    y = startTop.y + lines.take(idx + 1).sumOf { it.second.height.toDouble() }
                        .toFloat() - fontSize / 3 + padding
                )
                scale((1 / scale.x).toFloat(), (1 / scale.y).toFloat(), off) {
                    drawText(
                        line.first,
                        off,
                        fontSize,
                        contentColor,
                        scale.x.toFloat(),
                        TextAlign.Start
                    )
                }
            }
        }
    }
}

private operator fun Offset.minus(size: Size): Offset =
    Offset(x - size.width, y - size.height)
