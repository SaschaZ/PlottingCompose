package dev.zieger.plottingcompose.styles

import androidx.compose.ui.graphics.Color
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Output
import dev.zieger.plottingcompose.definition.PortValue
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.di.ChartScope

open class Line<T : Input, D : Output>(
    private val slot: Slot<T, D>,
    private val color: Color = Color.White,
    private val width: Float = 3f,
    private val lineForData: (D) -> Output.Line?
) : PlotStyle<T>(slot) {
    override fun ChartScope.drawSingle(
        idx: Long,
        value: T,
        data: Map<Key<T, *>, List<PortValue<*>>>,
        isFocused: Boolean
    ) {
        slot.value(data)?.let {
            lineForData(it)?.also { line ->
                drawLine(
                    color,
                    line.start.toScene(),
                    line.end.toScene(),
                    width,
                    alpha = color.alpha
                )
            }
        }
    }
}