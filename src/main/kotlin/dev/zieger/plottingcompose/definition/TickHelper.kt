@file:Suppress("MemberVisibilityCanBePrivate")

package dev.zieger.plottingcompose.definition

import dev.zieger.plottingcompose.scopes.range
import dev.zieger.utils.time.*
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat

object TickHelper {

    private val decimalFormat = DecimalFormat("##,###.###")
    private fun format(value: Number): String = decimalFormat.format(value.toDouble())

    fun ticksY(
        valueRange: ClosedRange<Double>,
        tickAmount: Int,
    ): Ticks = valueRange.ticksValue(tickAmount).doubleFormat()

    fun ticksX(
        valueRange: ClosedRange<Double>,
        tickAmount: Int,
        idxRange: ClosedRange<Int>
    ): Ticks = valueRange.ticksValue(tickAmount, 2f).doubleFormat()

    fun Pair<List<Double>, Double>.doubleFormat(): Ticks = Ticks(first.associateWith { setOf(format(it)) }, second)

    fun Pair<List<Double>, Double>.timeFormat(
        valueRange: ClosedRange<Double>,
        idxRange: ClosedRange<Int>
    ): Ticks {
        if (valueRange.range() == 0.0) return Ticks()

        var divider = 1.days
        while (valueRange.range().millis.divDouble(divider).toLong() < 4) {
            divider = when (divider) {
                1.days -> 12.hours
                12.hours -> 6.hours
                6.hours -> 1.hours
                1.hours -> 30.minutes
                30.minutes -> 1.minutes
                else -> divider
            }
        }
        return Ticks(first.associate { value ->
            val relIdx = (value - idxRange.start) / idxRange.range()
            value to (valueRange.start + valueRange.range() * relIdx).toLong().let {
                (it / divider.millisLong) * divider.millisLong
            }.toTime().run {
                val set = setOf(
                    formatTime(TimeFormat.CUSTOM("dd-MM-yy")),
                    formatTime(TimeFormat.CUSTOM("HH:mm"))
                )

                if (divider == 1.days) setOf(set.toList()[0])
                else set
            }
        }, second)
    }

    fun ClosedRange<Int>.ticksIdx(tickAmount: Int, extra: Float = 0f): Pair<List<Double>, Double> =
        (start.toDouble()..endInclusive.toDouble()).ticksValue(tickAmount, extra)

    fun ClosedRange<Double>.ticksValue(tickAmount: Int, extra: Float = 0f): Pair<List<Double>, Double> {
        val tickHeight = range() / tickAmount
        val scaledTickLength = tickHeight.toBigDecimal().round(MathContext(1, RoundingMode.HALF_UP)).toFloat()
        val numTicks = (range() / scaledTickLength).toInt()
        val e = (numTicks * extra).toInt()
        return (-e..numTicks.plus(e) step 1)
            .map { start - start % scaledTickLength + it * scaledTickLength } to -start
    }
}

data class Ticks(
    val ticks: Map<Double, Set<String>> = emptyMap(),
    val originIdx: Double = 0.0
)