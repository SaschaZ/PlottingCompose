package dev.zieger.plottingcompose

import androidx.compose.foundation.background
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.zieger.plottingcompose.styles.*
import java.lang.Float.max
import java.lang.Float.min
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

fun main() = application {
    MaterialTheme(MaterialTheme.colors.copy(background = Color.Black, onBackground = Color.White)) {
        fun randomOhcl(time: Long, lastClose: Float? = null): Ohcl {
            val open = lastClose ?: (Random.nextFloat() * 20_000 + 50_000f)
            val close = Random.nextFloat() * 20_000 + 50_000f
            val high = max(open, close) + Random.nextFloat() * 5_000f
            val low = min(open, close) - Random.nextFloat() * 5_000f
            return OhclValue(
                time * 60, open, high, close, low, Random.nextLong().absoluteValue % 2000L
            )
        }

        val ohcl = remember {
            var lastClose: Float? = null
            (0..150).map { idx ->
                randomOhcl(idx.toLong(), lastClose).also { c -> lastClose = c.close }
            }
        }
        val closes = remember {
            ohcl.map { it.time to it.close }
        }
        val sma20 = remember {
            closes.mapIndexed { idx, (time, _) ->
                time to closes.subList((idx - 20).coerceAtLeast(0), idx)
                    .map { it.second }.average()
            }
        }

        val candles = remember {
            PlotSeries(ohcl.takeLast(100).map { o ->
                OhclItem(
                    o,
                    SingleFocusable(
                        CandleSticks(lineColor = Color.White),
                        CandleSticks(Color.Yellow, Color.Blue, lineColor = Color.White)
                    ),
                    Label<Ohcl, Ohcl> { "${it.extra.time}\n${it.close}$" }.focused(),
                    Label<Ohcl, Ohcl>(borderColor = Color.Transparent,
                        borderRoundCorner = 0f,
                        borderWidth = 0f,
                        padding = 0f,
                        backgroundColor = Color.Transparent,
                        contentColor = Color.White,
                        mouseIsPositionSource = false,
                        position = { _, pos, _ -> pos }) { "${it.close}$" }.focused()
                )
            })
        }
        val volume = remember {
            PlotSeries(
                ohcl.takeLast(100).map {
                    PlotSeriesItem(
                        SimplePlotItem(it.time.toFloat(), it.volume.toFloat()),
                        Impulses(if (it.open <= it.close) Color.Green else Color.Red)
                    )
                }
            )
        }
        val bb = remember {
            val stdDev = sma20.mapIndexed { idx, (time, _) ->
                val items = sma20.subList((idx - 20).coerceAtLeast(0), idx)
                    .map { it.second }
                if (idx > 22) {
                    val avg = items.average()
                    time to sqrt(items.sumOf { (it - avg).pow(2) })
                } else null
            }
            val bb = sma20.map { (time, sma) ->
                time to Triple(
                    stdDev.firstOrNull { it?.first == time }?.let { (_, s) -> sma - s },
                    sma,
                    stdDev.firstOrNull { it?.first == time }?.let { (_, s) -> sma + s }
                )
            }.takeLast(100)
            PlotSeries(
                bb.map { (x, y) ->
                    PlotSeriesItem(
                        SimplePlotItem(
                            x.toFloat(),
                            *y.toList().map { it?.toFloat() }.toTypedArray()
                        )
                    )
                },
                Line(Color.Blue) { it.y[0] },
                Line(Color.Blue) { it.y[1] },
                Line(Color.Blue) { it.y[2] },
                FillBetween { it.y[0]?.let { y0 -> it.y[1]?.let { y1 -> y0 to y1 } } },
                FillBetween { it.y[1]?.let { y0 -> it.y[2]?.let { y1 -> y0 to y1 } } }
            )
        }
//        val values = remember {
//            Series((-100..100).map {
//                SeriesItem(
//                    Unit, it, -it.toDouble().pow(2) / 1,
//                    Focusable(
//                        Dot(Color.Black.copy(alpha = 0.5f)),
//                        Dot(Color.Black.copy(alpha = 0.5f), width = 50f)
//                    ), Line()
//                )
//            }).withPreDrawer(Filled(Color.Magenta.copy(alpha = 0.5f)))
//        }
//        val values2 = remember {
//            Series((-100..100).map {
//                SeriesItem(
//                    Unit, it, it.toDouble().pow(2) / 1,
//                    Focusable(
//                        Dot(Color.Black.copy(alpha = 0.5f)),
//                        Dot(Color.Black.copy(alpha = 0.5f), width = 50f)
//                    ), Line()
//                )
//            }).withPreDrawer(Filled(Color.Cyan.copy(alpha = 0.5f), upWards = true))
//        }

        val ctrlPressed: MutableState<Boolean> = remember { mutableStateOf(false) }
        val shiftPressed: MutableState<Boolean> = remember { mutableStateOf(false) }
        val altPressed: MutableState<Boolean> = remember { mutableStateOf(false) }
        Window(onCloseRequest = ::exitApplication, onKeyEvent = {
            ctrlPressed.value = it.isCtrlPressed
            shiftPressed.value = it.isShiftPressed
            altPressed.value = it.isAltPressed
            false
        }, undecorated = true) {
            MultiPlot(
                Modifier.background(Color.Black),
                parameter = PlotParameter(
                    focusAxis = Axis.X,
                    scrollAction = ScrollAction.WIDTH_FACTOR/*when {
                        ctrlPressed.value && !shiftPressed.value && !altPressed.value -> ScrollAction.WIDTH_FACTOR
                        !ctrlPressed.value && shiftPressed.value && !altPressed.value -> ScrollAction.X_TRANSLATION
                        else -> ScrollAction.SCALE
                    }*/,
                    verticalPadding = { 0.dp }, verticalPlotPadding = { 0.dp },
                    horizontalPadding = { 0.dp },
                    plotYLabelWidth = { plotSize.value.width.dp * 0.075f },
                    plotXLabelHeight = { plotSize.value.height.dp * 0.2f }
                ),
                colors = PlotColors(Color.Black, Color.White, Color.DarkGray, Color.White, Color.White)
            ) {
                plot(0.85f, it.copy(drawXLabels = false)) {
                    set(candles)
                    add(bb)
                }
                plot(0.15f) {
                    set(volume)
                }
            }
        }
    }
}

