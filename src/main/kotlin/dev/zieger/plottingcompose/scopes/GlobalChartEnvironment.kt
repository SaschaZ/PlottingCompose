package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import dev.zieger.plottingcompose.definition.Chart
import dev.zieger.plottingcompose.definition.NumData
import dev.zieger.plottingcompose.definition.VisibleArea
import dev.zieger.plottingcompose.div
import kotlin.math.pow

val <A, B> Pair<A, B>.x: A
    get() = first

val <A, B> Pair<A, B>.y: B
    get() = second

interface IGlobalChartEnvironment {
    val chartSize: MutableState<IntSize>
    val translation: MutableState<Offset>
    val translationXRange: MutableState<ClosedRange<Float>>
    val translationOffsetX: MutableState<Float>
    val scale: MutableState<Pair<Double, Double>>
    val scaleOffset: MutableState<Pair<Double, Double>>
    val scaleRange: MutableState<Pair<ClosedRange<Double>, ClosedRange<Double>>>
    val finalScale: Pair<Double, Double>
        get() = (scale.value.x * scaleOffset.value.x).coerceIn(scaleRange.value.first) to
                (scale.value.y * scaleOffset.value.y).coerceIn(scaleRange.value.second)
    val scaleCenterRelative: MutableState<Offset>
    val scaleCenter: Offset
        get() = scaleCenterRelative.value * chartSize.value
    val mousePosition: MutableState<Offset?>

    val focusedItemIdx: MutableState<FocusedInfo?>
    val scaleFocusedItemIdx: MutableState<FocusedInfo?>
    val yLabelWidth: MutableState<Double>

    fun reset() {
        chartSize.value = IntSize.Zero
        translation.value = Offset.Zero
        translationOffsetX.value = 0f
        scale.value = 1.0 to 1.0
        scaleOffset.value = 1.0 to 1.0
        scaleCenterRelative.value = Offset(0.5f, 0.5f)
        mousePosition.value = null
    }

    fun applyScaleRange(numData: Int, plotRect: Rect) {
        val xFactor = numData / plotRect.width.toDouble()
        scaleRange.value = ((xFactor * 0.01)..(xFactor)) to (0.0001..1000.0)
//        println("scaleRange=${scaleRange.value}")
    }

    fun applyTranslationRange(numData: Int, plotRect: Rect) {
        translationXRange.value =
            0f..(numData.toFloat() / finalScale.x.toFloat() - plotRect.width).coerceAtLeast(0.00001f)

//        println("transRangeX=${translationXRange.value}")
    }

    fun applyScaleOffset(visibleArea: VisibleArea, numData: Int, width: Float) {
        scaleOffset.value = when (val area = visibleArea.numData) {
            NumData.All -> scaleOffset.value.copy((numData - 1) / width.toDouble())
            is NumData.Fixed -> scaleOffset.value.copy(area.numData / width.toDouble())
        }

//        println("offset=${scaleOffset.value}; num=$numData; width=$width")
    }

    fun applyTranslationOffsetX(
        visibleArea: VisibleArea,
        rawXRange: ClosedRange<Float>,
        plotRect: Rect,
        numItems: Int,
        widthDivisor: Float
    ) {
        scaleOffset
        val x = /*scaleFocusedItemIdx.value?.let {
            val numVisible = numItems / finalScale.x
            val relativeVisibleIdx = (it.itemIdx - it.idxRange.start) / it.idxRange.range().toFloat()
            val visibleBeforeFocused = numVisible * relativeVisibleIdx// / scale.value.x
            (-it.itemX + visibleBeforeFocused.toFloat()) / scale.value.x.toFloat()
        } ?: */(-rawXRange.endInclusive + plotRect.width * visibleArea.relativeEnd)

        translationOffsetX.value = x

//        println("transOffX=${translationOffsetX.value}; scaledFocusedItemIdx=${scaleFocusedItemIdx.value}; width=${plotRect.width}; transx=${translation.value.x}; numItems=$numItems")
    }

