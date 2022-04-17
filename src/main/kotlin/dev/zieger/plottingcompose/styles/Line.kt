package dev.zieger.plottingcompose.styles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.scopes.IPlotDrawScope

open class Line<T : Input, D : Output>(
    private val slot: Slot<T, D>,
    private val color: Color = Color.White,
    private val width: Float = 3f,
    private val lineForData: (D) -> Output.Line?
) : PlotStyle<T>(slot) {
    override fun IPlotDrawScope<T>.drawSingle(
        idx: Long,
        value: T,
        data: Map<Key<T, *>, List<PortValue<*>>>,
        isFocused: Boolean
    ) {
        slot.value(data)?.let {
            lineForData(it)?.also { line ->
                drawLine(
                    color,
                    Offset(
                        (line.start.x / widthDivisor).toFloat(),
                        plotRect.bottom + line.start.y / heightDivisor.value.toFloat()
                    ).toScene(),
                    Offset(
                        (line.end.x / widthDivisor.toFloat()),
                        plotRect.bottom - line.end.y / heightDivisor.value.toFloat()
                    ).toScene(),
                    width,
                    alpha = color.alpha
                )
            }
        }
    }
}