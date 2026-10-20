package com.hunterbrewer.livingroomlauncher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.KeyEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

/**
 * A single lightweight Canvas view: no Compose runtime, no RecyclerView, and no background network work.
 * Coordinates use a 1920x1080 design surface and scale to the panel resolution.
 */
@SuppressLint("ViewConstructor")
class LauncherView(context: Context, private val actions: Actions) : View(context) {
    interface Actions {
        fun launch(entry: AppEntry)
        fun saveOrder(entries: List<AppEntry>)
        fun openWifi()
        fun openSettings()
        fun switchInput(input: TvInput)
        fun openJellyfin()
    }

    data class TvInput(val label: String, val detail: String, val id: String)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val rect = RectF()
    private val path = Path()
    private val apps = mutableListOf<AppEntry>()
    private var nav = NavigationState.grid(0, COLUMNS, 0)
    private var editMode = false
    private var centerHeld = false
    private var longPressTriggered = false
    private var selectedHeroPackage: String? = null

    private val regular: Typeface = if (Build.VERSION.SDK_INT >= 26) resources.getFont(R.font.outfit) else Typeface.create("sans", Typeface.NORMAL)
    private val light: Typeface = Typeface.create(regular, Typeface.NORMAL)
    private val medium: Typeface = Typeface.create(regular, Typeface.BOLD)

    private val clock = SimpleDateFormat("h:mm", Locale.US)
    private val amPm = SimpleDateFormat("a", Locale.US)
    private val date = SimpleDateFormat("EEEE   MMMM d", Locale.US)

