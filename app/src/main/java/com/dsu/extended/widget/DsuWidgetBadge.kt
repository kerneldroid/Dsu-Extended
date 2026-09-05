package com.dsu.extended.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.annotation.DrawableRes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.core.content.ContextCompat

object DsuWidgetBadge {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun createSunnyBadge(
        context: Context,
        sizePx: Int,
        badgeColorArgb: Int,
        @DrawableRes iconRes: Int,
        iconTintArgb: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Official M3 Expressive Sunny geometry, tessellated off-composition from
        // RoundedPolygon cubics (toShape() is @Composable and unusable here).
        // Polygon is centered at (centerX, centerY); normalize to [0, sizePx].
        val androidPath: Path = runCatching {
            val polygon = MaterialShapes.Sunny
            Path().apply {
                val cubics = polygon.cubics
                cubics.forEachIndexed { index, cubic ->
                    val ax0 = polygon.centerX + cubic.anchor0X
                    val ay0 = polygon.centerY + cubic.anchor0Y
                    val c0x = polygon.centerX + cubic.control0X
                    val c0y = polygon.centerY + cubic.control0Y
                    val c1x = polygon.centerX + cubic.control1X
                    val c1y = polygon.centerY + cubic.control1Y
                    val ax1 = polygon.centerX + cubic.anchor1X
                    val ay1 = polygon.centerY + cubic.anchor1Y
                    if (index == 0) moveTo(ax0, ay0)
                    cubicTo(c0x, c0y, c1x, c1y, ax1, ay1)
                }
                close()
                val bounds = android.graphics.RectF()
                computeBounds(bounds, true)
                val matrix = Matrix()
                matrix.setRectToRect(
                    bounds,
                    android.graphics.RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()),
                    Matrix.ScaleToFit.FILL,
                )
                transform(matrix)
            }
        }.getOrElse {
            Path().apply { addCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, Path.Direction.CW) }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeColorArgb
            style = Paint.Style.FILL
        }
        canvas.drawPath(androidPath, paint)

        val icon = ContextCompat.getDrawable(context, iconRes)?.mutate()
        if (icon != null) {
            val iconSize = (sizePx * 0.46f).toInt()
            val offset = (sizePx - iconSize) / 2
            icon.setBounds(offset, offset, offset + iconSize, offset + iconSize)
            icon.setTint(iconTintArgb)
            icon.draw(canvas)
        }

        return bitmap
    }
}
