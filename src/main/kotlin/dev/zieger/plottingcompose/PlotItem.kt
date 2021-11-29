package dev.zieger.plottingcompose

import androidx.compose.ui.geometry.Offset

interface PlotItem {
    val x: Float
    val y: Map<Int, Float?>

    val offset: Offset? get() = y.values.firstOrNull()?.let { Offset(x, it) }

    val yMin: Float? get() = y.values.filterNotNull().minOfOrNull { it }
    val yMax: Float? get() = y.values.filterNotNull().maxOfOrNull { it }

    var hasFocus: Boolean

    fun map(plot: SinglePlot): PlotItem = copy(
        plot.toScene(this@PlotItem.x, 0f).x,
        this@PlotItem.y.entries.associate { (idx, value) -> idx to value?.let { v -> plot.toScene(0f, v).y } },
        hasFocus
    )

    fun copy(x: Float = this.x, y: Map<Int, Float?> = this.y, hasFocus: Boolean): PlotItem
}

class SimplePlotItem(override val x: Float, vararg y: Float?) : PlotItem {
    override val y: Map<Int, Float?> = y.mapIndexed { idx, v -> idx to v }.toMap()
    override var hasFocus: Boolean = false

    override fun copy(x: Float, y: Map<Int, Float?>, hasFocus: Boolean) = SimplePlotItem(x, *y.values.toTypedArray())
        .apply { this.hasFocus = hasFocus }
}

