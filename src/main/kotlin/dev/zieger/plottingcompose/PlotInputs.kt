package dev.zieger.plottingcompose

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.mouse.MouseScrollOrientation
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import dev.zieger.plottingcompose.scopes.IPlotParameterScope

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.plotInputs(scope: IPlotParameterScope): Modifier = scope.run {
    mouseScrollFilter { event, _ ->
        when (event.orientation) {
            MouseScrollOrientation.Vertical -> ((event.delta as? MouseScrollUnit.Line)?.value
                ?: (event.delta as? MouseScrollUnit.Page)?.value)?.also { delta ->
                when (scrollAction) {
                    ScrollAction.X_TRANSLATION ->
                        translation.value = translation.value.let { it.copy(it.x + delta) }
                    ScrollAction.WIDTH_FACTOR -> {
                        widthFactor.value = (widthFactor.value + widthFactor.value * delta / 20).coerceAtLeast(1f)
                        mousePosition.value?.also { pos ->
                            val prev = widthFactorCenter.value
                            widthFactorCenter.value =
                                widthFactorCenter.value + (pos - widthFactorCenter.value - translation.value) / widthFactor.value
                            println("mouse: $pos; widthFactorCenter=$prev -> ${widthFactorCenter.value}; factor=${widthFactor.value}")
                        }
                    }
                    ScrollAction.SCALE -> {
                        if (!enableScale) return@mouseScrollFilter false
                        val newScale = (scale.value + 0.25f / delta).coerceAtLeast(1f)
                        when {
                            newScale < scale.value -> {
                                val diff = scale.value - newScale
                                val percent = diff / (scale.value - 1f)
                                translation.value = translation.value * (1f - percent)
                            }
                        }
                        scale.value = newScale
                        mousePosition.value?.also { pos ->
                            val prev = scaleCenter.value
                            scaleCenter.value =
                                scaleCenter.value + (pos - scaleCenter.value - finalTranslation(scope) + translationOffset.value) / scale.value
                            println("mouse: $pos; scaleCenter=$prev -> ${scaleCenter.value}; scale=${scale.value}")
                        }
                    }
                }
            }
            MouseScrollOrientation.Horizontal -> ((event.delta as? MouseScrollUnit.Line)?.value
                ?: (event.delta as? MouseScrollUnit.Page)?.value)?.also { delta ->
                println("horizontal delta=$delta")
                translation.value = translation.value.let { it.copy(it.x + delta) }
            }
        }
        true
    }.pointerInput(Unit) {
        detectDragGestures { _, dragAmount ->
            if (!enableTranslation) return@detectDragGestures
            translation.value = (translation.value + dragAmount)
        }
    }.pointerMoveFilter(onMove = {
        mousePosition.value = it
        false
    }, onExit = {
        mousePosition.value = null
        false
    })
}