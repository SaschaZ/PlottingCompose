package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.zieger.plottingcompose.definition.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.indicators.Ohcl
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import dev.zieger.plottingcompose.scopes.ValueHolder
import dev.zieger.plottingcompose.x
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

open class CandleSticks<T : InputContainer>(
    private val slot: Slot<Ohcl.Companion.Ohcl, T>,
    private val positiveColor: Color = Color.Green,
    private val negativeColor: Color = Color.Red,
    private val lineColor: Color = Color.White,
    private val lineWidth: Float = 1f
) : PlotStyle<T>(slot) {

    override fun IPlotDrawScope<T>.drawSingle(value: T, data: Map<Key<T>, Map<Port<*>, ValueHolder?>>) {
        val candle = slot.value(data)!!
        val bodySize = Size(
            40 / widthDivisor * xStretchFactor.value,
            (candle.open - candle.close).absoluteValue.toFloat() / heightDivisor
        )
        val topLeft = Offset(
            plotRect.left + (candle.openTime / widthDivisor - bodySize.width / 2),
            plotRect.bottom - max(candle.open, candle.close).toFloat() / heightDivisor
        )

        val color = if (candle.open <= candle.close) positiveColor else negativeColor
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
            alpha = (lineColor.alpha * (scale.value.x - 1f)).coerceIn(0f..1f),
            style = Stroke(lineWidth / scale.value.x)
        )

        val topMid = topLeft.copy(topLeft.x + bodySize.width / 2)
        drawLine(
            lineColor,
            topMid,
            topMid.copy(y = plotRect.bottom - candle.high.toFloat() / heightDivisor),
            lineWidth / scale.value.x, alpha = lineColor.alpha
        )
        val bottomMid = Offset(topMid.x, plotRect.bottom - min(candle.open, candle.close).toFloat() / heightDivisor)
        drawLine(
            lineColor,
            bottomMid,
            bottomMid.copy(y = plotRect.bottom - candle.low.toFloat() / heightDivisor),
            lineWidth / scale.value.x, alpha = lineColor.alpha
        )
    }
}