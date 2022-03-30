package dev.zieger.plottingcompose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.singleWindowApplication
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.processor.ProcessingScope
import dev.zieger.plottingcompose.processor.ProcessingUnit
import dev.zieger.plottingcompose.styles.Fill
import kotlinx.coroutines.flow.asFlow
import kotlin.math.pow

object Main2 {

    @JvmStatic
    fun main(args: Array<String>) = singleWindowApplication {
        val data = remember { (-300..300 step 1).map { InputData(it, it.toDouble().pow(3)) }.asFlow() }
        val definition = remember {
            ChartDefinition(
                Chart(
                    Fill(TestProcessor.key() with TestProcessor.TEST_PORT, Color.Cyan, true)
                ),
                visibleArea = VisibleArea(0.8f, NumData.Fixed(300))
            )
        }
        MultiChart(definition, data, Modifier.fillMaxSize())
    }

    class TestProcessor() : ProcessingUnit<InputData>(key(), listOf(TEST_PORT)) {

        companion object {
            fun key() = Key("TestProcessor", Unit) { TestProcessor() }
            val TEST_PORT = Port<Output.Scalar>("Test")
        }

        override suspend fun ProcessingScope<InputData>.process() {
            set(TEST_PORT, Output.Scalar(input.x, input.y))
        }
    }
}

data class InputData(override val x: Number, val y: Double) : Input