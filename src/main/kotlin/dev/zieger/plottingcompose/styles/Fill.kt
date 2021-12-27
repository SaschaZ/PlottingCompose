package dev.zieger.plottingcompose.styles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill

import dev.zieger.plottingcompose.SinglePlot
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.definition.Value
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

class Fill(
    private val slot: Slot<Float>,
    private val color: Color = Color.Black.copy(alpha = 0.25f),
    private val upWards: Boolean = false
) : PlotStyle(slot) {

    override fun IPlotDrawScope.drawSeries(data: Map<Long, Map<Key, Map<Port<*>, Value?>>>, plot: SinglePlot) {
        val checked = data.map { (x, d) -> plot.toScene(x.toFloat(), slot.float(d) ?: 0f) }
        if (checked.isEmpty()) return

        val path = Path()
        val startY = if (upWards) checked.maxOf { it.y } else checked.minOf { it.y }
        path.moveTo(checked.first().x, startY)
        checked.forEach { path.lineTo(it.x, it.y) }
        path.lineTo(checked.last().x, startY)

        drawPath(path, color, color.alpha, Fill)
    }
}