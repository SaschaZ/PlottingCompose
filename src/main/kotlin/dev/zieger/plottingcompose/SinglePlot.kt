package dev.zieger.plottingcompose

import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.zieger.plottingcompose.scopes.IPlotDrawScope
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine

data class SinglePlot(
    val scope: IPlotDrawScope,
    val main: Rect,
    val plot: Rect,
    val yLabel: Rect,
    val xLabel: Rect,
    val xTopTicks: Rect,
    val widthFactor: Float,
    val heightFactor: Float,
    val yTicks: List<Pair<Number, String>>,
    val xTicks: List<Pair<Number, String>>,
    val yLabelFontSize: Float,
    val xLabelFontSize: Float,
    val xStretch: MutableState<Float>,
    val xStretchCenter: MutableState<Offset>,
) {
    companion object {

        operator fun invoke(scope: IPlotDrawScope): SinglePlot? {
            val yTicks = scope.run { plotYTicks(scope.allY) }
            val xTicks = scope.run { plotXTicks(scope.allX) }
            val plotSize = scope.plotSize.value

            val main = scope.run {
                val left = horizontalPadding().value
                val top = verticalPadding().value
                val right =
                    plotSize.width - horizontalPadding().value - if (scope.drawYLabels) (plotTickLength().value + plotYLabelWidth().value) else 0f
                val bottom =
                    plotSize.height - verticalPadding().value - if (scope.drawXLabels) (plotTickLength().value + plotXLabelHeight().value) else 0f
                if (left > right || top > bottom) return null
                Rect(left, top, right, bottom)
            }
            val plot = scope.run {
                val left = main.left + horizontalPlotPadding().value
                val top = main.top + verticalPlotPadding().value
                val right = main.right - horizontalPlotPadding().value
                val bottom = main.bottom - verticalPlotPadding().value
                if (left > right || top > bottom) return null
                Rect(left, top, right, bottom)
            }
            val yLabel = scope.run {
                val left = main.right - plotTickLength().value / 2
                val top = main.top
                val right = plotSize.width - horizontalPadding().value
                val bottom = main.bottom
                if (left > right || top > bottom) return null
                Rect(left, top, right, bottom)
            }
            val xLabel = scope.run {
                val left = main.left
                val top = main.bottom - plotTickLength().value / 2
                val right = main.right
                val bottom = plotSize.height - verticalPadding().value
                if (left > right || top > bottom) return null
                Rect(left, top, right, bottom)
            }
            val xTopTicks = scope.run {
                val left = main.left
                val top = main.top - plotTickLength().value / 2
                val right = main.right
                val bottom = main.top + plotTickLength().value / 2
                if (left > right || top > bottom) return null
                Rect(left, top, right, bottom)
            }

            return SinglePlot(
                scope,
                main, plot, yLabel, xLabel, xTopTicks,
                plot.width / scope.allSeries.xWidth,
                plot.height / scope.allSeries.yHeight,
                yTicks, xTicks, yTicks.yLabelFontSize(scope), xTicks.xLabelFontSize(scope),
                scope.widthFactor, scope.widthFactorCenter
            )
        }

        private fun List<Pair<Number, String>>.yLabelFontSize(scope: IPlotDrawScope): Float {
            if (isEmpty()) return 0f
            fun labelWidth(fontSize: Float): Float {
                val font = Font(null, fontSize)
                return maxOf { (_, lbl) -> TextLine.make(lbl, font).width }
            }
            var fontSize = 30f
            do {
                val diff = scope.plotYLabelWidth(scope).value - labelWidth(fontSize--)
            } while (diff < 0f)
            return fontSize
        }

        private fun List<Pair<Number, String>>.xLabelFontSize(scope: IPlotDrawScope): Float {
            if (isEmpty()) return 0f
            fun labelHeight(fontSize: Float): Float {
                val font = Font(null, fontSize)
                return maxOf { (_, lbl) -> TextLine.make(lbl, font).height }
            }

            var fontSize = 30f
            do {
                val diff = scope.plotXLabelHeight(scope).value - labelHeight(fontSize--)
            } while (diff < 0f)
            return fontSize
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