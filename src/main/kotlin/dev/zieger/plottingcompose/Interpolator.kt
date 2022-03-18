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

sealed class Interpolator {

    abstract fun interpolate(t: Double): Double

    data class Linear(val c: Double = 0.0, val d: Double = 1.0, val b: Double = 0.0) : Interpolator() {
        override fun interpolate(t: Double): Double = c + t / d + b
    }

    data class Lerp(val start: Double, val end: Double) : Interpolator() {
        override fun interpolate(t: Double): Double = start + (end - start) * t
    }

    object Flip : Interpolator() {
        override fun interpolate(t: Double): Double = 1 - t
    }

    data class EaseIn(val exponent: Double = 2.0) : Interpolator() {
        override fun interpolate(t: Double): Double = t.pow(exponent)
    }

    data class EaseOut(val exponent: Double = 2.0) : Interpolator() {
        override fun interpolate(t: Double): Double =
            Flip.interpolate(EaseIn(exponent).interpolate(Flip.interpolate(t)))
    }

    data class EaseInOut(val exponent: Double = 2.0) : Interpolator() {
        override fun interpolate(t: Double): Double =
            Lerp(EaseIn(exponent).interpolate(t), EaseOut(exponent).interpolate(t)).interpolate(t)
    }

    data class Spike(val exponent: Double = 2.0) : Interpolator() {
        override fun interpolate(t: Double): Double = when {
            t <= 0.5 -> EaseIn(exponent).interpolate(t * 2)
            else -> EaseIn(exponent).interpolate(Flip.interpolate(t) * 2)
        }
    }

    companion object {

        @OptIn(ExperimentalFoundationApi::class)
        @JvmStatic
        fun main(args: Array<String>) = singleWindowApplication {
            val tsAmount = 2000
            val ts =
                remember { ((0..tsAmount step 1) + (0..tsAmount step 1).map { tsAmount - it }).map { it / tsAmount.toDouble() } }
            var t by remember { mutableStateOf(ts.first()) }
            val interpolator = remember {
                listOf(
                    Linear(), EaseIn(), EaseOut(), EaseInOut(), EaseInOut(0.5), Spike(), Spike(5.0)
                )
            }

            var size by remember { mutableStateOf(IntSize.Zero) }
            LazyVerticalGrid(
                GridCells.Fixed(3),
                Modifier.fillMaxSize().onSizeChanged { size = it }
                    .background(Color.Black),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                items(interpolator) {
                    Interpolator(Modifier.fillMaxWidth().height((size.height / 2).dp), it, t)
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
        private fun Interpolator(
            modifier: Modifier = Modifier,
            interpolator: Interpolator,
            t: Double
        ) {
            Column(modifier.background(Color.Black), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(interpolator::class.simpleName!!, Modifier.padding(vertical = 4.dp), Color.White, 20.sp)
                Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                    drawRect(Color.Black, Rect(0f, 0f, size.width, size.height))
                    var prev = Offset(0f, size.height)
                    (0..1000 step 1).map { it / 1000.0 }.map {
                        val off = Offset(
                            it.toFloat() * size.width,
                            size.height - interpolator.interpolate(it).toFloat() * size.height
                        )
                        drawLine(Color.Cyan, prev, off, 4f)
                        prev = off
                    }

                    drawCircle(
                        Color.Cyan, 15f, Offset(
                            t.toFloat() * size.width,
                            size.height - interpolator.interpolate(t).toFloat() * size.height
                        )
                    )
                }
            }
        }
    }
}