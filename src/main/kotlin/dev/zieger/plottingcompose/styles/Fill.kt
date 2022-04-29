package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.di.ChartScope

class Fill<I : Input>(
    private val slot: Slot<I, Output.Scalar>,
    private val color: Color = Color.Black.copy(alpha = 0.25f),
    private val upWards: Boolean = false
) : PlotStyle<I>(slot) {

    override fun ChartScope.drawSeries(data: List<Pair<InputContainer<I>, Map<Key<I, *>, List<PortValue<*>>>>>) {
        val checked = data.map { (x, d) -> Offset(x.idx.toFloat(), slot.value(d)?.scalar?.toFloat() ?: 0f).toScene() }
        if (checked.isEmpty()) return

        val path = Path()
        val startY = if (upWards) checked.maxOf { it.y } else checked.minOf { it.y }
        path.moveTo(checked.first().x, startY)
        checked.forEach { path.lineTo(it.x, it.y) }
        path.lineTo(checked.last().x, startY)

        drawPath(path, color, color.alpha, Fill)
    }
}