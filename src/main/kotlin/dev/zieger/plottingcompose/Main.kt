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

        val candles = remember {
            var lastClose: Float? = null
            PlotSeries((0..150).map { idx ->
                OhclItem(randomOhcl(idx.toLong(), lastClose).also { c -> lastClose = c.close },
                    SingleFocusable(
                        CandleSticks(lineColor = Color.White),
                        CandleSticks(Color.Yellow, Color.Blue, lineColor = Color.White)
                    ),
                    Label { "${it.extra.time}\n${it.volume}" })
            })
        }
        val volume = remember {
            PlotSeries(
                candles.items.map {
                    PlotSeriesItem(
                        SimplePlotItem(it.item.time.toFloat(), it.item.volume.toFloat(), extra = Unit),
                        Impulses(if (it.item.open <= it.item.close) Color.Green else Color.Red)
                    )
                }
            )
        }
        val sma = remember {
            val closes = candles.items.map { it.item.time to it.item.close }
            PlotSeries(
                closes.mapIndexed { idx, (x, _) ->
                    x to closes.subList((idx - 24).coerceAtLeast(0), idx)
                        .map { it.second }.average()
                }.map { (x, sma) -> if (sma.isNaN() || sma.isInfinite()) x to null else x to sma }
                    .map { (x, sma) -> PlotSeriesItem(SimplePlotItem(x.toFloat(), sma?.toFloat())) },
                Line()
            )
        }
        val bb = remember {
            val stdDev = sma.items.mapIndexed { idx, _ ->
                val items = sma.items.subList((idx - 20).coerceAtLeast(0), idx)
                    .mapNotNull { it.item.y.values.first() }
                if (idx > 22) {
                    val avg = items.average()
                    sqrt(items.sumOf { (it - avg).pow(2) })
                } else null
            }
            val bb = sma.items.mapIndexed { idx, item ->
                item.item.x to item.item.y.values.firstOrNull()?.let {
                    Triple(
                        stdDev[idx]?.let { s -> it - s },
                        it,
                        stdDev[idx]?.let { s -> it + s }
                    )
                }
            }
            PlotSeries(
                bb.map { (x, y) ->
                    PlotSeriesItem(
                        SimplePlotItem(
                            x,
                            *y?.toList()?.map { it?.toFloat() }?.toTypedArray() ?: emptyArray()
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
                    horizontalPadding = { 0.dp }, horizontalPlotPadding = { 0.dp },
//                    drawYLabels = false, drawXLabels = false
                    plotYLabelWidth = { plotSize.value.width.dp * 0.075f }
                ),
                colors = PlotColors(Color.Black, Color.White, Color.DarkGray, Color.White, Color.White)
            ) {
                plot(0.85f, it.copy(drawXLabels = false)) {
                    set(candles)
                    add(bb)
                }
                plot(0.15f, it.copy(verticalPlotPadding = { 0.dp })) {
                    set(volume)
                }
            }
        }
    }
}

