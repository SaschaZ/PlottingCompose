package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class States(
    globalStates: IGlobalStates
) : IStates, IGlobalStates by globalStates {

    override val heightDivisor: MutableState<Double> = mutableStateOf(1.0)
    override val xLabelHeight: MutableState<Double> = mutableStateOf(20.0)
}

interface IStates : IGlobalStates {

    val heightDivisor: MutableState<Double>
    val xLabelHeight: MutableState<Double>
}

data class GlobalStates(
    override val focusedItemIdx: MutableState<FocusedInfo?> = mutableStateOf(null),
    override val yLabelWidth: MutableState<Double> = mutableStateOf(70.0) { new, cur -> new <= cur }
) : IGlobalStates

interface IGlobalStates {
    val focusedItemIdx: MutableState<FocusedInfo?>
    val yLabelWidth: MutableState<Double>
}

data class FocusedInfo(
    val itemIdx: Long,
    val itemX: Float
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