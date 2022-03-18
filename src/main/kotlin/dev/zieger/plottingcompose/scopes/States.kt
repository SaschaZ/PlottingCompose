package dev.zieger.plottingcompose.scopes

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope

class States(private val scope: CoroutineScope) : IStates {

    override val heightDivisor: MutableState<Double> = mutableStateOf(1.0)
    override val focusedItemIdx: MutableState<FocusedInfo?> = mutableStateOf(null)
    override val xLabelHeight: MutableState<Double> = mutableStateOf(20.0)
    override val yLabelWidth: MutableState<Double> = mutableStateOf(70.0)
}

interface IStates {

    val heightDivisor: MutableState<Double>
    val focusedItemIdx: MutableState<FocusedInfo?>
    val yLabelWidth: MutableState<Double>
    val xLabelHeight: MutableState<Double>
}

data class FocusedInfo(
    val itemIdx: Int,
    val itemX: Float
)

fun mutableDoubleState(
    initial: Double,
    length: Int = 10
) = object : MutableState<Double> {

    private var previous = emptyList<Double>()
    private val internal = mutableStateOf(initial)

    override var value: Double
        get() = internal.value
        set(value) {
            when {
                value > this.value -> {
                    previous = listOf(value)
                }
                value < this.value -> {
                    previous = (previous + value).takeLast(length)
                }
            }
            internal.value = previous.average()
        }

    override fun component1(): Double = value
    override fun component2(): (Double) -> Unit = { value = it }
}