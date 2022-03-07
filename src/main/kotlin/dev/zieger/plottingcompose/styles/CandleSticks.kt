package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.indicators.Ohcl
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import dev.zieger.plottingcompose.x
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

open class CandleSticks<I : Input>(
    private val slot: Slot<I, Ohcl.Companion.Ohcl>,
    private val positiveColor: Color = Color.Green,
    private val negativeColor: Color = Color.Red,
    private val lineColor: Color = Color.White,
    private val lineWidth: Float = 1f
) : PlotStyle<I>(slot) {

    override fun IPlotDrawScope<I>.drawSingle(value: I, data: Map<Key<I>, List<PortValue<*>>>) {
        val candle = slot.value(data) ?: return
        println("heightDivisor=$heightDivisor(${System.identityHashCode(this)})  in style")
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