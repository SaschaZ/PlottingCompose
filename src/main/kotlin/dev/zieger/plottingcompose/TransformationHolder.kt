package dev.zieger.plottingcompose

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import dev.zieger.plottingcompose.definition.NumData
import dev.zieger.plottingcompose.definition.VisibleArea
import java.util.*
import kotlin.math.pow

interface TransformationHolder {

    val translation: State<Offset>
    val translationOffset: State<Offset>
    val translationRange: State<Pair<ClosedRange<Float>, ClosedRange<Float>>>
    val finalTranslation: Offset
    fun onTranslationChanged(listener: (Offset) -> Unit): () -> Unit
    fun setTranslation(offset: Offset)
    fun setTranslationOffset(offset: Offset)
    fun setTranslationRange(range: Pair<ClosedRange<Float>, ClosedRange<Float>>)

    val scale: State<Offset>
    val scaleOffset: State<Offset>
    val scaleRange: State<Pair<ClosedRange<Float>, ClosedRange<Float>>>
    val finalScale: Offset
    fun onScaleChanged(listener: (Offset) -> Unit): () -> Unit
    fun setScale(scale: Offset)
    fun setScaleOffset(offset: Offset)
    fun setScaleRange(range: Pair<ClosedRange<Float>, ClosedRange<Float>>)

    val mousePosition: State<Offset?>
    val mousePositionRelative: State<Offset?>
    fun onMousePositionChanged(listener: TransformationHolder.(Offset?) -> Unit): () -> Unit
    val scaleCenter: MutableState<Offset>
    val scaleCenterRelative: State<Offset>

    fun onTransformationChanged(listener: (translation: Offset, scale: Offset) -> Unit): () -> Unit {
        val t = onTranslationChanged { listener(it, scale.value) }
        val s = onScaleChanged { listener(translation.value, it) }
        return { t(); s() }
    }

    fun applyScaleRange(numData: Int, plotRect: Rect)
    fun applyTranslationRange(numData: Int, plotRect: Rect)
    fun applyScaleOffset(visibleArea: VisibleArea, numData: Int, width: Float)
    fun applyTranslationOffsetX(
        visibleArea: VisibleArea,
        rawXRange: ClosedRange<Float>,
        plotRect: Rect,
        numItems: Int,
        scale: Float
    )

    @Composable
    fun buildModifier(modifier: Modifier): Modifier
}

class ObservableStateOffset {

    private val _value = mutableStateOf(Offset.Zero)
    val value: State<Offset> = _value

    private val _valueOffset = mutableStateOf(Offset.Zero)
    val valueOffset: State<Offset> = _valueOffset

    private val _valueRange = mutableStateOf<Pair<ClosedRange<Float>, ClosedRange<Float>>>(0f..0f to 0f..0f)
    val valueRange: State<Pair<ClosedRange<Float>, ClosedRange<Float>>> = _valueRange

    private val valueListener = LinkedList<(Offset) -> Unit>()

    val finalValue: Offset
        get() = Offset(
            value.value.x + valueOffset.value.x,
            value.value.y + valueOffset.value.y
        )

    fun setValue(offset: Offset) {
        if (_value.value == offset) return
        _value.value = offset
        valueListener.forEach { it(offset) }
    }

    fun setValueOffset(offset: Offset) {
        _valueOffset.value = offset
    }

    fun setValueRange(range: Pair<ClosedRange<Float>, ClosedRange<Float>>) {
        _valueRange.value = range
    }

    fun onValueChanged(listener: (Offset) -> Unit): () -> Unit {
        valueListener += listener
        return { valueListener -= listener }
    }
}

