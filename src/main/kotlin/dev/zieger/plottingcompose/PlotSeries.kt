package dev.zieger.plottingcompose

class PlotSeries<T : PlotItem<Position.Raw>>(
    val items: List<SeriesItem<T>>,
    vararg style: PlotStyle<*>
) {
    val xRange = items.minOf { it.data.position.offset.x }..items.maxOf { it.data.position.offset.x }
    val xWidth = xRange.run { endInclusive - start }
    val yRange = items.minOf { it.data.yMin }..items.maxOf { it.data.yMax }
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

fun <T : PlotItem<Position.Raw>> List<SeriesItem<T>>.toSeries(vararg style: PlotStyle<*>) = PlotSeries(this, *style)

open class SeriesItem<out T : PlotItem<Position.Raw>>(
    val data: T
) {

    companion object {

        operator fun invoke(x: Number, y: Number?) = SeriesItem(SimplePlotItem(x.toFloat(), y?.toFloat()))
    }

    lateinit var parent: PlotSeries<*>

    open val yMin: Number = data.yMin
    open val yMax: Number = data.yMax
}
