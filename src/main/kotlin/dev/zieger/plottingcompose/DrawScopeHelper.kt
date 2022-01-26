package dev.zieger.plottingcompose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.style.TextAlign
import dev.zieger.plottingcompose.definition.InputContainer
import dev.zieger.plottingcompose.scopes.IChartDrawScope
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
    drawRect(color, rect.topLeft, rect.size, style = drawStyle)

fun <T : InputContainer, S : IChartDrawScope<T>> S.clipRect(rect: Rect, block: S.() -> Unit): Unit = rect.run {
    clipRect(left, top, right, bottom, ClipOp.Intersect) {
        block(this@clipRect)
    }
}

fun <T : InputContainer, S : IChartDrawScope<T>> S.translate(offset: Offset, block: S.() -> Unit) {
    translate(offset.x, offset.y) { block(this@translate) }
}

fun <T : InputContainer, S : IChartDrawScope<T>> S.scale(
    scale: Pair<Float, Float>,
    pivot: Offset,
    block: S.() -> Unit
) {
    scale(scale.x, scale.y, pivot) { block(this@scale) }
}

val Pair<Float, Float>.x: Float get() = first
val Pair<Float, Float>.y: Float get() = second
