package dev.zieger.plottingcompose

import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine

data class SinglePlot(
    val scope: IPlotDrawScope,
    val main: Rect,
    val plot: Rect,
    val yLabel: Rect,
    val xLabel: Rect,
    val widthFactor: Float,
    val heightFactor: Float,
    val yTicks: List<Pair<Number, String>>,
    val xTicks: List<Pair<Number, String>>,
    val xStretch: MutableState<Float>,
    val xStretchCenter: MutableState<Offset>,
) {
    companion object {

        operator fun invoke(scope: IPlotDrawScope): SinglePlot? {
            val yTicks = scope.run { plotYTicks(scope.allItems) }
            val xTicks = scope.run { plotXTicks(scope.allItems) }
            val yLabelWidth = if (scope.drawYLabels) yTicks.yLabelWidth(scope) else 0.dp
            val xLabelHeight = if (scope.drawXLabels) xTicks.xLabelHeight(scope) else 0.dp
            val plotSize = scope.plotSize.value

            val main = scope.run {
                val left = horizontalPadding.value
                val top = verticalPadding.value
                val right =
                    plotSize.width - horizontalPadding.value - yLabelWidth.value - if (scope.drawYLabels) plotTickLength.value else 0f
                val bottom =
                    plotSize.height - verticalPadding.value - xLabelHeight.value - if (scope.drawXLabels) plotTickLength.value else 0f
                if (left > right || top > bottom) return null
                Rect(left, top, right, bottom)
            }
            val plot = scope.run {
                val left = main.left + horizontalPlotPadding.value
                val top = main.top + verticalPlotPadding.value
                val right = main.right - horizontalPlotPadding.value
                val bottom = main.bottom - verticalPlotPadding.value
                if (left > right || top > bottom) return null
                Rect(left, top, right, bottom)
            }
            val yLabel = scope.run {
                val left = main.right - plotTickLength.value / 2
                val top = main.top
                val right = plotSize.width - horizontalPadding.value
                val bottom = main.bottom
                if (left > right || top > bottom) return null
                Rect(left, top, right, bottom)
            }
            val xLabel = scope.run {
                val left = main.left
                val top = main.bottom - plotTickLength.value / 2
                val right = main.right
                val bottom = plotSize.height - verticalPadding.value
                if (left > right || top > bottom) return null
                Rect(left, top, right, bottom)
            }

            return SinglePlot(
                scope,
                main, plot, yLabel, xLabel,
                plot.width / scope.allSeries.xWidth,
                plot.height / scope.allSeries.yHeight,
                yTicks, xTicks, scope.widthFactor, scope.widthFactorCenter
            )
        }

        private fun List<Pair<Number, String>>.yLabelWidth(scope: IPlotDrawScope): Dp {
            val font = Font(null, scope.plotLabelFontSize)
            return maxOf { TextLine.make(it.second, font).width }.dp
        }

        private fun List<Pair<Number, String>>.xLabelHeight(scope: IPlotDrawScope): Dp {
            val font = Font(null, scope.plotLabelFontSize)
            return maxOf { TextLine.make(it.second, font).height }.dp
        }
    }

    fun toScene(x: Float, y: Float): Offset = Offset(
        plot.left + widthFactor * x * xStretch.value,
        plot.bottom - heightFactor * y
    )

    fun toScene(offset: Offset) = toScene(offset.x, offset.y)

    fun toScreen(x: Float, y: Float): Offset = toScreen(Offset(x, y))

    fun toScreen(offset: Offset) = scope.run {
        try {
            scaleCenter.value + (offset - scaleCenter.value - translation.value) / scale.value - translationOffset.value
        } catch (t: Throwable) {
            Offset.Zero
        }
    }
}