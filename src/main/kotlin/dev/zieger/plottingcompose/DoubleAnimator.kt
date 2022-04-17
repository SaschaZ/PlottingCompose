package dev.zieger.plottingcompose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import dev.zieger.utils.time.ITimeSpan
import dev.zieger.utils.time.TimeStamp
import dev.zieger.utils.time.millis
import kotlinx.coroutines.*

open class DoubleAnimator(
    initial: Double,
    private val scope: CoroutineScope,
    protected val defaultDuration: ITimeSpan = 300.millis,
    private val interpolator: Interpolator = { linear(t = it) },
    private val block: AnimationScope.(Double) -> Unit
) {

    private var currentValue: Double = initial
    private var currentTaretValue: Double = initial
    private var currentAnimation: Job? = null

    fun animateTo(value: Double, duration: ITimeSpan = defaultDuration) {
        if (value == currentTaretValue) return

        currentTaretValue = value
        currentAnimation?.cancel()
        currentAnimation = scope.launch {
            val startValue = currentValue
            val startedAt = TimeStamp()
            block(AnimationScope(0.0, duration, 0.millis), startValue)
            do {
                delay(33)
                val runtime = TimeStamp() - startedAt
                val relT = (runtime.divDouble(duration)).coerceIn(0.0..1.0)
                currentValue = startValue + (value - startValue) * interpolator(relT)
                block(AnimationScope(relT, duration, runtime), currentValue)
            } while (runtime < duration && isActive)
        }
    }

    data class AnimationScope(val relT: Double, val duration: ITimeSpan, val runtime: ITimeSpan)
}

fun mutableDoubleStateAnimated(
    initial: Double,
    scope: CoroutineScope,
    defaultDuration: ITimeSpan = 600.millis,
    interpolator: Interpolator = { linear(t = it) }
): MutableState<Double> = object : MutableState<Double> {

    private val internal = mutableStateOf(initial)
    private val animator = DoubleAnimator(initial, scope, defaultDuration, interpolator) { internal.value = it }

    override var value: Double
        get() = internal.value
        set(value) = animator.animateTo(value)

    override fun component1(): Double = value
    override fun component2(): (Double) -> Unit = { value = it }
}