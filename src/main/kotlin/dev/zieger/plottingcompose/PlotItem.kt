package dev.zieger.plottingcompose

import androidx.compose.ui.geometry.Offset

interface PlotItem<P : Position> {
    val position: P

    val yMin: Float get() = position.yRange.start
    val yMax: Float get() = position.yRange.endInclusive

    var hasFocus: Boolean

    fun toScene(plot: SinglePlot): PlotItem<Position.Scene> = ScenePlotItem(this, plot)
    fun toScreen(plot: SinglePlot): PlotItem<Position.Screen> = ScreenPlotItem(this, plot)
}

data class SimplePlotItem(override val position: Position.Raw) : PlotItem<Position.Raw> {
    constructor(x: Float, y: Float?, z: Int = 0, yRange: ClosedRange<Float> = (y ?: 0f)..(y ?: 0f)) : this(
        Position.Raw(Offset(x, y ?: 0f), yRange, y == null)
    )

    override var hasFocus = false
}

data class ScenePlotItem(
    override val position: Position.Scene,
    override var hasFocus: Boolean = false
) : PlotItem<Position.Scene> {
    constructor(item: PlotItem<*>, plot: SinglePlot) : this(item.position.scene(plot), item.hasFocus)
}

data class ScreenPlotItem(
    override val position: Position.Screen,
    override var hasFocus: Boolean = false
) : PlotItem<Position.Screen> {
    constructor(item: PlotItem<*>, plot: SinglePlot) : this(item.position.screen(plot), item.hasFocus)
}


