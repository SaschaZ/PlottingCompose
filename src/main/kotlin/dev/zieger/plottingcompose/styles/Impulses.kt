package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import dev.zieger.plottingcompose.definition.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import dev.zieger.plottingcompose.scopes.ValueHolder

class Impulses<T : InputContainer>(
    private val slot: Slot<Float, T>,
    private val color: Color = Color.Cyan
) : PlotStyle<T>(slot) {
    override fun IPlotDrawScope<T>.drawSingle(value: T, data: Map<Key<T>, Map<Port<*>, ValueHolder?>>) {
        Offset(value.x.toFloat(), slot.value(data) ?: 0f).toScene().let { offset ->
            val size = Size(40 / widthDivisor * xStretchFactor.value, offset.y)
            if (size.width < 0 || size.height < 0) return
            val topLeft = offset.copy(x = offset.x - size.width / 2, y = plotRect.height - offset.y)
            drawRect(color, topLeft, size, color.alpha, Fill)
        }
    }
}