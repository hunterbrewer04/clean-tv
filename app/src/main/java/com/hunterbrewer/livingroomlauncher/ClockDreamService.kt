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

    private class ClockView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
        private val now = Date()
        private val time = SimpleDateFormat("h:mm", Locale.US)
        private val amPm = SimpleDateFormat("a", Locale.US)
        private val date = SimpleDateFormat("EEEE  ·  MMMM d", Locale.US)
        private val face = if (Build.VERSION.SDK_INT >= 26) resources.getFont(R.font.outfit) else Typeface.create("sans", Typeface.NORMAL)

        override fun onDraw(canvas: Canvas) {
            now.time = System.currentTimeMillis()
            canvas.drawColor(Color.rgb(7, 12, 23))
            paint.typeface = face; paint.color = Color.WHITE; paint.textSize = height * .25f
            canvas.drawText(time.format(now), width / 2f, height * .53f, paint)
            paint.color = Color.argb(170, 255, 255, 255); paint.textSize = height * .05f
            canvas.drawText(amPm.format(now), width / 2f, height * .62f, paint)
            paint.textSize = height * .045f
            canvas.drawText(date.format(now).uppercase(Locale.US), width / 2f, height * .73f, paint)
            postInvalidateDelayed(10_000L)
        }
    }
}
