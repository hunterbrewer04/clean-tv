package com.hunterbrewer.livingroomlauncher

import android.graphics.*
import android.graphics.drawable.Drawable

/**
 * Pre-renders each app tile once into a rounded bitmap so onDraw only blits.
 * Uses the app's own Android TV banner (16:9 art every Leanback app ships) when present;
 * otherwise centers the icon on a background sampled from the icon's own colors.
 */
object CardArt {
    /** Rendered at the focused (1.14x) size so the zoomed tile stays crisp. */
    const val W = 356
    const val H = 214
    private const val RADIUS = 24f

    fun render(banner: Drawable?, icon: Drawable): Pair<Bitmap, Int> {
        val out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val clip = Path().apply { addRoundRect(RectF(0f, 0f, W.toFloat(), H.toFloat()), RADIUS, RADIUS, Path.Direction.CW) }
        c.clipPath(clip)
        val accent: Int
        if (banner != null) {
            accent = sample(banner)
            c.drawColor(accent)
            draw(banner, c, 0, 0, W, H)
        } else {
            accent = sample(icon)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.shader = LinearGradient(0f, 0f, 0f, H.toFloat(), lift(accent, 28), accent, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
            val s = 116
            draw(icon, c, (W - s) / 2, (H - s) / 2, s, s)
        }
        // Thin inner edge so dark banners (Netflix, Peacock) still read as cards on a dark wall.
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.argb(40, 255, 255, 255) }
        c.drawRoundRect(RectF(1f, 1f, W - 1f, H - 1f), RADIUS, RADIUS, edge)
        return out to accent
    }

    /** Soft drop shadow, rendered once in software and reused for every focused card. */
    fun shadow(): Bitmap {
        val pad = 60
        val bmp = Bitmap.createBitmap(W + pad * 2, H + pad * 2, Bitmap.Config.ALPHA_8)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL); color = Color.argb(210, 0, 0, 0) }
        Canvas(bmp).drawRoundRect(RectF(pad.toFloat(), pad + 14f, pad + W.toFloat(), pad + H + 14f), RADIUS, RADIUS, p)
        return bmp
    }

    const val SHADOW_PAD = 60

    private fun sample(d: Drawable): Int {
        val small = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        draw(d, Canvas(small), 0, 0, 16, 16)
        val px = IntArray(256)
        small.getPixels(px, 0, 16, 0, 0, 16, 16)
        small.recycle()
        return ColorMath.cardBackground(px)
    }

    private fun draw(d: Drawable, c: Canvas, x: Int, y: Int, w: Int, h: Int) {
        val old = Rect(d.bounds)
        d.setBounds(x, y, x + w, y + h)
        d.draw(c)
        d.bounds = old
    }

    private fun lift(color: Int, by: Int) =
        Color.rgb((Color.red(color) + by).coerceAtMost(255), (Color.green(color) + by).coerceAtMost(255), (Color.blue(color) + by).coerceAtMost(255))
}
