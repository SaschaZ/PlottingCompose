package dev.zieger.plottingcompose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dev.zieger.plottingcompose.indicators.candles.IndicatorCandle

interface FocusInfoHolder {

    val focusedItem: State<FocusedItem?>
}

class FocusInfoHolderImpl(
    transformationHolder: TransformationHolder,
    private val sizeHolder: SizeHolder,
    private val processingScopeHolder: ProcessingScopeHolder
) : FocusInfoHolder {

    private val removeMp = transformationHolder.onMousePositionChanged {
        focusedItem.value = mousePosition.value?.div(sizeHolder.chartRect)?.let { rm ->
            val idx = (rm.x * processingScopeHolder.chartData.size).toInt()
            processingScopeHolder.chartData.entries.toList().getOrNull(idx)?.key?.let { FocusedItem(it.idx, it.input) }
        }
    }

    override val focusedItem: MutableState<FocusedItem?> = mutableStateOf(null)

    fun release() {
        removeMp()
    }
}

data class FocusedItem(
    val idx: Long,
    val candle: IndicatorCandle
)