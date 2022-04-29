package dev.zieger.plottingcompose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine


fun DrawScope.drawText(
    text: String,
    offset: Offset,
    size: Float,
    color: Color,
    scaleX: Float = 1f,
    align: TextAlign = TextAlign.Start
) {
    drawIntoCanvas {
        val font = Font(null, size, scaleX, 0f)
        val textLine = if (align != TextAlign.Start) TextLine.make(text, font) else null
        val off = when (align) {
            TextAlign.Center, TextAlign.Justify -> offset - Offset(textLine!!.width / 2f / scaleX, 0f)
            TextAlign.End, TextAlign.Right -> offset + Offset(textLine!!.width / 2f, -textLine.height / 2f)
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

fun DrawScope.drawRect(color: Color, rect: Rect, drawStyle: DrawStyle = Fill) =
    runCatching { drawRect(color, rect.topLeft, rect.size, style = drawStyle) }

fun <S : DrawScope> S.clipRect(rect: Rect, block: S.() -> Unit) {
    runCatching {
        clipRect(rect.left, rect.top, rect.right, rect.bottom, block = { block(this@clipRect) })
    }.getOrNull()
}

fun <S : DrawScope> S.translate(translation: Offset, block: S.() -> Unit) =
    translate(translation.x, translation.y) { block(this@translate) }

fun <S : DrawScope> S.scale(scale: Offset, pivot: Offset, block: S.() -> Unit) =
    scale(scale.x, scale.y, pivot) { block(this@scale) }

val Pair<Float, Float>.x: Float get() = first
val Pair<Float, Float>.y: Float get() = second
