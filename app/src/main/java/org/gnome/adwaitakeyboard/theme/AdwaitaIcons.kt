package org.gnome.adwaitakeyboard.theme

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SymbolicIcon {
    MENU,
    CLOSE,
    BACK,
    UNDO,
    REDO,
    COPY,
    CUT,
    PASTE,
    SELECT_ALL,
    CURSOR_LEFT,
    CURSOR_RIGHT,
    CLIPBOARD,
    EMOJI,
    PRIVATE_SHIELD,
    ONE_HANDED,
    SETTINGS,
    SHIFT,
    SHIFT_FILLED,
    CAPS_LOCK,
    BACKSPACE,
    ENTER,
    GLOBE,
    PIN,
    DELETE,
    SEARCH,
    KEYBOARD,
    APPEARANCE,
    TYPING,
    SOUND,
    INFO,
    RECENT_CLOCK,
    STAR,
    PLUS,
    MINUS,
    ARROW_UP,
    ARROW_DOWN
}

/**
 * Renders crisp monochrome GNOME Adwaita Symbolic icons (16x16 / 24x24 grid)
 * across Android Canvas, Android View Drawables, and Jetpack Compose without emoji glyphs.
 */
object AdwaitaIcons {

    fun draw(
        canvas: Canvas,
        icon: SymbolicIcon,
        cx: Float,
        cy: Float,
        sizePx: Float,
        color: Int
    ) {
        val scale = sizePx / 24f
        val left = cx - sizePx / 2f
        val top = cy - sizePx / 2f

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = (2.0f * scale).coerceAtLeast(1.5f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        fun gx(x: Float) = left + x * scale
        fun gy(y: Float) = top + y * scale

        val path = Path()
        when (icon) {
            SymbolicIcon.MENU -> {
                canvas.drawLine(gx(4f), gy(7f), gx(20f), gy(7f), strokePaint)
                canvas.drawLine(gx(4f), gy(12f), gx(20f), gy(12f), strokePaint)
                canvas.drawLine(gx(4f), gy(17f), gx(20f), gy(17f), strokePaint)
            }
            SymbolicIcon.CLOSE -> {
                canvas.drawLine(gx(6f), gy(6f), gx(18f), gy(18f), strokePaint)
                canvas.drawLine(gx(18f), gy(6f), gx(6f), gy(18f), strokePaint)
            }
            SymbolicIcon.BACK -> {
                path.moveTo(gx(15f), gy(6f))
                path.lineTo(gx(9f), gy(12f))
                path.lineTo(gx(15f), gy(18f))
                canvas.drawPath(path, strokePaint)
                canvas.drawLine(gx(9.5f), gy(12f), gx(19f), gy(12f), strokePaint)
            }
            SymbolicIcon.CURSOR_LEFT -> {
                path.moveTo(gx(15f), gy(6f))
                path.lineTo(gx(9f), gy(12f))
                path.lineTo(gx(15f), gy(18f))
                canvas.drawPath(path, strokePaint)
            }
            SymbolicIcon.CURSOR_RIGHT -> {
                path.moveTo(gx(9f), gy(6f))
                path.lineTo(gx(15f), gy(12f))
                path.lineTo(gx(9f), gy(18f))
                canvas.drawPath(path, strokePaint)
            }
            SymbolicIcon.UNDO -> {
                path.moveTo(gx(9f), gy(7f))
                path.lineTo(gx(5f), gy(11f))
                path.lineTo(gx(9f), gy(15f))
                canvas.drawPath(path, strokePaint)
                val arcRect = RectF(gx(5f), gy(11f), gx(19f), gy(19f))
                canvas.drawArc(arcRect, 180f, 180f, false, strokePaint)
            }
            SymbolicIcon.REDO -> {
                path.moveTo(gx(15f), gy(7f))
                path.lineTo(gx(19f), gy(11f))
                path.lineTo(gx(15f), gy(15f))
                canvas.drawPath(path, strokePaint)
                val arcRect = RectF(gx(5f), gy(11f), gx(19f), gy(19f))
                canvas.drawArc(arcRect, 180f, 180f, false, strokePaint)
            }
            SymbolicIcon.COPY -> {
                canvas.drawRoundRect(RectF(gx(9f), gy(9f), gx(19f), gy(20f)), 2f * scale, 2f * scale, strokePaint)
                path.moveTo(gx(15f), gy(9f))
                path.lineTo(gx(15f), gy(6f))
                path.lineTo(gx(6f), gy(6f))
                path.lineTo(gx(6f), gy(16f))
                path.lineTo(gx(9f), gy(16f))
                canvas.drawPath(path, strokePaint)
            }
            SymbolicIcon.CUT -> {
                canvas.drawCircle(gx(7.5f), gy(17.5f), 2.5f * scale, strokePaint)
                canvas.drawCircle(gx(16.5f), gy(17.5f), 2.5f * scale, strokePaint)
                canvas.drawLine(gx(9.5f), gy(15.5f), gx(18f), gy(5f), strokePaint)
                canvas.drawLine(gx(14.5f), gy(15.5f), gx(6f), gy(5f), strokePaint)
            }
            SymbolicIcon.PASTE, SymbolicIcon.CLIPBOARD -> {
                canvas.drawRoundRect(RectF(gx(6f), gy(6f), gx(18f), gy(20f)), 2.2f * scale, 2.2f * scale, strokePaint)
                canvas.drawRoundRect(RectF(gx(9.5f), gy(4f), gx(14.5f), gy(7.5f)), 1.2f * scale, 1.2f * scale, fillPaint)
                canvas.drawLine(gx(9f), gy(11.5f), gx(15f), gy(11.5f), strokePaint)
                canvas.drawLine(gx(9f), gy(15.5f), gx(13.5f), gy(15.5f), strokePaint)
            }
            SymbolicIcon.SELECT_ALL -> {
                canvas.drawRoundRect(RectF(gx(5f), gy(5f), gx(19f), gy(19f)), 2f * scale, 2f * scale, strokePaint)
                canvas.drawRoundRect(RectF(gx(8.5f), gy(8.5f), gx(15.5f), gy(15.5f)), 1f * scale, 1f * scale, fillPaint)
            }
            SymbolicIcon.EMOJI -> {
                canvas.drawCircle(gx(12f), gy(12f), 8f * scale, strokePaint)
                canvas.drawCircle(gx(9.2f), gy(10f), 1.2f * scale, fillPaint)
                canvas.drawCircle(gx(14.8f), gy(10f), 1.2f * scale, fillPaint)
                val smileRect = RectF(gx(8.5f), gy(10.5f), gx(15.5f), gy(16f))
                canvas.drawArc(smileRect, 20f, 140f, false, strokePaint)
            }
            SymbolicIcon.PRIVATE_SHIELD -> {
                path.moveTo(gx(12f), gy(4f))
                path.lineTo(gx(19f), gy(7f))
                path.lineTo(gx(19f), gy(12.5f))
                path.quadTo(gx(19f), gy(18f), gx(12f), gy(20.5f))
                path.quadTo(gx(5f), gy(18f), gx(5f), gy(12.5f))
                path.lineTo(gx(5f), gy(7f))
                path.close()
                canvas.drawPath(path, strokePaint)
                canvas.drawLine(gx(12f), gy(8.5f), gx(12f), gy(15.5f), strokePaint)
            }
            SymbolicIcon.ONE_HANDED -> {
                canvas.drawRoundRect(RectF(gx(4f), gy(6f), gx(20f), gy(18f)), 2f * scale, 2f * scale, strokePaint)
                canvas.drawRoundRect(RectF(gx(11f), gy(8f), gx(18f), gy(16f)), 1.2f * scale, 1.2f * scale, fillPaint)
            }
            SymbolicIcon.SETTINGS -> {
                canvas.drawCircle(gx(12f), gy(12f), 3.2f * scale, strokePaint)
                canvas.drawCircle(gx(12f), gy(12f), 7.2f * scale, strokePaint)
                canvas.drawLine(gx(12f), gy(2.8f), gx(12f), gy(4.8f), strokePaint)
                canvas.drawLine(gx(12f), gy(19.2f), gx(12f), gy(21.2f), strokePaint)
                canvas.drawLine(gx(2.8f), gy(12f), gx(4.8f), gy(12f), strokePaint)
                canvas.drawLine(gx(19.2f), gy(12f), gx(21.2f), gy(12f), strokePaint)
            }
            SymbolicIcon.SHIFT, SymbolicIcon.SHIFT_FILLED, SymbolicIcon.CAPS_LOCK -> {
                path.moveTo(gx(12f), gy(5f))
                path.lineTo(gx(19f), gy(12f))
                path.lineTo(gx(15f), gy(12f))
                path.lineTo(gx(15f), gy(17f))
                path.lineTo(gx(9f), gy(17f))
                path.lineTo(gx(9f), gy(12f))
                path.lineTo(gx(5f), gy(12f))
                path.close()
                if (icon == SymbolicIcon.SHIFT) {
                    canvas.drawPath(path, strokePaint)
                } else {
                    canvas.drawPath(path, fillPaint)
                }
                if (icon == SymbolicIcon.CAPS_LOCK) {
                    canvas.drawLine(gx(9f), gy(19.5f), gx(15f), gy(19.5f), strokePaint)
                }
            }
            SymbolicIcon.BACKSPACE -> {
                path.moveTo(gx(9.5f), gy(6.5f))
                path.lineTo(gx(4.5f), gy(12f))
                path.lineTo(gx(9.5f), gy(17.5f))
                path.lineTo(gx(19.5f), gy(17.5f))
                path.lineTo(gx(19.5f), gy(6.5f))
                path.close()
                canvas.drawPath(path, strokePaint)
                canvas.drawLine(gx(11.5f), gy(9.8f), gx(15.8f), gy(14.2f), strokePaint)
                canvas.drawLine(gx(15.8f), gy(9.8f), gx(11.5f), gy(14.2f), strokePaint)
            }
            SymbolicIcon.ENTER -> {
                path.moveTo(gx(18f), gy(6f))
                path.lineTo(gx(18f), gy(14f))
                path.lineTo(gx(6f), gy(14f))
                canvas.drawPath(path, strokePaint)
                path.reset()
                path.moveTo(gx(10f), gy(10f))
                path.lineTo(gx(6f), gy(14f))
                path.lineTo(gx(10f), gy(18f))
                canvas.drawPath(path, strokePaint)
            }
            SymbolicIcon.GLOBE -> {
                canvas.drawCircle(gx(12f), gy(12f), 7.5f * scale, strokePaint)
                canvas.drawLine(gx(4.5f), gy(12f), gx(19.5f), gy(12f), strokePaint)
                canvas.drawOval(RectF(gx(8.5f), gy(4.5f), gx(15.5f), gy(19.5f)), strokePaint)
            }
            SymbolicIcon.PIN -> {
                path.moveTo(gx(9f), gy(5f))
                path.lineTo(gx(15f), gy(5f))
                path.lineTo(gx(14f), gy(11f))
                path.lineTo(gx(17f), gy(14f))
                path.lineTo(gx(7f), gy(14f))
                path.lineTo(gx(10f), gy(11f))
                path.close()
                canvas.drawPath(path, strokePaint)
                canvas.drawLine(gx(12f), gy(14f), gx(12f), gy(20f), strokePaint)
            }
            SymbolicIcon.DELETE -> {
                canvas.drawLine(gx(5f), gy(7f), gx(19f), gy(7f), strokePaint)
                canvas.drawRoundRect(RectF(gx(7f), gy(7f), gx(17f), gy(19.5f)), 1.5f * scale, 1.5f * scale, strokePaint)
                canvas.drawLine(gx(9.5f), gy(5f), gx(14.5f), gy(5f), strokePaint)
            }
            SymbolicIcon.SEARCH -> {
                canvas.drawCircle(gx(10.5f), gy(10.5f), 5.5f * scale, strokePaint)
                canvas.drawLine(gx(14.8f), gy(14.8f), gx(19.5f), gy(19.5f), strokePaint)
            }
            SymbolicIcon.KEYBOARD -> {
                canvas.drawRoundRect(RectF(gx(3.5f), gy(6.5f), gx(20.5f), gy(17.5f)), 2f * scale, 2f * scale, strokePaint)
                canvas.drawLine(gx(7f), gy(10f), gx(8.5f), gy(10f), strokePaint)
                canvas.drawLine(gx(11.2f), gy(10f), gx(12.8f), gy(10f), strokePaint)
                canvas.drawLine(gx(15.5f), gy(10f), gx(17f), gy(10f), strokePaint)
                canvas.drawLine(gx(8f), gy(14.2f), gx(16f), gy(14.2f), strokePaint)
            }
            SymbolicIcon.APPEARANCE -> {
                canvas.drawCircle(gx(12f), gy(12f), 7.5f * scale, strokePaint)
                canvas.drawArc(RectF(gx(4.5f), gy(4.5f), gx(19.5f), gy(19.5f)), 270f, 180f, true, fillPaint)
            }
            SymbolicIcon.TYPING -> {
                canvas.drawLine(gx(5f), gy(7f), gx(19f), gy(7f), strokePaint)
                canvas.drawLine(gx(5f), gy(12f), gx(15f), gy(12f), strokePaint)
                canvas.drawLine(gx(5f), gy(17f), gx(12f), gy(17f), strokePaint)
            }
            SymbolicIcon.SOUND -> {
                path.moveTo(gx(5f), gy(10f))
                path.lineTo(gx(9f), gy(10f))
                path.lineTo(gx(14f), gy(6f))
                path.lineTo(gx(14f), gy(18f))
                path.lineTo(gx(9f), gy(14f))
                path.lineTo(gx(5f), gy(14f))
                path.close()
                canvas.drawPath(path, strokePaint)
                canvas.drawArc(RectF(gx(13f), gy(8f), gx(19f), gy(16f)), -45f, 90f, false, strokePaint)
            }
            SymbolicIcon.INFO -> {
                canvas.drawCircle(gx(12f), gy(12f), 8f * scale, strokePaint)
                canvas.drawCircle(gx(12f), gy(8.2f), 1.2f * scale, fillPaint)
                canvas.drawLine(gx(12f), gy(11.5f), gx(12f), gy(16.5f), strokePaint)
            }
            SymbolicIcon.RECENT_CLOCK -> {
                canvas.drawCircle(gx(12f), gy(12f), 7.5f * scale, strokePaint)
                canvas.drawLine(gx(12f), gy(8f), gx(12f), gy(12f), strokePaint)
                canvas.drawLine(gx(12f), gy(12f), gx(15f), gy(14f), strokePaint)
            }
            SymbolicIcon.STAR -> {
                path.moveTo(gx(12f), gy(4.5f))
                path.lineTo(gx(14.3f), gy(9.5f))
                path.lineTo(gx(19.5f), gy(10.2f))
                path.lineTo(gx(15.7f), gy(14f))
                path.lineTo(gx(16.7f), gy(19.2f))
                path.lineTo(gx(12f), gy(16.6f))
                path.lineTo(gx(7.3f), gy(19.2f))
                path.lineTo(gx(8.3f), gy(14f))
                path.lineTo(gx(4.5f), gy(10.2f))
                path.lineTo(gx(9.7f), gy(9.5f))
                path.close()
                canvas.drawPath(path, strokePaint)
            }
            SymbolicIcon.PLUS -> {
                canvas.drawLine(gx(12f), gy(6f), gx(12f), gy(18f), strokePaint)
                canvas.drawLine(gx(6f), gy(12f), gx(18f), gy(12f), strokePaint)
            }
            SymbolicIcon.MINUS -> {
                canvas.drawLine(gx(6f), gy(12f), gx(18f), gy(12f), strokePaint)
            }
            SymbolicIcon.ARROW_UP -> {
                path.moveTo(gx(7f), gy(14f))
                path.lineTo(gx(12f), gy(9f))
                path.lineTo(gx(17f), gy(14f))
                canvas.drawPath(path, strokePaint)
            }
            SymbolicIcon.ARROW_DOWN -> {
                path.moveTo(gx(7f), gy(10f))
                path.lineTo(gx(12f), gy(15f))
                path.lineTo(gx(17f), gy(10f))
                canvas.drawPath(path, strokePaint)
            }
        }
    }

    fun createDrawable(context: Context, icon: SymbolicIcon, sizeDp: Int, color: Int): Drawable {
        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(16)
        return object : Drawable() {
            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val b = bounds
                val w = if (b.width() > 0) b.width().toFloat() else sizePx.toFloat()
                val h = if (b.height() > 0) b.height().toFloat() else sizePx.toFloat()
                val cx = b.left + w / 2f
                val cy = b.top + h / 2f
                val s = minOf(w, h)
                AdwaitaIcons.draw(canvas, icon, cx, cy, s, color)
            }

            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: ColorFilter?) {}
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply {
            setBounds(0, 0, sizePx, sizePx)
        }
    }

    @Composable
    fun SymbolicIconView(
        icon: SymbolicIcon,
        tint: Int,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        ComposeCanvas(modifier = modifier.size(size)) {
            val s = this.size.minDimension
            drawContext.canvas.nativeCanvas.let { native ->
                draw(
                    canvas = native,
                    icon = icon,
                    cx = this.size.width / 2f,
                    cy = this.size.height / 2f,
                    sizePx = s,
                    color = tint
                )
            }
        }
    }
}