    fun processVerticalScrolling(delta: Float) {
        val prevScale = scale.value
        scale.value =
            scale.value.x.let { it - it * 0.01 * delta }
                .coerceIn(
                    scaleRange.value.x.start / scaleOffset.value.first..
                            scaleRange.value.x.endInclusive / scaleOffset.value.first
                ) to 1.0
        if (scale.value.x.run { isNaN() || isInfinite() })
            scale.value = prevScale

        mousePosition.value?.also { mp -> scaleCenterRelative.value = mp / chartSize.value }
        focusedItemIdx.value?.also { item -> scaleFocusedItemIdx.value = item }

//        println("scale=${scale.value}; offset=${scaleOffset.value}; final=$finalScale")
    }

    fun processHorizontalScroll(delta: Float) {
        translation.value = translation.value.run {
            copy(x + delta * finalScale.x.pow(0.01).toFloat() * 100)
        }
    }

    fun processDrag(dragAmount: Offset) {
        translation.value = translation.value.run { copy(x + dragAmount.x) }
    }
}

fun Offset.coerceIn(xRange: ClosedRange<Float>, yRange: ClosedRange<Float>): Offset =
    Offset(-(-x).coerceIn(xRange), y.coerceIn(yRange))

private operator fun Offset.times(other: IntSize): Offset =
    Offset(x * other.width, y * other.height)

interface IChartEnvironment : IGlobalChartEnvironment {
    val translationOffsetY: MutableState<Float>
    val finalTranslation: Offset
        get() = (translation.value +
                Offset(translationOffsetX.value, translationOffsetY.value))
            .run { Offset(-(-x).coerceIn(translationXRange.value), y) }

    val heightDivisor: MutableState<Double>
    val xLabelHeight: MutableState<Double>

    override fun reset() {
        super.reset()

        translationOffsetY.value = 0f
    }

    fun applyTranslationOffsetY(chart: Chart<*>, visibleYPixelRange: ClosedFloatingPointRange<Double>, plotRect: Rect) {
        val relMargin = chart.margin.bottom(chartSize.value).value / chartSize.value.height
        translationOffsetY.value = visibleYPixelRange.start.toFloat() - plotRect.height * relMargin
//        println("transOffY=${translationOffsetY.value}")
    }
}

data class GlobalChartEnvironment(
    override val chartSize: MutableState<IntSize> = mutableStateOf(IntSize.Zero),
    override val translation: MutableState<Offset> = mutableStateOf(Offset.Zero),
    override val translationXRange: MutableState<ClosedRange<Float>> = mutableStateOf(0f..1f),
    override val translationOffsetX: MutableState<Float> = mutableStateOf(0f),
    override val scale: MutableState<Pair<Double, Double>> = mutableStateOf(1.0 to 1.0),
    override val scaleOffset: MutableState<Pair<Double, Double>> = mutableStateOf(1.0 to 1.0),
    override val scaleRange: MutableState<Pair<ClosedRange<Double>, ClosedRange<Double>>> = mutableStateOf(1.0..1.0 to 1.0..1.0),
    override val scaleCenterRelative: MutableState<Offset> = mutableStateOf(Offset(0.5f, 0.5f)),
    override val mousePosition: MutableState<Offset?> = mutableStateOf(null),
    override val focusedItemIdx: MutableState<FocusedInfo?> = mutableStateOf(null),
    override val scaleFocusedItemIdx: MutableState<FocusedInfo?> = mutableStateOf(null),
    override val yLabelWidth: MutableState<Double> = mutableStateOf(70.0) { new, cur -> new <= cur }
) : IGlobalChartEnvironment

data class ChartEnvironment(
    val globalChartEnvironment: IGlobalChartEnvironment,
    override val translationOffsetY: MutableState<Float> = mutableStateOf(0f),
    override val heightDivisor: MutableState<Double> = mutableStateOf(1.0),
    override val xLabelHeight: MutableState<Double> = mutableStateOf(20.0)
) : IChartEnvironment, IGlobalChartEnvironment by globalChartEnvironment {

    override fun reset() = super<IChartEnvironment>.reset()
}

data class FocusedInfo(
    val itemIdx: Long,
    val itemX: Float,
    val idxRange: ClosedRange<Int>
)

fun mutableStateOf(
    initial: Double,
    veto: (new: Double, cur: Double) -> Boolean = { _, _ -> false }
) = object : MutableState<Double> {

    private val internal = mutableStateOf(initial)

    override var value: Double
        get() = internal.value
        set(value) {
            if (!veto(value, internal.value))
                internal.value = value
        }

    override fun component1(): Double = value
    override fun component2(): (Double) -> Unit = { value = it }
}