package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class Line<T : Input>(
    private val slot: Slot<T, Output.Line>,
    private val color: Color = Color.Cyan,
    private val width: Float = 1f
) : PlotStyle<T>(slot) {
    override fun IPlotDrawScope<T>.drawSingle(
        idx: Long,
        value: T,
        data: Map<Key<T>, List<PortValue<*>>>,
        isFocused: Boolean
    ) {
        slot.value()?.let { line ->
            drawLine(
                color,
                Offset(line.start.x / widthDivisor, plotRect.bottom - line.start.y / heightDivisor.value.toFloat()),
                Offset(line.end.x / widthDivisor, plotRect.bottom - line.end.y / heightDivisor.value.toFloat()),
                width,
                alpha = color.alpha
            )
        }
    }
}