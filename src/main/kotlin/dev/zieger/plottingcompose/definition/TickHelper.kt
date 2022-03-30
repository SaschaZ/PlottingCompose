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
        chartSize: Int,
        tickLength: Float = 30f
    ): Map<Double, Set<String>> = valueRange.ticksValue(chartSize, tickLength).doubleFormat()

    fun ticksX(
        valueRange: ClosedRange<Double>,
        chartSize: Int,
        tickLength: Float = 30f,
        idxRange: ClosedRange<Int>
    ): Map<Double, Set<String>> = idxRange.ticksIdx(chartSize, tickLength).doubleFormat()

    fun List<Double>.doubleFormat() = associateWith { setOf(format(it)) }

    fun List<Double>.timeFormat(
        valueRange: ClosedRange<Double>,
        idxRange: ClosedRange<Int>
    ): Map<Double, Set<String>> {
        if (valueRange.range() == 0.0) return emptyMap()

        var divider = 1.days
        while (valueRange.range().millis.div(divider).toLong() < 4) {
            divider = when (divider) {
                1.days -> 12.hours
                12.hours -> 6.hours
                6.hours -> 1.hours
                1.hours -> 30.minutes
                30.minutes -> 1.minutes
                else -> divider
            }
        }
        return associate { value ->
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
        }
    }

    fun ClosedRange<Int>.ticksIdx(availablePixel: Int, tickLength: Float): List<Double> =
        (start.toDouble()..endInclusive.toDouble()).ticksValue(availablePixel, tickLength)

    fun ClosedRange<Double>.ticksValue(availablePixel: Int, tickLength: Float): List<Double> {
        val pSize = availablePixel / tickLength
        val tickHeight = range() / pSize
        val scaledTickLength = tickHeight.toBigDecimal().round(MathContext(1, RoundingMode.HALF_UP)).toFloat()
        return (-100..(range() / scaledTickLength).toInt().plus(100) step 1)
            .map { start - start % scaledTickLength + it * scaledTickLength }
    }
}