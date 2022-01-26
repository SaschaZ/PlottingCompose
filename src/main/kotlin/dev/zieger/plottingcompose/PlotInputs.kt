//package dev.zieger.plottingcompose
//
//import androidx.compose.foundation.gestures.detectDragGestures
//import androidx.compose.ui.ExperimentalComposeUiApi
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.geometry.Rect
//import androidx.compose.ui.input.mouse.MouseScrollOrientation
//import androidx.compose.ui.input.mouse.MouseScrollUnit
//import androidx.compose.ui.input.mouse.mouseScrollFilter
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.input.pointer.pointerMoveFilter
//
//@OptIn(ExperimentalComposeUiApi::class)
//fun Modifier.plotInputs(scope: IPlotParameterScope): Modifier = scope.run {
//    mouseScrollFilter { event, _ ->
//        when (event.orientation) {
//            MouseScrollOrientation.Vertical -> ((event.delta as? MouseScrollUnit.Line)?.value
//                ?: (event.delta as? MouseScrollUnit.Page)?.value)?.also { delta ->
//                when (scrollAction) {
//                    ScrollAction.X_TRANSLATION ->
//                        translation.value = translation.value.let { it.copy(it.x + delta * 4) }
//                    ScrollAction.WIDTH_FACTOR -> {
//                        val newXStretch = (widthFactor.value + widthFactor.value * delta / 20).coerceAtLeast(1f)
//                        when {
//                            newXStretch < widthFactor.value -> {
//                                val diff = widthFactor.value - newXStretch
//                                val percent = diff / (widthFactor.value - 1f)
//                                translation.value = translation.value * (1f - percent)
//                            }
//                        }
//                        widthFactor.value = newXStretch
//                        mousePosition.value?.also { pos ->
//                            val prev = widthFactorCenter.value
//                            widthFactorCenter.value = widthFactorCenter.value +
//                                    (pos - widthFactorCenter.value - translation.value - translationOffset.value) / widthFactor.value
//                            println("mouse: $pos; widthFactorCenter=$prev -> ${widthFactorCenter.value}; factor=${widthFactor.value}")
//                        }
//                    }
//                    ScrollAction.SCALE -> {
//                        if (!enableScale) return@mouseScrollFilter false
//                        val newScale = (scale.value + 0.25f / delta).coerceAtLeast(1f)
//                        when {
//                            newScale < scale.value -> {
//                                val diff = scale.value - newScale
//                                val percent = diff / (scale.value - 1f)
//                                translation.value = translation.value * (1f - percent)
//                            }
//                        }
//                        scale.value = newScale
//                        mousePosition.value?.also { pos ->
//                            val prev = scaleCenter.value
//                            scaleCenter.value =
//                                scaleCenter.value + (pos - scaleCenter.value - finalTranslation(scope) + translationOffset.value) / scale.value
//                            println("mouse: $pos; scaleCenter=$prev -> ${scaleCenter.value}; scale=${scale.value}")
//                        }
//                    }
//                }
//            }
//            MouseScrollOrientation.Horizontal -> ((event.delta as? MouseScrollUnit.Line)?.value
//                ?: (event.delta as? MouseScrollUnit.Page)?.value)?.also { delta ->
//                println("horizontal delta=$delta")
//                translation.value = translation.value.let { it.copy(it.x + delta * 4) }
//            }
//        }
//        true
//    }.pointerInput(Unit) {
//        var isValidDrag = false
//        detectDragGestures(onDragStart = {
//            val validRect = Rect(
//                horizontalPadding().value + 2, verticalPadding().value + 3,
//                plotSize.value.width - horizontalPadding().value - if (drawYLabels) plotYLabelWidth().value else 0f - 2,
//                plotSize.value.height - verticalPadding().value - if (drawXLabels) plotXLabelHeight().value else 0f - 2
//            )
//            isValidDrag = it inside validRect
//        }) { _, dragAmount ->
//            if (!enableTranslation || !isValidDrag) return@detectDragGestures
//            translation.value = (translation.value + dragAmount)
//        }
//    }.pointerMoveFilter(onMove = {
//        mousePosition.value = it
//        false
//    }, onExit = {
//        mousePosition.value = null
//        false
//    })
//}
//
//private infix fun Offset.inside(rect: Rect): Boolean =
//    x in rect.left..rect.right && y in rect.top..rect.bottom
