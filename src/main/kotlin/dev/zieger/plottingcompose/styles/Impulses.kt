package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

class Impulses<I : Input>(
    private val slot: Slot<I, Output.Scalar>,
    private val color: Color = Color.Cyan
) : PlotStyle<I>(slot) {
    override fun IPlotDrawScope<I>.drawSingle(value: I, data: Map<Key<I>, List<PortValue<*>>>) {
        Offset(value.x.toFloat(), slot.value(data)?.scalar?.toFloat() ?: 0f).toScene().let { offset ->
            val size = Size(40 / widthDivisor * xStretchFactor.value, offset.y)
            if (size.width < 0 || size.height < 0) return
            val topLeft = offset.copy(x = offset.x - size.width / 2, y = plotRect.height - offset.y)
            drawRect(color, topLeft, size, color.alpha, Fill)
        }
    }
}