package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import dev.zieger.plottingcompose.mutableDoubleStateAnimated
import kotlinx.coroutines.CoroutineScope

class States(private val scope: CoroutineScope) : IStates {

    override val heightDivisor: MutableState<Double> = mutableDoubleStateAnimated(1.0, scope)
    override val translationOffsetY: MutableState<Double> = mutableDoubleStateAnimated(0.0, scope)
    override val focusedItemIdx: MutableState<FocusedInfo?> = mutableStateOf(null)
}

interface IStates {

    val heightDivisor: MutableState<Double>
    val translationOffsetY: MutableState<Double>
    val focusedItemIdx: MutableState<FocusedInfo?>
}

data class FocusedInfo(
    val itemIdx: Int,
    val itemX: Float
)