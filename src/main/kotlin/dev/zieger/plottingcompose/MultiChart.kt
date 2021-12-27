package dev.zieger.plottingcompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import dev.zieger.plottingcompose.definition.ChartDefinition
import dev.zieger.plottingcompose.definition.units
import dev.zieger.plottingcompose.processor.Processor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun <T : Any> MultiChart(
    definition: ChartDefinition<T>,
    input: Flow<T>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    remember {
        scope.launch {
            Processor(definition.units()).process(input).collect { s ->

            }
        }
    }
}