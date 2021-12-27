package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.zieger.plottingcompose.SinglePlot
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.definition.Value
import dev.zieger.plottingcompose.indicators.Ohcl
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

open class CandleSticks(
    private val slot: Slot<Ohcl.Companion.Ohcl>,
    private val positiveColor: Color = Color.Green,
    private val negativeColor: Color = Color.Red,
    private val lineColor: Color = Color.Black,
    private val lineWidth: Float = 1f
) : PlotStyle(slot) {

    override fun IPlotDrawScope.drawSingle(x: Long, data: Map<Key, Map<Port<*>, Value?>>, plot: SinglePlot) {
        val wF = plot.widthFactor

        val candle = slot.any<Ohcl.Companion.Ohcl>(data)!!
            .map { plot.yToScene(it.toFloat()).toDouble() }
        val bodySize = Size(40 * wF * widthFactor.value, (candle.open - candle.close).absoluteValue.toFloat())
        val topLeft = Offset(
            (candle.openTime - bodySize.width / 2 * wF),
            min(candle.open, candle.close).toFloat()
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
            alpha = (lineColor.alpha * (scale.value - 1f)).coerceIn(0f..1f),
            style = Stroke(lineWidth / scale.value)
        )

        val topMid = topLeft.copy(topLeft.x + bodySize.width / 2)
        drawLine(
            lineColor,
            topMid,
            topMid.copy(y = candle.high.toFloat()),
            lineWidth / scale.value, alpha = lineColor.alpha
        )
        val bottomMid = Offset(topMid.x, max(candle.open, candle.close).toFloat())
        drawLine(
            lineColor,
            bottomMid,
            bottomMid.copy(y = candle.low.toFloat()),
            lineWidth / scale.value, alpha = lineColor.alpha
        )
    }
}