@file:Suppress("MemberVisibilityCanBePrivate")

package dev.zieger.plottingcompose.strategy

import dev.zieger.plottingcompose.scopes.nullWhenEmpty
import java.util.*

fun stringBlock(block: LineFactory.() -> Unit): String {
    var current = LineGroup()
    val lines = LinkedList(listOf(current))
    val factory = LineFactory { line, append ->
        when (append) {
            true -> {
                val new = LineGroup(mutableListOf(line))
                current.lines.last().subGroups.add(new)
                current = new
                lines += current
            }
            false -> current.lines += line
        }
    }
    factory.block()
    return buildOutput(lines)
}

fun buildOutput(groups: List<LineGroup>): String = groups.joinToString("") { group ->
    group.build()
}

data class LineFactory(val newLine: (line: Line, append: Boolean) -> Unit) {

    fun starLine() = line(onlyStars = true)

    fun line(
        center: Boolean = false,
        onlyStars: Boolean = false
    ) = "".line(center, onlyStars)

    operator fun String.unaryPlus(): Line = line()
    operator fun String.unaryMinus(): Line = line(center = true)

    fun String.line(
        center: Boolean = false,
        onlyStars: Boolean = false
    ) = Line(this, center, onlyStars).also {
        newLine(it, false)
    }

    operator fun Line.plus(producer: LineSource?) {
        var first = true
        producer?.line(this@LineFactory.copy(newLine = { line, _ ->
            newLine(line, first)
            first = false
        }))
    }
}

data class LineGroup(val lines: MutableList<Line> = LinkedList()) {
    val length get() = lines.maxOf { it.length } + 4

    fun build(startOffset: Int = 0): String = lines.joinToString("") { it.build(startOffset, length) }
}

data class Line(
    val content: String,
    val center: Boolean = false,
    val onlyStars: Boolean = false,
    val subGroups: MutableList<LineGroup> = LinkedList()
) {

    val length: Int get() = content.length + (subGroups.nullWhenEmpty()?.maxOf { it.length } ?: 0) + 4

    fun build(startOffset: Int, maxLength: Int): String =
        (0 until maxLength).joinToString("") { idx ->
            when {
                onlyStars -> "*"
                center -> when (idx) {
                    startOffset, maxLength - 1 -> "*"
                    in startOffset + (maxLength - startOffset) / 2 - content.length / 2..
                            startOffset + (maxLength - startOffset) / 2 + content.length / 2 ->
                        content.getOrNull(idx - (startOffset + (maxLength - startOffset) / 2 - content.length / 2))
                            ?.toString() ?: " "
                    else -> " "
                }
                else -> when (idx) {
                    startOffset, maxLength - 1 -> "*"
                    else -> content.getOrNull(idx - startOffset - 2)?.toString() ?: " "
                }
            }
        } + "\n"
}

interface LineSource {
    fun line(factory: LineFactory)
}

fun <T : Any> List<T>.joinToStringIndexed(separator: String = "", block: (idx: Int, value: T) -> String): String {
    var idx: Int = 0
    return joinToString(separator) { block(idx++, it) }
}