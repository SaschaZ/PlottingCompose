package dev.zieger.plottingcompose

import androidx.compose.foundation.layout.padding
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
                time * 60, open, high, close, low, Random.nextLong()
            )
        }

        val candles = remember {
            var lastClose: Float? = null
            PlotSeries((0..100).map {
                OhclItem(randomOhcl(it.toLong(), lastClose).also { c -> lastClose = c.close })
            }, CandleSticks(),
                Label<Ohcl> { i -> "${i.time}\n${i.close}" })
        }
        val sma = remember {
            val closes = candles.items.map { it.data.time to it.data.close }
            PlotSeries(
                closes.mapIndexed { idx, (x, _) ->
                    x to closes.subList((idx - 24).coerceAtLeast(0), idx)
                        .map { it.second }.average()
                }.map { (x, sma) -> if (sma.isNaN() || sma.isInfinite()) x to null else x to sma }
                    .map { (x, sma) -> SeriesItem(x, sma) },
                Line(SimpleLine(Color.Blue, 2f))
            )
        }
        val bb = remember {
            val stdDev = sma.items.mapIndexed { idx, _ ->
                val items = sma.items.subList((idx - 20).coerceAtLeast(0), idx)
                    .map { it.data.position.offset.y }
                if (idx > 22) {
                    val avg = items.average()
                    sqrt(items.sumOf { (it - avg).pow(2) })
                } else null
            }
            val bb = sma.items.mapIndexed { idx, item ->
                item.data.position.offset.x to item.data.position.let {
                    Triple(
                        stdDev[idx]?.let { s -> it.offset.y - s },
                        if (it.isYEmpty) null else it.offset.y,
                        stdDev[idx]?.let { s -> it.offset.y + s }
                    )
                }
            }
            listOf(
                PlotSeries(
                    bb.map { (x, y) -> SeriesItem(x, y.first) },
                    Line(SimpleLine(Color.Blue))
                ),
                PlotSeries(
                    bb.map { (x, y) -> SeriesItem(x, y.second) },
                    Line(SimpleLine(Color.Blue))
                ),
                PlotSeries(
                    bb.map { (x, y) -> SeriesItem(x, y.third) },
                    Line(SimpleLine(Color.Blue))
                )
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
                Modifier.padding(vertical = 8.dp),
                parameter = PlotParameter(
                    focusAxis = Axis.X,
                    scrollAction = when {
                        ctrlPressed.value && !shiftPressed.value && !altPressed.value -> ScrollAction.WIDTH_FACTOR
                        !ctrlPressed.value && shiftPressed.value && !altPressed.value -> ScrollAction.X_TRANSLATION
                        else -> ScrollAction.SCALE
                    },
                    verticalPadding = { 0.dp }, //verticalPlotPadding = { 0.dp },
                    horizontalPadding = { 8.dp },// horizontalPlotPadding = { 0.dp },
//                    drawYLabels = false, drawXLabels = false
                )
            ) {
                plot(0.75f, it.copy(drawXLabels = false)) {
                    set(candles)
                    add(bb[0])
                    add(bb[1])
                    add(bb[2])
                }
                plot(0.25f) {
                    set(sma)
                }
            }
        }
    }
}

