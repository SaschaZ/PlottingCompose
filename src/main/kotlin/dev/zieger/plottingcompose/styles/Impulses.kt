package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

class Impulses<I : Input>(
    private val slot: Slot<I, out Output.Scalar>,
    private val positiveColor: Color = Color.Green,
    private val negativeColor: Color = Color.Red
) : PlotStyle<I>(slot) {
    override fun IPlotDrawScope<I>.drawSingle(
        idx: Long,
        value: I,
        data: Map<Key<I>, List<PortValue<*>>>,
        isFocused: Boolean
    ) {
        val color = when ((slot.value(data) as? ImpulseData)?.isPositive) {
            null,
            true -> positiveColor
            false -> negativeColor
        }
        Offset(0f, slot.value(data)?.scalar?.toFloat() ?: 0f).let { offset ->
            val size = Size(0.85f / widthDivisor, offset.y / heightDivisor.value.toFloat())
            if (size.width < 0 || size.height < 0) return
            val topLeft = offset.copy(
                x = plotRect.left + idx / widthDivisor - size.width / 2,
                y = plotRect.bottom - size.height
            )
            drawRect(color, topLeft, size, color.alpha, Fill)
        }
    }
}

open class ImpulseData(x: Number, height: Number, val isPositive: Boolean = true) : Output.Scalar(x, height)