    private val inputs = listOf(
        TvInput("HDMI 1", "HW4", "com.hisense.tv.hitvinput/.hdmi.HdmiTvInputService/HW4"),
        TvInput("HDMI 2", "HW5", "com.hisense.tv.hitvinput/.hdmi.HdmiTvInputService/HW5"),
        TvInput("HDMI 3", "HW6", "com.hisense.tv.hitvinput/.hdmi.HdmiTvInputService/HW6"),
        TvInput("HDMI 4", "HW7", "com.hisense.tv.hitvinput/.hdmi.HdmiTvInputService/HW7"),
        TvInput("AV", "Composite", "com.hisense.tv.hitvinput/.composite.CompositeTvInputService/HW1"),
        TvInput("Live TV", "Antenna", "com.hisense.tv.hitvinput/.tuner.TunerTvInputService/HW0")
    )

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setBackgroundColor(Color.BLACK)
    }

    fun setApps(entries: List<AppEntry>) {
        val focusedPackage = apps.getOrNull(nav.index)?.packageName
        apps.clear()
        apps.addAll(entries)
        val nextIndex = focusedPackage?.let { pkg -> apps.indexOfFirst { it.packageName == pkg }.takeIf { it >= 0 } } ?: nav.index
        nav = NavigationState.grid(nextIndex.coerceIn(0, max(0, apps.lastIndex)), COLUMNS, apps.size)
        if (selectedHeroPackage == null) selectedHeroPackage = apps.firstOrNull()?.packageName
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sx = width / DESIGN_W
        val sy = height / DESIGN_H
        canvas.save()
        canvas.scale(sx, sy)
        drawWallpaper(canvas)
        drawHero(canvas)
        drawHeader(canvas)
        drawGrid(canvas)
        if (nav.zone == Zone.INPUTS) drawInputMenu(canvas)
        if (editMode) drawEditHint(canvas)
        canvas.restore()
    }

    /** One clock tick per minute boundary, owned by the view lifecycle (never re-armed from onDraw). */
    private val clockTick = object : Runnable {
        override fun run() {
            invalidate()
            val now = System.currentTimeMillis()
            postDelayed(this, 60_000L - (now % 60_000L) + 50L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        removeCallbacks(clockTick)
        post(clockTick)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(clockTick)
        super.onDetachedFromWindow()
    }

    private fun drawWallpaper(c: Canvas) {
        val gradient = LinearGradient(0f, 0f, 0f, DESIGN_H, intArrayOf(Color.rgb(10, 23, 48), Color.rgb(29, 58, 95), Color.rgb(223, 136, 89)), null, Shader.TileMode.CLAMP)
        paint.shader = gradient
        c.drawRect(0f, 0f, DESIGN_W, DESIGN_H, paint)
        paint.shader = null

        path.reset()
        path.moveTo(0f, 760f); path.lineTo(220f, 610f); path.lineTo(380f, 700f); path.lineTo(560f, 540f)
        path.lineTo(760f, 690f); path.lineTo(920f, 600f); path.lineTo(1120f, 720f); path.lineTo(1340f, 560f)
        path.lineTo(1560f, 690f); path.lineTo(1760f, 600f); path.lineTo(1920f, 660f); path.lineTo(1920f, 1080f); path.lineTo(0f, 1080f); path.close()
        paint.color = Color.rgb(38, 51, 77); c.drawPath(path, paint)
        path.reset(); path.moveTo(0f, 870f); path.lineTo(280f, 740f); path.lineTo(500f, 835f); path.lineTo(730f, 720f)
        path.lineTo(970f, 850f); path.lineTo(1220f, 740f); path.lineTo(1480f, 860f); path.lineTo(1710f, 760f); path.lineTo(1920f, 830f)
        path.lineTo(1920f, 1080f); path.lineTo(0f, 1080f); path.close(); paint.color = Color.rgb(19, 31, 51); c.drawPath(path, paint)
        paint.color = Color.argb(70, 0, 0, 0); c.drawRect(0f, 0f, DESIGN_W, DESIGN_H, paint)
    }

    private fun drawHeader(c: Canvas) {
        paint.shader = LinearGradient(0f, 0f, 0f, 230f, Color.argb(145, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, DESIGN_W, 230f, paint); paint.shader = null

        val now = Date()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        label(c, TimeLabels.greeting(hour), 112f, 79f, 25f, Color.argb(165, 255, 255, 255), medium, 5f)
        label(c, clock.format(now), 104f, 181f, 112f, Color.WHITE, light)
        label(c, amPm.format(now), 362f, 174f, 30f, Color.argb(190, 255, 255, 255), medium, 2f)
        paint.color = Color.argb(80, 255, 255, 255); c.drawRoundRect(434f, 116f, 436f, 177f, 1f, 1f, paint)
        label(c, date.format(now).uppercase(Locale.US), 470f, 165f, 30f, Color.argb(170, 255, 255, 255), regular, 3f)

        val names = arrayOf("Inputs", "Wi-Fi", "Settings")
        val widths = floatArrayOf(196f, 188f, 214f)
        var x = 1255f
        for (i in names.indices) {
            val focused = nav.zone == Zone.HEADER && nav.index == i || nav.zone == Zone.INPUTS && i == 0
            drawPill(c, x, 112f, widths[i], 84f, names[i], i, focused)
            x += widths[i] + 20f
        }
    }

    private fun drawPill(c: Canvas, x: Float, y: Float, w: Float, h: Float, name: String, icon: Int, focused: Boolean) {
        rect.set(x, y, x + w, y + h)
        paint.color = if (focused) Color.WHITE else Color.argb(42, 255, 255, 255)
        paint.style = Paint.Style.FILL; c.drawRoundRect(rect, 45f, 45f, paint)
        paint.color = if (focused) Color.WHITE else Color.argb(65, 255, 255, 255)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f; c.drawRoundRect(rect, 45f, 45f, paint); paint.style = Paint.Style.FILL
        val fg = if (focused) Color.rgb(20, 22, 27) else Color.WHITE
        drawHeaderIcon(c, icon, x + 31f, y + 25f, fg)
        label(c, name, x + 78f, y + 54f, 29f, fg, medium)
    }

    private fun drawHeaderIcon(c: Canvas, type: Int, x: Float, y: Float, color: Int) {
        paint.color = color; paint.style = Paint.Style.STROKE; paint.strokeWidth = 3.2f; paint.strokeCap = Paint.Cap.ROUND
        when (type) {
            0 -> { rect.set(x, y + 4f, x + 34f, y + 29f); c.drawRoundRect(rect, 4f, 4f, paint); c.drawLine(x + 8f, y + 11f, x + 26f, y + 11f, paint) }
            1 -> { c.drawArc(x, y, x + 36f, y + 29f, 215f, 110f, false, paint); c.drawArc(x + 7f, y + 8f, x + 29f, y + 29f, 215f, 110f, false, paint); paint.style = Paint.Style.FILL; c.drawCircle(x + 18f, y + 30f, 2.4f, paint) }
            else -> { c.drawCircle(x + 18f, y + 18f, 12f, paint); c.drawCircle(x + 18f, y + 18f, 4.5f, paint); for (i in 0..7) { val a = Math.toRadians((i * 45).toDouble()); c.drawLine(x + 18f + (13 * kotlin.math.cos(a)).toFloat(), y + 18f + (13 * kotlin.math.sin(a)).toFloat(), x + 18f + (17 * kotlin.math.cos(a)).toFloat(), y + 18f + (17 * kotlin.math.sin(a)).toFloat(), paint) } }
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawHero(c: Canvas) {
        if (nav.zone == Zone.GRID && nav.index / COLUMNS > 0) return
        val app = apps.firstOrNull { it.packageName == selectedHeroPackage } ?: apps.firstOrNull() ?: return
        val color = packageColor(app.packageName)
        paint.shader = RadialGradient(1460f, 380f, 920f, lighten(color), Color.argb(25, 0, 0, 0), Shader.TileMode.CLAMP)
        c.drawRect(0f, 210f, DESIGN_W, 650f, paint); paint.shader = null
        if (app.packageName == "org.jellyfin.androidtv") drawJellyfinShelf(c, app) else drawAppHero(c, app)
    }

    private fun drawAppHero(c: Canvas, app: AppEntry) {
        drawDrawable(c, app.icon, 150f, 300f, 150f, 150f)
        label(c, app.label, 330f, 380f, 66f, Color.WHITE, medium)
        label(c, "Press OK to open", 334f, 430f, 26f, Color.argb(180, 255, 255, 255), regular)
    }

    private fun drawJellyfinShelf(c: Canvas, app: AppEntry) {
        label(c, "Continue Watching", 112f, 292f, 34f, Color.WHITE, medium)
        val names = arrayOf("Open Jellyfin", "Movies", "Shows", "Live TV")
        val shades = intArrayOf(Color.rgb(40, 123, 163), Color.rgb(113, 69, 155), Color.rgb(28, 95, 112), Color.rgb(46, 84, 143))
        for (i in names.indices) {
            val x = 112f + i * 425f
            rect.set(x, 320f, x + 385f, 510f)
            paint.shader = LinearGradient(x, 320f, x + 385f, 510f, shades[i], Color.rgb(18, 25, 42), Shader.TileMode.CLAMP)
            c.drawRoundRect(rect, 18f, 18f, paint); paint.shader = null
            if (i == 0) drawDrawable(c, app.icon, x + 135f, 347f, 110f, 110f)
            label(c, names[i], x + 22f, 492f, 27f, Color.WHITE, medium)
        }
        label(c, "Jellyfin account data stays inside the Jellyfin app", 112f, 552f, 22f, Color.argb(145, 255, 255, 255), regular)
    }

    private fun drawGrid(c: Canvas) {
        if (apps.isEmpty()) {
            label(c, "No TV apps found", 112f, 720f, 32f, Color.WHITE, medium)
            return
        }
        val focusedRow = if (nav.zone == Zone.GRID) nav.index / COLUMNS else 0
        val baseY = if (focusedRow == 0) 665f else 275f - max(0, focusedRow - 1) * ROW_STEP
        apps.forEachIndexed { index, app ->
            val row = index / COLUMNS
            val col = index % COLUMNS
            val x = 90f + col * 357f
            val y = baseY + row * ROW_STEP
            if (y < 205f || y > DESIGN_H + 100f) return@forEachIndexed
            drawAppCard(c, app, x, y, nav.zone == Zone.GRID && nav.index == index)
        }
    }

    private fun drawAppCard(c: Canvas, app: AppEntry, x: Float, y: Float, focused: Boolean) {
        c.save()
        if (focused) {
            val cx = x + CARD_W / 2f; val cy = y + CARD_H / 2f
            c.scale(1.14f, 1.14f, cx, cy)
            paint.color = Color.argb(105, 0, 0, 0)
            rect.set(x - 18f, y + 12f, x + CARD_W + 18f, y + CARD_H + 36f)
            c.drawRoundRect(rect, 28f, 28f, paint)
        }
        rect.set(x, y, x + CARD_W, y + CARD_H)
        paint.color = packageColor(app.packageName); c.drawRoundRect(rect, 20f, 20f, paint)
        paint.shader = LinearGradient(x, y, x, y + CARD_H, Color.argb(25, 255, 255, 255), Color.argb(35, 0, 0, 0), Shader.TileMode.CLAMP)
        c.drawRoundRect(rect, 20f, 20f, paint); paint.shader = null
        drawDrawable(c, app.icon, x + 106f, y + 39f, 100f, 100f)
        c.restore()
        if (focused) label(c, app.label, x + CARD_W / 2f, y + CARD_H + 51f, 27f, Color.WHITE, medium, align = Paint.Align.CENTER)
    }

    private fun drawInputMenu(c: Canvas) {
        val x = 1310f; val y = 220f; val w = 520f; val h = 590f
        paint.color = Color.argb(225, 25, 28, 35); rect.set(x, y, x + w, y + h); c.drawRoundRect(rect, 34f, 34f, paint)
        label(c, "INPUTS", x + 26f, y + 50f, 22f, Color.argb(150, 255, 255, 255), medium, 4f)
        inputs.forEachIndexed { i, input ->
            val iy = y + 70f + i * 82f
            val focused = nav.index == i
            if (focused) { paint.color = Color.WHITE; rect.set(x + 15f, iy, x + w - 15f, iy + 72f); c.drawRoundRect(rect, 20f, 20f, paint) }
            val fg = if (focused) Color.rgb(18, 21, 27) else Color.WHITE
            drawHeaderIcon(c, if (i < 5) 0 else 1, x + 38f, iy + 18f, fg)
            label(c, input.label, x + 97f, iy + 46f, 29f, fg, medium)
            label(c, input.detail, x + w - 35f, iy + 44f, 21f, if (focused) Color.rgb(95, 98, 105) else Color.argb(125, 255, 255, 255), regular, align = Paint.Align.RIGHT)
        }
    }

    private fun drawEditHint(c: Canvas) {
        paint.color = Color.argb(220, 26, 28, 34); rect.set(650f, 1000f, 1270f, 1058f); c.drawRoundRect(rect, 30f, 30f, paint)
        label(c, "REARRANGING  ·  ARROWS MOVE  ·  OK TO SAVE", 960f, 1038f, 23f, Color.WHITE, medium, align = Paint.Align.CENTER)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (event.repeatCount == 0) { centerHeld = true; longPressTriggered = false }
                else if (event.repeatCount >= 2 && nav.zone == Zone.GRID && !editMode) { editMode = true; longPressTriggered = true; invalidate() }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> return navigate(Direction.LEFT)
            KeyEvent.KEYCODE_DPAD_RIGHT -> return navigate(Direction.RIGHT)
            KeyEvent.KEYCODE_DPAD_UP -> return navigate(Direction.UP)
            KeyEvent.KEYCODE_DPAD_DOWN -> return navigate(Direction.DOWN)
            KeyEvent.KEYCODE_BACK -> {
                when {
                    nav.zone == Zone.INPUTS -> nav = NavigationState.header(0, COLUMNS, apps.size)
                    editMode -> { editMode = false; actions.saveOrder(apps) }
                    else -> return true
                }
                invalidate(); return true
            }
            KeyEvent.KEYCODE_HOME -> { nav = NavigationState.grid(0, COLUMNS, apps.size); editMode = false; invalidate(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (centerHeld && !longPressTriggered) {
                if (editMode) { editMode = false; actions.saveOrder(apps) } else activate()
            }
            centerHeld = false; longPressTriggered = false; invalidate(); return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun navigate(direction: Direction): Boolean {
        if (editMode && nav.zone == Zone.GRID) {
            val target = nav.move(direction)
            if (target.zone == Zone.GRID && target.index != nav.index) {
                val moved = AppOrder.move(apps, nav.index, target.index)
                apps.clear(); apps.addAll(moved); nav = target
                selectedHeroPackage = apps.getOrNull(nav.index)?.packageName
            }
        } else {
            nav = nav.move(direction)
            if (nav.zone == Zone.GRID) selectedHeroPackage = apps.getOrNull(nav.index)?.packageName
        }
        invalidate(); return true
    }

    private fun activate() {
        when (nav.zone) {
            Zone.GRID -> apps.getOrNull(nav.index)?.let(actions::launch)
            Zone.HEADER -> when (nav.index) {
                0 -> nav = NavigationState.inputs(0, inputs.size)
                1 -> actions.openWifi()
                else -> actions.openSettings()
            }
            Zone.INPUTS -> inputs.getOrNull(nav.index)?.let(actions::switchInput)
        }
        invalidate()
    }

    private fun drawDrawable(c: Canvas, drawable: Drawable, x: Float, y: Float, w: Float, h: Float) {
        val old = drawable.bounds
        drawable.setBounds(x.toInt(), y.toInt(), (x + w).toInt(), (y + h).toInt())
        drawable.draw(c)
        drawable.bounds = old
    }

    private fun label(c: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int, face: Typeface, spacing: Float = 0f, align: Paint.Align = Paint.Align.LEFT) {
        text.textSize = size; text.color = color; text.typeface = face; text.textAlign = align; text.letterSpacing = if (size > 0) spacing / size else 0f
        c.drawText(value, x, baseline, text)
    }

    private fun packageColor(pkg: String): Int = when (pkg) {
        "org.jellyfin.androidtv" -> Color.rgb(77, 73, 170)
        "com.netflix.ninja" -> Color.rgb(10, 10, 10)
        "com.disney.disneyplus" -> Color.rgb(8, 45, 120)
        "com.amazon.amazonvideo.livingroom" -> Color.rgb(15, 23, 30)
        "com.tubitv" -> Color.rgb(102, 0, 238)
        "com.fubo.firetv.screen" -> Color.rgb(250, 70, 22)
        "com.hulu.livingroomplus" -> Color.rgb(20, 175, 105)
        "com.wbd.stream" -> Color.rgb(0, 39, 184)
        "com.peacocktv.peacockandroid" -> Color.BLACK
        "com.google.android.youtube.tv" -> Color.rgb(235, 25, 25)
        "com.spotify.tv.android" -> Color.rgb(18, 18, 18)
        "com.android.vending" -> Color.rgb(10, 93, 220)
        else -> Color.rgb(40, 47, 62)
    }

    private fun lighten(color: Int): Int = Color.rgb((Color.red(color) + 70).coerceAtMost(255), (Color.green(color) + 70).coerceAtMost(255), (Color.blue(color) + 70).coerceAtMost(255))

    companion object {
        private const val DESIGN_W = 1920f
        private const val DESIGN_H = 1080f
        private const val COLUMNS = 5
        private const val CARD_W = 312f
        private const val CARD_H = 187f
        private const val ROW_STEP = 270f
    }
}
