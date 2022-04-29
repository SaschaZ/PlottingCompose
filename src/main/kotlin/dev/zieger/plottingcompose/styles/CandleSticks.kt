package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.di.ChartScope
import dev.zieger.plottingcompose.indicators.candles.Ohcl
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

    override fun ChartScope.drawSingle(
        idx: Long,
        value: I,
        data: Map<Key<I, *>, List<PortValue<*>>>,
        isFocused: Boolean
    ) {
        if (finalScale.y.run { isInfinite() || isNaN() }) {
            println("invalid heightDivisor ${finalScale.y}")
            return
        }

        val candle = slot.value() ?: run {
            println("no candle stored")
            return
        }
        val bodySize = Size(
            (0.85f / finalScale.x).toFloat(),
            ((candle.open - candle.close).absoluteValue.toFloat() / finalScale.y).coerceAtLeast(
                (lineWidth / finalScale.x).toFloat()
            )
        )
        val topLeft = Offset(
            plotRect.left + (idx / finalScale.x - bodySize.width / 2).toFloat(),
            plotRect.bottom - max(candle.open, candle.close).toFloat() / finalScale.y
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
            alpha = (lineColor.alpha * (finalScale.x - 1f)).coerceIn(0f..1f),
            style = Stroke(lineWidth / finalScale.x)
        )

        val topMid = topLeft.copy(topLeft.x + bodySize.width / 2)
        drawLine(
            lineColor,
            topMid,
            topMid.copy(y = (plotRect.bottom - candle.high / finalScale.y).toFloat()),
            (lineWidth / finalScale.x).toFloat(), alpha = lineColor.alpha
        )
        val bottomMid =
            Offset(topMid.x, plotRect.bottom - min(candle.open, candle.close).toFloat() / finalScale.y)
        drawLine(
            lineColor,
            bottomMid,
            bottomMid.copy(y = (plotRect.bottom - candle.low / finalScale.y).toFloat()),
            lineWidth / finalScale.x, alpha = lineColor.alpha
        )
    }
}