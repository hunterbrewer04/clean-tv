package com.hunterbrewer.livingroomlauncher

import android.graphics.*
import android.os.Build
import android.service.dreams.DreamService
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

class ClockDreamService : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        setContentView(ClockView(this))
    }

    /**
     * Static text on an idle screen for hours is how OLED panels burn in, so the whole block drifts a little every
     * minute (see [ClockDrift]). One lifecycle-owned tick per minute; onDraw never schedules anything.
     */
    private class ClockView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
        private val now = Date()
        private val time = SimpleDateFormat("h:mm", Locale.US)
        private val amPm = SimpleDateFormat("a", Locale.US)
        private val date = SimpleDateFormat("EEEE  ·  MMMM d", Locale.US)
        private val face = if (Build.VERSION.SDK_INT >= 26) resources.getFont(R.font.outfit) else Typeface.create("sans", Typeface.NORMAL)

        private val tick = object : Runnable {
            override fun run() {
                invalidate()
                postDelayed(this, ClockDrift.msToNextMinute(System.currentTimeMillis()))
            }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            post(tick)
        }

        override fun onDetachedFromWindow() {
            removeCallbacks(tick)
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            val ms = System.currentTimeMillis()
            now.time = ms
            val (fx, fy) = ClockDrift.offset(ms / 60_000L)
            val cx = width / 2f + fx * width * ClockDrift.MAX_X
            val dy = fy * height * ClockDrift.MAX_Y
            canvas.drawColor(Color.rgb(7, 12, 23))
            paint.typeface = face; paint.color = Color.WHITE; paint.textSize = height * .25f
            canvas.drawText(time.format(now), cx, height * .53f + dy, paint)
            paint.color = Color.argb(170, 255, 255, 255); paint.textSize = height * .05f
            canvas.drawText(amPm.format(now), cx, height * .62f + dy, paint)
            paint.textSize = height * .045f
            canvas.drawText(date.format(now).uppercase(Locale.US), cx, height * .73f + dy, paint)
        }
    }
}