class TransformationHolderImpl(
    private val sizeHolder: GlobalSizeHolder
) : TransformationHolder {

    private val translationValue = ObservableStateOffset()

    override val translation: State<Offset> = translationValue.value
    override val translationOffset: State<Offset> = translationValue.valueOffset
    override val translationRange: State<Pair<ClosedRange<Float>, ClosedRange<Float>>> = translationValue.valueRange
    override val finalTranslation: Offset
        get() = translationValue.finalValue

    override fun onTranslationChanged(listener: (Offset) -> Unit): () -> Unit =
        translationValue.onValueChanged(listener)

    override fun setTranslation(offset: Offset) = translationValue.setValue(offset)
    override fun setTranslationOffset(offset: Offset) = translationValue.setValueOffset(offset)
    override fun setTranslationRange(range: Pair<ClosedRange<Float>, ClosedRange<Float>>) =
        translationValue.setValueRange(range)

    private val scaleValue = ObservableStateOffset()

    override val scale: State<Offset> = scaleValue.value
    override val scaleOffset: State<Offset> = scaleValue.valueOffset
    override val scaleRange: State<Pair<ClosedRange<Float>, ClosedRange<Float>>> = scaleValue.valueRange
    override val finalScale: Offset
        get() = scaleValue.finalValue

    override fun onScaleChanged(listener: (Offset) -> Unit): () -> Unit = scaleValue.onValueChanged(listener)
    override fun setScale(scale: Offset) = scaleValue.setValue(scale)
    override fun setScaleOffset(offset: Offset) = scaleValue.setValueOffset(offset)
    override fun setScaleRange(range: Pair<ClosedRange<Float>, ClosedRange<Float>>) = scaleValue.setValueRange(range)

    private val mousePositionListener = LinkedList<TransformationHolder.(Offset?) -> Unit>()
    override val mousePosition: MutableState<Offset?> = mutableStateOf(null)
    override val mousePositionRelative: MutableState<Offset?> = mutableStateOf(null)

    private fun setMousePosition(mp: Offset?) {
        mousePosition.value = mp
        mousePositionRelative.value = mp?.div(sizeHolder.rootBorderRect)?.coerceIn(0f..1f, 0f..1f)
    }

    override fun onMousePositionChanged(listener: TransformationHolder.(Offset?) -> Unit): () -> Unit {
        mousePositionListener += listener
        return { mousePositionListener -= listener }
    }

    override val scaleCenter: MutableState<Offset> = mutableStateOf(Offset.Zero)
    override val scaleCenterRelative: MutableState<Offset> = mutableStateOf(Offset.Zero)

    override fun applyScaleRange(numData: Int, plotRect: Rect) {
        val xFactor = numData / plotRect.width
        setScaleRange(((xFactor * 0.01f)..(xFactor)) to (0.0001f..1000.0f))
//        println("scaleRange=${scaleRange.value}")
    }

    override fun applyTranslationRange(numData: Int, plotRect: Rect) {
        setTranslationRange(0f..(numData / finalScale.x - plotRect.width).coerceAtLeast(0.00001f) to 0f..0f)
//        println("transRangeX=${translationXRange.value}")
    }

    override fun applyScaleOffset(visibleArea: VisibleArea, numData: Int, width: Float) {
        setScaleOffset(
            when (val area = visibleArea.numData) {
                NumData.All -> scaleOffset.value.copy((numData - 1) / width)
                is NumData.Fixed -> scaleOffset.value.copy(area.numData / width)
            }
        )
//        println("offset=${scaleOffset.value}; num=$numData; width=$width")
    }

    override fun applyTranslationOffsetX(
        visibleArea: VisibleArea,
        rawXRange: ClosedRange<Float>,
        plotRect: Rect,
        numItems: Int,
        scale: Float
    ) {
        val x = /*scaleFocusedItemIdx.value?.let {
            val numVisible = numItems / finalScale.x
            val relativeVisibleIdx = (it.itemIdx - it.idxRange.start) / it.idxRange.range().toFloat()
            val visibleBeforeFocused = numVisible * relativeVisibleIdx// / scale.value.x
            (-it.itemX + visibleBeforeFocused.toFloat()) / scale.value.x.toFloat()
        } ?: */(-rawXRange.endInclusive + plotRect.width * visibleArea.relativeEnd)

        setTranslationOffset(Offset(x, 0f))
//        println("transOffX=${translationOffsetX.value}; scaledFocusedItemIdx=${scaleFocusedItemIdx.value}; width=${plotRect.width}; transx=${translation.value.x}; numItems=$numItems")
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun buildModifier(modifier: Modifier): Modifier = modifier
        .onSizeChanged { sizeHolder.chartSize.value = it }
        .scrollable(rememberScrollableState { delta ->
            processHorizontalScroll(delta)
            delta
        }, Orientation.Horizontal)
        .scrollable(rememberScrollableState { delta ->
            processVerticalScrolling(delta)
            delta
        }, Orientation.Vertical)
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consumeAllChanges()
                processDrag(dragAmount)
            }
        }.pointerMoveFilter(onExit = {
            mousePosition.value = null
            mousePositionListener.forEach { l -> l(null) }
            false
        }, onMove = {
            mousePosition.value = it
            mousePositionListener.forEach { l -> l(it) }
            false
        })

    private fun processVerticalScrolling(delta: Float) {
        val prevScale = scale.value
        setScale(Offset(scale.value.x.let { it - it * 0.01 * delta }
            .coerceIn(
                scaleRange.value.first.start / scaleOffset.value.x.toDouble()..
                        scaleRange.value.first.endInclusive / scaleOffset.value.x.toDouble()
            ), 1.0))
        if (scale.value.x.run { isNaN() || isInfinite() })
            setScale(prevScale)

        mousePosition.value?.also { mp ->
            scaleCenter.value = mp
            scaleCenterRelative.value = mp / sizeHolder.chartSize.value
        }

//        println("scale=${scale.value}; offset=${scaleOffset.value}; final=$finalScale")
    }

    private fun processHorizontalScroll(delta: Float) {
        setTranslation(translation.value.run {
            copy(x + delta * finalScale.x.toDouble().pow(0.01).toFloat() * 100)
        })
    }

    private fun processDrag(dragAmount: Offset) {
        setTranslation(translation.value.run { copy(x + dragAmount.x) })
    }
}

private fun Offset.coerceIn(
    xRange: ClosedFloatingPointRange<Float>,
    yRange: ClosedFloatingPointRange<Float>
): Offset = Offset(x.coerceIn(xRange), y.coerceIn(yRange))

operator fun Offset.div(rect: Rect): Offset =
    copy((x - rect.left) / rect.width, (y - rect.top) / rect.height)
