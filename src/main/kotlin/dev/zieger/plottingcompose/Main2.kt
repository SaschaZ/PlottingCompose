package dev.zieger.plottingcompose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.singleWindowApplication
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.processor.processorOf
import dev.zieger.plottingcompose.styles.Dot
import dev.zieger.plottingcompose.styles.Fill
import dev.zieger.plottingcompose.styles.Label
import dev.zieger.plottingcompose.styles.whenFocused
import kotlinx.coroutines.flow.asFlow
import kotlin.math.pow

object Main2 {

    @JvmStatic
    fun main(args: Array<String>) = singleWindowApplication {
        val data = remember { (-2000..3000 step 1).map { InputData(it, it.toDouble().pow(3)) }.asFlow() }
        val definition = remember {
            val processor = processorOf<InputData>(label = {
                Output.Label(it.x, it.y, "${it.x}")
            }) { it.y }
            ChartDefinition(
                Chart(
                    Fill(processor.valueSlot(), Color.Cyan, true),
                    Label(processor.valueSlot(), processor.labelSlot()).whenFocused(),
                    Dot(processor.valueSlot(), Color.Red, 20f).whenFocused(),
                    margin = Margin(0f, 0f)
                ),
                visibleArea = VisibleArea(1f, NumData.All)
            )
        }
        MultiChart(definition, data, Modifier.fillMaxSize())
    }

    data class InputData(override val x: Number, val y: Double) : Input
}