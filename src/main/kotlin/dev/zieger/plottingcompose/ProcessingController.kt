package dev.zieger.plottingcompose

import androidx.compose.ui.geometry.Offset
import dev.zieger.utils.time.parse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ProcessingController(
    private val processingScopeHolder: GlobalProcessingScopeHolder,
    private val transformationHolder: TransformationHolder,
    private val sizeHolder: GlobalSizeHolder,
    private val scope: CoroutineScope
) {

    fun control() {
        transformationHolder.onTransformationChanged { translation, scale ->
            onTransformationChanged(translation, scale)
        }
    }

    private fun onTransformationChanged(translation: Offset, scale: Offset) {
        scope.launch {
            processingScopeHolder.processRange("1.10.2021".parse().."3.10.2021".parse())
        }
    }
}