package dev.zieger.plottingcompose.scopes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import dev.zieger.plottingcompose.styles.lineTo
import dev.zieger.plottingcompose.styles.moveTo
import java.util.*

data class Region(
    val path: Path
) {
    constructor(topLeft: Offset, size: Size) : this(Path().apply {
        moveTo(topLeft)
        lineTo(topLeft.x + size.width, topLeft.y)
        lineTo(topLeft.x + size.width, topLeft.y - size.height)
        lineTo(topLeft.x, topLeft.y - size.height)
    })

    constructor(path: List<Offset>) : this(Path().apply {
        path.forEachIndexed { idx, offset ->
            when (idx) {
                0 -> moveTo(offset)
                else -> lineTo(offset)
            }
        }
    })

    constructor(line: Pair<Offset, Offset>) : this(Path().apply {
        moveTo(line.first)
        lineTo(line.second)
    })

    fun isInside(rect: Rect): Boolean = !path.getBounds().intersect(rect).isEmpty
    fun isInsideXRange(xRange: ClosedRange<Float>): Boolean = path.getBounds().run { left in xRange || right in xRange }
    fun isInsideYRange(yRange: ClosedRange<Float>): Boolean = path.getBounds().run { top in yRange || bottom in yRange }
}


interface IDrawScopeRegionHolder {
    val regions: Map<Int, MutableList<Region>>
    var activeIdx: Int

    fun yPixelRange(xRange: ClosedRange<Float>? = null): ClosedRange<Float>? {
        val inside = xRange?.let { regions.filter { (k, v) -> v.any { it.isInsideXRange(xRange) } }.toMap() } ?: regions
        if (inside.isEmpty()) return null
        return inside.values.minOf { it.minOfOrNull { m -> m.path.getBounds().top } ?: 0f }..
                inside.values.maxOf { it.maxOfOrNull { m -> m.path.getBounds().bottom } ?: 0f }
    }

    fun xPixelRange(yRange: ClosedRange<Float>? = null): ClosedRange<Float>? {
        val inside = yRange?.let { regions.filter { (k, v) -> v.any { it.isInsideYRange(yRange) } }.toMap() } ?: regions
        if (inside.isEmpty()) return null
        return inside.values.minOf { it.minOf { m -> m.path.getBounds().left } }..inside.values.maxOf { it.maxOf { m -> m.path.getBounds().right } }
    }
}

class DrawScopeRegionHolder : IDrawScopeRegionHolder {

    override val regions = HashMap<Int, MutableList<Region>>()
    override var activeIdx = -1

    fun addRegion(topLeft: Offset, size: Size) = regions.getOrPut(activeIdx) { LinkedList() }.add(Region(topLeft, size))
    fun addRegion(topLeft: IntOffset, size: IntSize) =
        regions.getOrPut(activeIdx) { LinkedList() }.add(Region(topLeft.toOffset(), size.toSize()))

    fun addRegion(center: Offset, radius: Float) =
        regions.getOrPut(activeIdx) { LinkedList() }.add(
            Region(Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2))
        )

    fun addRegion(start: Offset, end: Offset) = regions.getOrPut(activeIdx) { LinkedList() }.add(Region(start to end))
    fun addRegion(path: Path) = regions.getOrPut(activeIdx) { LinkedList() }.add(Region(path))
    fun addRegion(path: List<Offset>) = regions.getOrPut(activeIdx) { LinkedList() }.add(Region(path))
}