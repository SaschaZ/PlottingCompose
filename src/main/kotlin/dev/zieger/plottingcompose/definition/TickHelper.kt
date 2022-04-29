@file:Suppress("MemberVisibilityCanBePrivate")

package dev.zieger.plottingcompose.definition

import dev.zieger.utils.time.*
import dev.zieger.utils.time.progression.step
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

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

    fun ticksXTime(
        timeRange: ClosedRange<ITimeStamp>,
        idxRange: ClosedRange<Int>,
        amount: Int
    ): Ticks {
        val possibleDivider = setOf(
            1.years,
            6.months, 3.months, 1.months,
            2.weeks, 1.weeks,
            3.days, 2.days, 1.days,
            12.hours, 6.hours, 1.hours,
            30.minutes, 15.minutes, 5.minutes, 1.minutes
        )

        val expectedDivider = timeRange.range().divDouble(amount)
        val divider = possibleDivider.minByOrNull { (it - expectedDivider).abs }!!
        val timeStampIdxMap = timeRange.normalizedProgression(divider)
            .filter { it in timeRange }
            .associateWith { time -> (idxRange.start + (time - timeRange.start) / timeRange.range() * idxRange.range()).millisLong }

        return Ticks(timeStampIdxMap.entries
            .associate { (k, v) ->
                v.toDouble() to when {
                    divider < 1.days -> k.formatTimeSets("HH:mm", "dd.MM")
                    else -> k.formatTimeSets("dd.MM", "yyyy")
                }
            }
        )
    }

    fun ITimeStamp.formatTimeSets(vararg pattern: String): Set<String> =
        pattern.map { formatTime(TimeFormat.CUSTOM(it)) }.toSet()

    fun Pair<List<Double>, Double>.doubleFormat(): Ticks = Ticks(first.associateWith { setOf(format(it)) }, second)

    fun Pair<List<Double>, Double>.timeFormat(
        valueRange: ClosedRange<Double>,
        idxRange: ClosedRange<Int>
    ): Ticks {
        if (valueRange.range() == 0.0) return Ticks()

        var divider = 1.days
        while (valueRange.range().millis.divDouble(divider).toLong() < 3) {
            divider = when (divider) {
                1.days -> 12.hours
                12.hours -> 6.hours
                6.hours -> 1.hours
                1.hours -> 30.minutes
                30.minutes -> 1.minutes
                else -> divider
            }
        }
        return Ticks(first.associateWith { value ->
            val relIdx = (value - idxRange.start) / idxRange.range()
            (valueRange.start + valueRange.range() * relIdx).toLong().let {
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
        toDouble().ticksValue(tickAmount, extra)

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

fun <T> ClosedRange<T>.toTime(
    unit: TimeUnit = TimeUnit.MILLI,
    zone: TimeZone = UTC
): ClosedRange<ITimeStamp> where T : Comparable<T>, T : Number =
    start.toTime(unit, zone)..endInclusive.toTime(unit, zone)

fun ClosedRange<ITimeStamp>.range(): ITimeSpan = endInclusive - start

fun ITimeStamp.normalizeDown(duration: ITimeSpan) = this - this % duration
fun ITimeStamp.normalizeUp(duration: ITimeSpan) = normalizeDown(duration).let { if (it != this) it + duration else it }
fun ClosedRange<ITimeStamp>.normalize(duration: ITimeSpan) =
    start.normalizeDown(duration)..endInclusive.normalizeUp(duration)

fun ClosedRange<ITimeStamp>.normalizedProgression(duration: ITimeSpan) = normalize(duration) step duration
fun ClosedFloatingPointRange<Float>.range() = endInclusive - start
fun ClosedRange<Double>.range() = endInclusive - start
fun ClosedRange<Int>.range() = endInclusive - start
fun IntRange.range() = endInclusive - start

fun ClosedRange<Int>.toDouble() = start.toDouble()..endInclusive.toDouble()