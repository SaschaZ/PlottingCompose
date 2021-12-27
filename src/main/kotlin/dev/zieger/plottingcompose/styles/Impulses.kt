package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import dev.zieger.plottingcompose.SinglePlot
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.definition.Value
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

class Impulses(
    private val slot: Slot<Float>,
    private val color: Color = Color.Cyan
) : PlotStyle(slot) {
    override fun IPlotDrawScope.drawSingle(x: Long, data: Map<Key, Map<Port<*>, Value?>>, plot: SinglePlot) {
        plot.toScene(x.toFloat(), slot.float(data) ?: 0f).let { offset ->
            val size = Size(40 * plot.widthFactor * widthFactor.value, offset.y)
            if (size.width < 0 || size.height < 0) return
            val topLeft = offset.copy(x = offset.x - size.width / 2 * plot.widthFactor, y = plot.plot.height - offset.y)
            drawRect(color, topLeft, size, color.alpha, Fill)
        }
    }
}