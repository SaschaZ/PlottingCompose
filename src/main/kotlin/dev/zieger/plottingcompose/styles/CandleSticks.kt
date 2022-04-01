package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.zieger.plottingcompose.definition.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.indicators.candles.Ohcl
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import dev.zieger.plottingcompose.scopes.x
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

open class CandleSticks<I : Input>(
    private val slot: Slot<I, Ohcl.Companion.Ohcl>,
    private val positiveColor: Color = Color.Green,
    private val negativeColor: Color = Color.Red,
    private val lineColor: Color = Color.White,
    private val lineWidth: Float = 0.033f
) : PlotStyle<I>(slot) {

    override fun IPlotDrawScope<I>.drawSingle(
        idx: Long,
        value: I,
        data: Map<Key<I, *>, List<PortValue<*>>>,
        isFocused: Boolean
    ) {
        if (heightDivisor.value.run { isInfinite() || isNaN() }) {
            println("invalid heightDivisor ${heightDivisor.value}")
            return
        }

        val candle = slot.value() ?: run {
            println("no candle stored")
            return
        }
        val bodySize = Size(
            (0.85f / widthDivisor).toFloat(),
            ((candle.open - candle.close).absoluteValue.toFloat() / heightDivisor.value.toFloat()).coerceAtLeast(
                (lineWidth / widthDivisor).toFloat()
            )
        )
        val topLeft = Offset(
            plotRect.left + (idx / widthDivisor - bodySize.width / 2).toFloat(),
            plotRect.bottom - max(candle.open, candle.close).toFloat() / heightDivisor.value.toFloat()
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
            alpha = (lineColor.alpha * (finalScale.x - 1f)).coerceIn(0.0..1.0).toFloat(),
            style = Stroke((lineWidth / widthDivisor).toFloat())
        )

        val topMid = topLeft.copy(topLeft.x + bodySize.width / 2)
        drawLine(
            lineColor,
            topMid,
            topMid.copy(y = (plotRect.bottom - candle.high / heightDivisor.value).toFloat()),
            (lineWidth / widthDivisor).toFloat(), alpha = lineColor.alpha
        )
        val bottomMid =
            Offset(topMid.x, plotRect.bottom - min(candle.open, candle.close).toFloat() / heightDivisor.value.toFloat())
        drawLine(
            lineColor,
            bottomMid,
            bottomMid.copy(y = (plotRect.bottom - candle.low / heightDivisor.value).toFloat()),
            (lineWidth / widthDivisor).toFloat(), alpha = lineColor.alpha
        )
    }
}