package dev.zieger.plottingcompose

import androidx.compose.ui.geometry.Offset

sealed class Position {

    abstract val offset: Offset
    abstract val yRange: ClosedRange<Float>
    abstract val isYEmpty: Boolean

    open fun scene(plot: SinglePlot): Scene = Scene(
        plot.toScene(offset),
        plot.toScene(offset.copy(y = yRange.start)).y..
                plot.toScene(offset.copy(y = yRange.endInclusive)).y,
        isYEmpty
    )

    open fun screen(plot: SinglePlot): Screen = Screen(
        plot.toScreen(offset),
        plot.toScreen(offset.copy(y = yRange.start)).y..
                plot.toScreen(offset.copy(y = yRange.endInclusive)).y,
        isYEmpty
    )

    data class Raw(
        override val offset: Offset,
        override val yRange: ClosedRange<Float> = offset.y..offset.y,
        override val isYEmpty: Boolean = false
    ) : Position() {
        override fun screen(plot: SinglePlot): Screen = scene(plot).run { Screen(offset, yRange, isYEmpty) }
    }

    data class Scene(
        override val offset: Offset,
        override val yRange: ClosedRange<Float> = offset.y..offset.y,
        override val isYEmpty: Boolean = false
    ) : Position() {
        override fun scene(plot: SinglePlot): Scene = this
    }

    data class Screen(
        override val offset: Offset,
        override val yRange: ClosedRange<Float> = offset.y..offset.y,
        override val isYEmpty: Boolean = false
    ) : Position() {
        override fun screen(plot: SinglePlot): Screen = this
    }
}