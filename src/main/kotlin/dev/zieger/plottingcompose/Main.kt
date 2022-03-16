package dev.zieger.plottingcompose

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.zieger.plottingcompose.definition.Chart
import dev.zieger.plottingcompose.definition.ChartDefinition
import dev.zieger.plottingcompose.definition.with
import dev.zieger.plottingcompose.indicators.AverageType
import dev.zieger.plottingcompose.indicators.BollingerBands
import dev.zieger.plottingcompose.indicators.Ohcl
import dev.zieger.plottingcompose.styles.CandleSticks
import dev.zieger.plottingcompose.styles.FillBetween
import dev.zieger.plottingcompose.styles.Line
import dev.zieger.plottingcompose.styles.SingleFocusable
import kotlinx.coroutines.flow.asFlow
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextInt

fun main() = application {
    MaterialTheme(MaterialTheme.colors.copy(background = Color.Black, onBackground = Color.White)) {
        fun randomOhcl(time: Long, lastClose: Double? = null): Ohcl.Companion.Ohcl {
            val open = lastClose ?: (Random.nextDouble() * 20_000 + 100_000.0)
            val close =
                open + Random.nextDouble() * 10_000 * (if (Random.nextBoolean()) 1 else -1) * when (Random.nextInt(1..20)) {
                    20 -> 5
                    else -> 1
                }
            val high = max(open, close) + Random.nextDouble() * 5_000.0
            val low = min(open, close) - Random.nextDouble() * 5_000.0
            return Ohcl.Companion.Ohcl(
                open, high, close, low, Random.nextLong().absoluteValue % 2000L, time * 60
            )
        }

        val ohcl = remember {
            var lastClose: Double? = null
            (0..50000).map { idx ->
                randomOhcl(idx.toLong(), lastClose).also { c -> lastClose = c.close }
            }.asFlow()
        }

        val ctrlPressed: MutableState<Boolean> = remember { mutableStateOf(false) }
        val shiftPressed: MutableState<Boolean> = remember { mutableStateOf(false) }
        val altPressed: MutableState<Boolean> = remember { mutableStateOf(false) }
        Window(onCloseRequest = ::exitApplication, onKeyEvent = {
            ctrlPressed.value = it.isCtrlPressed
            shiftPressed.value = it.isShiftPressed
            altPressed.value = it.isAltPressed
            false
        }, undecorated = true) {
            MultiChart(
                ChartDefinition(
                    Chart(
                        Line(
                            BollingerBands.key(20, 2.0, AverageType.SMA) with BollingerBands.HIGH,
                            Color.Yellow.copy(alpha = 0.5f), 1f
                        ),
                        Line(
                            BollingerBands.key(20, 2.0, AverageType.SMA) with BollingerBands.MID,
                            Color.Yellow.copy(alpha = 0.5f), 1f
                        ),
                        Line(
                            BollingerBands.key(20, 2.0, AverageType.SMA) with BollingerBands.LOW,
                            Color(0xFF9900).copy(alpha = 0.5f), 1f
                        ),
                        FillBetween(
                            (BollingerBands.key(20, 2.0, AverageType.SMA) with BollingerBands.HIGH) to
                                    (BollingerBands.key(20, 2.0, AverageType.SMA) with BollingerBands.LOW),
                            Color.Yellow.copy(alpha = 0.2f)
                        ),
                        SingleFocusable(
                            CandleSticks(
                                Ohcl.key() with Ohcl.OHCL,
                                positiveColor = Color(0xFF39a59a),
                                negativeColor = Color(0xFFe95751)
                            ), CandleSticks(
                                Ohcl.key() with Ohcl.OHCL,
                                positiveColor = Color(0xFF00FF00),
                                negativeColor = Color(0xFFFF0000)
                            )
                        )
                    )
                ),
                ohcl
            )
        }
    }
}

