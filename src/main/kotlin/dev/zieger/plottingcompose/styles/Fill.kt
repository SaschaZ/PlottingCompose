package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import dev.zieger.plottingcompose.definition.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import dev.zieger.plottingcompose.scopes.ValueHolder

class Fill<T : InputContainer>(
    private val slot: Slot<Float, T>,
    private val color: Color = Color.Black.copy(alpha = 0.25f),
    private val upWards: Boolean = false
) : PlotStyle<T>(slot) {

    override fun IPlotDrawScope<T>.drawSeries(data: Map<T, Map<Key<T>, Map<Port<*>, ValueHolder?>>>) {
        val checked = data.map { (x, d) -> Offset(x.x.toFloat(), slot.value(d) ?: 0f).toScene() }
        if (checked.isEmpty()) return

        val path = Path()
        val startY = if (upWards) checked.maxOf { it.y } else checked.minOf { it.y }
        path.moveTo(checked.first().x, startY)
        checked.forEach { path.lineTo(it.x, it.y) }
        path.lineTo(checked.last().x, startY)

        drawPath(path, color, color.alpha, Fill)
    }
}