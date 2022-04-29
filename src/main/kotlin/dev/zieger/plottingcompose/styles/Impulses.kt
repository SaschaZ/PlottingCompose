package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.di.ChartScope

class Impulses<I : Input>(
    private val slot: Slot<I, Output.Scalar>,
    private val positiveColor: Color = Color.Green,
    private val negativeColor: Color = Color.Red
) : PlotStyle<I>(slot) {
    override fun ChartScope.drawSingle(
        idx: Long,
        value: I,
        data: Map<Key<I, *>, List<PortValue<*>>>,
        isFocused: Boolean
    ) {
        val color = when ((slot.value() as? ImpulseData)?.isPositive) {
            null,
            true -> positiveColor
            false -> negativeColor
        }
        Offset(0f, slot.value()?.scalar?.toFloat() ?: 0f).let { offset ->
            val size = Size(0.85f / finalScale.x, offset.y / finalScale.y)
            if (size.width < 0 || size.height < 0) return
            val topLeft = offset.copy(
                x = plotRect.left + idx / finalScale.x - size.width / 2,
                y = plotRect.bottom - size.height
            )
            drawRect(color, topLeft, size, color.alpha, Fill)
        }
    }
}

open class ImpulseData(x: Number, height: Number, val isPositive: Boolean = true) : Output.Scalar(x, height)