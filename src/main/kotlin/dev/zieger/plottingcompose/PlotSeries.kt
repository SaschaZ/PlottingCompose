package dev.zieger.plottingcompose

class PlotSeries<T : PlotItem>(
    val items: List<PlotSeriesItem<T>>,
    vararg style: PlotStyle.SeriesPlotStyle<T>
) {
    val xRange = items.minOf { it.item.x }..items.maxOf { it.item.x }
    val xWidth = xRange.run { endInclusive - start }
    val yRange = items.filterNot { it.yMin == null || it.yMax == null }.run { minOf { it.yMin!! }..maxOf { it.yMax!! } }
    val yHeight = yRange.run { endInclusive - start }

    val styles = style.toList()
    val z: List<Int> = styles.flatMap { it.z }

    init {
        items.forEach { it.parent = this }
    }
}

val List<PlotSeries<*>>.xRange get() = minOf { it.xRange.start }..maxOf { it.xRange.endInclusive }
val List<PlotSeries<*>>.xWidth get() = xRange.run { endInclusive - start }
val List<PlotSeries<*>>.yRange get() = minOf { it.yRange.start }..maxOf { it.yRange.endInclusive }
val List<PlotSeries<*>>.yHeight get() = yRange.run { endInclusive - start }

fun <T : PlotItem> List<PlotSeriesItem<T>>.toSeries(vararg style: PlotStyle.SeriesPlotStyle<T>) =
    PlotSeries(this, *style)

open class PlotSeriesItem<out T : PlotItem>(
    val item: T,
    vararg style: PlotStyle.SinglePlotStyle<T>,
) {

    companion object {

        operator fun invoke(x: Number, y: List<Number?>, vararg style: PlotStyle.SinglePlotStyle<SimplePlotItem>) =
            PlotSeriesItem(SimplePlotItem(x.toFloat(), *y.map { it?.toFloat() }.toTypedArray()), *style)
    }

    lateinit var parent: PlotSeries<@UnsafeVariance T>

    val styles = style.toList()
    val z: List<Int> = styles.flatMap { it.z }

    open val yMin: Float? = item.y.values.filterNotNull().ifEmpty { null }?.minOf { it }
    open val yMax: Float? = item.y.values.filterNotNull().ifEmpty { null }?.maxOf { it }
}
