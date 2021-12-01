package dev.zieger.plottingcompose

import androidx.compose.ui.geometry.Offset


interface PlotItem<out T : Any> {
    val x: Float
    val y: Map<Int, Float?>
    val extra: T

    val offset: Offset? get() = y.values.firstOrNull()?.let { Offset(x, it) }

    val yMin: Float? get() = y.values.filterNotNull().minOfOrNull { it }
    val yMax: Float? get() = y.values.filterNotNull().maxOfOrNull { it }

    var hasFocus: Boolean

    fun map(plot: SinglePlot): PlotItem<T> = copy(
        plot.toScene(this@PlotItem.x, 0f).x,
        this@PlotItem.y.entries.associate { (idx, value) -> idx to value?.let { v -> plot.toScene(0f, v).y } },
        extra, hasFocus
    )

    fun copy(
        x: Float = this.x, y: Map<Int, Float?> = this.y,
        extra: @UnsafeVariance T = this.extra, hasFocus: Boolean
    ): PlotItem<T>
}

class SimplePlotItem<out T : Any>(override val x: Float, vararg y: Float?, override val extra: T) : PlotItem<T> {

    companion object {
        operator fun invoke(x: Float, vararg y: Float?): SimplePlotItem<Unit> = SimplePlotItem(x, *y, extra = Unit)
    }

    override val y: Map<Int, Float?> = y.mapIndexed { idx, v -> idx to v }.toMap()
    override var hasFocus: Boolean = false

    override fun copy(x: Float, y: Map<Int, Float?>, extra: @UnsafeVariance T, hasFocus: Boolean) =
        SimplePlotItem(x, *y.values.toTypedArray(), extra = extra).apply { this.hasFocus = hasFocus }
}

