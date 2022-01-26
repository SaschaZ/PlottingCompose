package dev.zieger.plottingcompose.styles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.zieger.plottingcompose.definition.InputContainer
import dev.zieger.plottingcompose.definition.Key
import dev.zieger.plottingcompose.definition.Port
import dev.zieger.plottingcompose.definition.Slot
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import dev.zieger.plottingcompose.scopes.ValueHolder

open class Line<T : InputContainer>(
    private val slot: Slot<Float, T>,
    private val color: Color = Color.Black,
    private val width: Float = 1f
) : PlotStyle<T>(slot) {

    override fun IPlotDrawScope<T>.drawSeries(data: Map<T, Map<Key<T>, Map<Port<*>, ValueHolder?>>>) {
        if (data.isEmpty()) return

        drawPath(Path().apply {
            data.map { (x, data) -> x.x.toFloat() / widthDivisor to (slot.value(data) ?: 0f) / heightDivisor }
                .forEach { (x, y) ->
                    when {
                        isEmpty -> moveTo(x, y)
                        else -> lineTo(x, y)
                    }
                }
        }, color, color.alpha, Stroke(width))
    }
}