package dev.zieger.plottingcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.singleWindowApplication
import dev.zieger.utils.time.TimeStamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

fun linear(c: Double = 0.0, d: Double = 1.0, b: Double = 0.0, t: Double): Double =
    c + t / d + b

fun lerp(start: Double, end: Double, t: Double): Double =
    start + (end - start) * t

fun flip(t: Double): Double = 1 - t

fun easeIn(exp: Double, t: Double): Double = t.pow(exp)
fun easeOut(exp: Double, t: Double): Double = flip(easeIn(exp, flip(t)))
fun easeInOut(exp: Double, t: Double): Double = lerp(easeIn(exp, t), easeOut(exp, t), t)

fun spike(exp: Double, tSpike: Float = 0.5f, t: Double): Double = when {
    t <= tSpike -> easeIn(exp, t * 2)
    else -> easeIn(exp, flip(t) * 2)
}

typealias Interpolator = (Double) -> Double

object InterpolatorTest {

    @OptIn(ExperimentalFoundationApi::class)
    @JvmStatic
    fun main(args: Array<String>) = singleWindowApplication {
        val tsAmount = 2000
        val ts =
            remember { ((0..tsAmount step 1) + (0..tsAmount step 1).map { tsAmount - it }).map { it / tsAmount.toDouble() } }
        var t by remember { mutableStateOf(ts.first()) }
        val interpolator = listOf<Interpolator>(
            { linear(t = it) }, { easeIn(3.0, it) }, { easeOut(3.0, it) },
            { easeInOut(0.4, it) }, { easeInOut(0.5, it) },
            { spike(2.0, 0.5f, it) }, { spike(5.0, 0.5f, it) }
        )

        var size by remember { mutableStateOf(IntSize.Zero) }
        LazyVerticalGrid(
            GridCells.Fixed(3),
            Modifier.fillMaxSize().onSizeChanged { size = it }
                .background(Color.Black),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            items(interpolator) {
                InterpolatorComposable(Modifier.fillMaxWidth().height((size.height / 2).dp), it, t)
            }
        }

        val scope = rememberCoroutineScope()
        remember {
            scope.launch {
                val start = TimeStamp()
                while (isActive) {
                    delay(1)
                    t = ts[(TimeStamp() - start).millisLong.toInt() % ts.size]
                }
            }
        }
    }

    @Composable
    private fun InterpolatorComposable(
        modifier: Modifier = Modifier,
        interpolator: Interpolator,
        t: Double
    ) {
        Column(modifier.background(Color.Black), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("none", Modifier.padding(vertical = 4.dp), Color.White, 20.sp)
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                clipRect(0f, 0f, size.width, size.height) {
                    drawRect(Color.Black, Rect(0f, 0f, size.width, size.height))
                    var prev = Offset(0f, size.height)
                    (0..1000 step 1).map { it / 1000.0 }.map {
                        val off = Offset(
                            it.toFloat() * size.width,
                            size.height - interpolator(it).toFloat() * size.height
                        )
                        drawLine(Color.Cyan, prev, off, 4f)
                        prev = off
                    }

                    drawCircle(
                        Color.Cyan, 15f, Offset(
                            t.toFloat() * size.width,
                            size.height - interpolator(t).toFloat() * size.height
                        )
                    )
                }
            }
        }
    }
}