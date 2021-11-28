package dev.zieger.plottingcompose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

open class ListeningMutableState<T>(initial: T, private val onChanged: MutableStateScope<T>.(T) -> Unit = {}) :
    MutableState<T> {
    private val internal = mutableStateOf(initial)

    override var value: T
        get() = internal.value
        set(value) {
            val previous = internal.value
            internal.value = value
            onChanged(MutableStateScope(previous), value)
        }

    override fun component1(): T = value
    override fun component2(): (T) -> Unit = { value = it }
}

data class MutableStateScope<T>(val previous: T?)
