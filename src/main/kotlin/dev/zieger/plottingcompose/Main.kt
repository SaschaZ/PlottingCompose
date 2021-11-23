package dev.zieger.plottingcompose

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.lang.Float.max
import java.lang.Float.min
import kotlin.math.pow
import kotlin.random.Random

fun main() = application {
    MaterialTheme(MaterialTheme.colors.copy(background = Color.Black, onBackground = Color.White)) {
        fun randomOhcl(time: Long, lastClose: Float? = null): Ohcl {
            val open = lastClose ?: (Random.nextFloat() * 20_000 + 5_000f)
            val close = Random.nextFloat() * 20_000 + 5_000f
            val high = max(open, close) + Random.nextFloat() * 5_000f
            val low = min(open, close) - Random.nextFloat() * 5_000f
            return OhclValue(
                time * 60, open, high, close, low, Random.nextLong()
            )
        }

        val candles = remember {
            var lastClose: Float? = null
            Series((0..250).map {
                OhclItem(randomOhcl(it.toLong(), lastClose).also { c -> lastClose = c.close },
                    Focusable(CandleSticks(), CandleSticks(Color.Yellow, Color.Blue)), Focusable(EmtpyPlotStyle(), Label { i -> "${i.time}\n${i.close}" }))
            })
        }
        val values = remember {
            Series((-100..100).map {
                SeriesItem(
                    Unit, it, -it.toDouble().pow(2) / 1000000000,
                    Focusable(
                        Dot(Color.Black.copy(alpha = 0.5f)),
                        Dot(Color.Black.copy(alpha = 0.5f), width = 50f)
                    ), Line()
                )
            })
                .withPreDrawer(Filled(Color.Magenta.copy(alpha = 0.5f)))
        }
        val values2 = remember {
            Series((-100..100).map {
                SeriesItem(
                    Unit, it, it.toDouble().pow(2) / 1000000000,
                    Focusable(
                        Dot(Color.Black.copy(alpha = 0.5f)),
                        Dot(Color.Black.copy(alpha = 0.5f), width = 50f)
                    ), Line()
                )
            }).withPreDrawer(Filled(Color.Cyan.copy(alpha = 0.5f), upWards = true))
        }

        Window(onCloseRequest = ::exitApplication) {
            Plot(parameter = PlotParameter(plotYLabelWidth = { 150.dp }, focusAxis = Axis.BOTH)) {
                add(values2)
                add(values)
//                add(candles)
            }
        }
    }
}

