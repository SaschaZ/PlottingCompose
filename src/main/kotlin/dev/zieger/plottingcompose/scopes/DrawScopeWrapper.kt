package dev.zieger.plottingcompose.scopes

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

class DrawScopeWrapper(
    private val scope: DrawScope,
    private val regionHolder: DrawScopeRegionHolder
) : DrawScope {

    override val density: Float get() = scope.density
    override val drawContext: DrawContext get() = scope.drawContext
    override val fontScale: Float get() = scope.fontScale
    override val layoutDirection: LayoutDirection get() = scope.layoutDirection

    override fun drawArc(
        brush: Brush,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(topLeft, size)
        scope.drawArc(brush, startAngle, sweepAngle, useCenter, topLeft, size, alpha, style, colorFilter, blendMode)
    }

    override fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(topLeft, size)
        scope.drawArc(color, startAngle, sweepAngle, useCenter, topLeft, size, alpha, style, colorFilter, blendMode)
    }

    override fun drawCircle(
        brush: Brush,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(center, radius)
        scope.drawCircle(brush, radius, center, alpha, style, colorFilter, blendMode)
    }

    override fun drawCircle(
        color: Color,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(center, radius)
        scope.drawCircle(color, radius, center, alpha, style, colorFilter, blendMode)
    }

    override fun drawImage(
        image: ImageBitmap,
        topLeft: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(topLeft, Size(image.height.toFloat(), image.width.toFloat()))
        scope.drawImage(image, topLeft, alpha, style, colorFilter, blendMode)
    }

    override fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(dstOffset, dstSize)
        scope.drawImage(image, srcOffset, srcSize, dstOffset, dstSize, alpha, style, colorFilter, blendMode)
    }

    override fun drawLine(
        brush: Brush,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(start, end)
        scope.drawLine(brush, start, end, strokeWidth, cap, pathEffect, alpha, colorFilter, blendMode)
    }

    override fun drawLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(start, end)
        scope.drawLine(color, start, end, strokeWidth, cap, pathEffect, alpha, colorFilter, blendMode)
    }

    override fun drawOval(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(topLeft, size)
        scope.drawOval(brush, topLeft, size, alpha, style, colorFilter, blendMode)
    }

    override fun drawOval(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(topLeft, size)
        scope.drawOval(color, topLeft, size, alpha, style, colorFilter, blendMode)
    }

    override fun drawPath(
        path: Path,
        brush: Brush,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(path)
        scope.drawPath(path, brush, alpha, style, colorFilter, blendMode)
    }

    override fun drawPath(
        path: Path,
        color: Color,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(path)
        scope.drawPath(path, color, alpha, style, colorFilter, blendMode)
    }

    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        brush: Brush,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(points)
        scope.drawPoints(points, pointMode, brush, strokeWidth, cap, pathEffect, alpha, colorFilter, blendMode)
    }

    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        color: Color,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(points)
        scope.drawPoints(points, pointMode, color, strokeWidth, cap, pathEffect, alpha, colorFilter, blendMode)
    }

    override fun drawRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(topLeft, size)
        scope.drawRect(brush, topLeft, size, alpha, style, colorFilter, blendMode)
    }

    override fun drawRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(topLeft, size)
        scope.drawRect(color, topLeft, size, alpha, style, colorFilter, blendMode)
    }

    override fun drawRoundRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(topLeft, size)
        scope.drawRoundRect(brush, topLeft, size, cornerRadius, alpha, style, colorFilter, blendMode)
    }

    override fun drawRoundRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        style: DrawStyle,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        regionHolder.addRegion(topLeft, size)
        scope.drawRoundRect(color, topLeft, size, cornerRadius, style, alpha, colorFilter, blendMode)
    }
}