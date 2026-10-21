package com.hunterbrewer.livingroomlauncher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
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
        fun uninstall(entry: AppEntry)
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
    // Hold-OK app menu: which app it's for and which option is focused.
    private var menuApp: AppEntry? = null
    private var menuOptions: List<MenuOption> = emptyList()
    private var menuIndex = 0
    // Greeting letter-spacing is fitted to the clock+date line width; cached per greeting/date text.
    private var greetingKey = ""
    private var greetingEm = 0f
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadowPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { color = Color.BLACK }
    private val shadow: Bitmap = CardArt.shadow()
    private val dst = RectF()

    // Focus animation: the newly focused card grows in while the previous one settles back.
    private var focusedKey: String? = null
    private var previousKey: String? = null
    private var focusT = 1f
    private var animStart = 0L
    private val animRunner = object : Runnable {
        override fun run() {
            focusT = ((System.currentTimeMillis() - animStart) / FOCUS_MS.toFloat()).coerceIn(0f, 1f)
            invalidate()
            if (focusT < 1f) postOnAnimation(this)
        }
    }

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
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sx = width / DESIGN_W
        val sy = height / DESIGN_H
        val key = if (nav.zone == Zone.GRID) apps.getOrNull(nav.index)?.packageName else null
        if (key != focusedKey) {
            previousKey = focusedKey; focusedKey = key
            focusT = 0f; animStart = System.currentTimeMillis()
            removeCallbacks(animRunner); postOnAnimation(animRunner)
        }
        canvas.save()
        canvas.scale(sx, sy)
        drawWallpaper(canvas)
        drawGrid(canvas)
        drawHeader(canvas)
        if (nav.zone == Zone.INPUTS) drawInputMenu(canvas)
        if (editMode) drawEditHint(canvas)
        menuApp?.let { drawAppMenu(canvas, it) }
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
        removeCallbacks(animRunner)
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
        val greeting = TimeLabels.greeting(hour)
        val dateText = date.format(now).uppercase(Locale.US)
        label(c, clock.format(now), 104f, 181f, 112f, Color.WHITE, light)
        label(c, amPm.format(now), 362f, 174f, 30f, Color.argb(190, 255, 255, 255), medium, 2f)
        paint.color = Color.argb(80, 255, 255, 255); c.drawRoundRect(434f, 116f, 436f, 177f, 1f, 1f, paint)
        label(c, dateText, DATE_X, 165f, DATE_SIZE, Color.argb(170, 255, 255, 255), regular, 3f)
        drawGreeting(c, greeting, dateText)

        val names = arrayOf("Inputs", "Wi-Fi", "Settings")
        val widths = floatArrayOf(196f, 188f, 214f)
        var x = 1255f
        for (i in names.indices) {
            val focused = nav.zone == Zone.HEADER && nav.index == i || nav.zone == Zone.INPUTS && i == 0
            drawPill(c, x, 112f, widths[i], 84f, names[i], i, focused)
            x += widths[i] + 20f
        }
    }

    /** The greeting stretches (slightly larger + letter-spaced) from the clock's left edge to the date's right edge. */
    private fun drawGreeting(c: Canvas, greeting: String, dateText: String) {
        val key = "$greeting|$dateText"
        if (key != greetingKey) {
            text.typeface = regular; text.textSize = DATE_SIZE; text.letterSpacing = 3f / DATE_SIZE
            val dateInkEnd = DATE_X + text.measureText(dateText) - text.letterSpacing * DATE_SIZE / 2f
            text.typeface = medium; text.textSize = GREET_SIZE
            text.letterSpacing = 0f; val w0 = text.measureText(greeting)
            text.letterSpacing = 0.1f; val w1 = text.measureText(greeting)
            greetingEm = TextFit.spacingToFill(w0, w1, 0.1f, dateInkEnd - GREET_X, 1.2f, trimPerEm = GREET_SIZE)
            greetingKey = key
        }
        text.typeface = medium; text.textSize = GREET_SIZE; text.textAlign = Paint.Align.LEFT
        text.letterSpacing = greetingEm; text.color = Color.argb(165, 255, 255, 255)
        c.drawText(greeting, GREET_X - greetingEm * GREET_SIZE / 2f, 82f, text)
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

    private fun drawGrid(c: Canvas) {
        if (apps.isEmpty()) {
            label(c, "No TV apps found", 112f, GRID_TOP + 60f, 32f, Color.WHITE, medium)
            return
        }
        // Grid sits directly under the header; scroll only once focus passes the last visible row.
        val focusedRow = if (nav.zone == Zone.GRID) nav.index / COLUMNS else 0
        val firstVisibleRow = max(0, focusedRow - (VISIBLE_ROWS - 1))
        val baseY = GRID_TOP - firstVisibleRow * ROW_STEP
        apps.forEachIndexed { index, app ->
            val row = index / COLUMNS
            val col = index % COLUMNS
            val x = 90f + col * 357f
            val y = baseY + row * ROW_STEP
            if (y < GRID_TOP - 20f || y > DESIGN_H) return@forEachIndexed
            drawAppCard(c, app, x, y, nav.zone == Zone.GRID && nav.index == index)
        }
    }

    private fun drawAppCard(c: Canvas, app: AppEntry, x: Float, y: Float, focused: Boolean) {
        val ease = 1f - (1f - focusT) * (1f - focusT) * (1f - focusT)
        val lift = when {
            focused -> ease
            app.packageName == previousKey -> 1f - ease
            else -> 0f
        }
        val scale = 1f + 0.14f * lift
        val cx = x + CARD_W / 2f; val cy = y + CARD_H / 2f
        c.save()
        c.translate(0f, -8f * lift)
        c.scale(scale, scale, cx, cy)
        if (lift > 0.01f) {
            val pad = CardArt.SHADOW_PAD * CARD_W / CardArt.W
            dst.set(x - pad, y - pad, x + CARD_W + pad, y + CARD_H + pad)
            shadowPaint.alpha = (255 * lift).toInt()
            c.drawBitmap(shadow, null, dst, shadowPaint)
        }
        dst.set(x, y, x + CARD_W, y + CARD_H)
        bitmapPaint.alpha = if (nav.zone == Zone.GRID && !focused && lift < 0.01f) 225 else 255
        c.drawBitmap(app.card, null, dst, bitmapPaint)
        bitmapPaint.alpha = 255
        if (lift > 0.01f) drawShine(c, x, y, lift)
        if (editMode && focused) {
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f; paint.color = Color.WHITE
            c.drawRoundRect(dst, 20f, 20f, paint); paint.style = Paint.Style.FILL
        }
        c.restore()
        if (focused) label(c, app.label, x + CARD_W / 2f, y + CARD_H + 56f, 27f, withAlpha(Color.WHITE, (255 * ease).toInt()), medium, align = Paint.Align.CENTER)
    }

    /** Apple TV-style glare: a soft diagonal highlight across the top of the focused card. */
    private fun drawShine(c: Canvas, x: Float, y: Float, lift: Float) {
        c.save()
        path.reset(); rect.set(x, y, x + CARD_W, y + CARD_H)
        path.addRoundRect(rect, 20f, 20f, Path.Direction.CW)
        c.clipPath(path)
        paint.shader = LinearGradient(x, y, x + CARD_W * 0.7f, y + CARD_H,
            intArrayOf(Color.argb((70 * lift).toInt(), 255, 255, 255), Color.argb((14 * lift).toInt(), 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 0.75f), Shader.TileMode.CLAMP)
        c.drawRect(rect, paint); paint.shader = null
        c.restore()
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
        label(c, "MOVING  ·  ARROWS MOVE  ·  OK TO SAVE", 960f, 1038f, 23f, Color.WHITE, medium, align = Paint.Align.CENTER)
    }

    private fun drawAppMenu(c: Canvas, app: AppEntry) {
        paint.color = Color.argb(150, 0, 0, 0); c.drawRect(0f, 0f, DESIGN_W, DESIGN_H, paint)
        val w = 640f; val rowH = 92f
        val h = 330f + menuOptions.size * (rowH + 12f)
        val x = (DESIGN_W - w) / 2f; val y = (DESIGN_H - h) / 2f
        paint.color = Color.argb(240, 26, 29, 36); rect.set(x, y, x + w, y + h); c.drawRoundRect(rect, 36f, 36f, paint)
        // App card + name on top so it's obvious which app this menu is for.
        val cw = 300f; val ch = cw * CardArt.H / CardArt.W
        dst.set(x + (w - cw) / 2f, y + 34f, x + (w + cw) / 2f, y + 34f + ch)
        c.drawBitmap(app.card, null, dst, bitmapPaint)
        label(c, app.label, x + w / 2f, y + 34f + ch + 56f, 34f, Color.WHITE, medium, align = Paint.Align.CENTER)
        menuOptions.forEachIndexed { i, opt ->
            val ry = y + 330f + i * (rowH + 12f)
            val focused = i == menuIndex
            rect.set(x + 30f, ry, x + w - 30f, ry + rowH)
            paint.color = if (focused) Color.WHITE else Color.argb(30, 255, 255, 255); c.drawRoundRect(rect, 24f, 24f, paint)
            val (title, sub) = when (opt) {
                MenuOption.MOVE -> "Move" to "Rearrange on the home screen"
                MenuOption.UNINSTALL -> "Uninstall" to "Deletes the app and its login"
            }
            val titleColor = when {
                opt == MenuOption.UNINSTALL && focused -> Color.rgb(214, 40, 40)
                opt == MenuOption.UNINSTALL -> Color.rgb(255, 120, 120)
                focused -> Color.rgb(20, 22, 27)
                else -> Color.WHITE
            }
            label(c, title, x + 64f, ry + 44f, 31f, titleColor, medium)
            label(c, sub, x + 64f, ry + 76f, 21f, if (focused) Color.rgb(95, 98, 105) else Color.argb(140, 255, 255, 255), regular)
        }
    }

    private fun openMenu(app: AppEntry) {
        menuApp = app; menuOptions = AppMenu.options(app.removable); menuIndex = 0
    }

    private fun menuKey(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> menuIndex = (menuIndex - 1).coerceAtLeast(0)
            KeyEvent.KEYCODE_DPAD_DOWN -> menuIndex = (menuIndex + 1).coerceAtMost(menuOptions.lastIndex)
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME -> menuApp = null
            else -> return true
        }
        invalidate(); return true
    }

    private fun chooseMenuOption() {
        val app = menuApp ?: return
        val option = menuOptions.getOrNull(menuIndex)
        menuApp = null
        when (option) {
            MenuOption.MOVE -> editMode = true
            MenuOption.UNINSTALL -> actions.uninstall(app)
            null -> {}
        }
        invalidate()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (menuApp != null && keyCode != KeyEvent.KEYCODE_DPAD_CENTER && keyCode != KeyEvent.KEYCODE_ENTER) return menuKey(keyCode)
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (event.repeatCount == 0) { centerHeld = true; longPressTriggered = false }
                else if (event.repeatCount >= 2 && nav.zone == Zone.GRID && !editMode && menuApp == null && !longPressTriggered) {
                    apps.getOrNull(nav.index)?.let { openMenu(it) }
                    longPressTriggered = true; invalidate()
                }
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
                when {
                    menuApp != null -> chooseMenuOption()
                    editMode -> { editMode = false; actions.saveOrder(apps) }
                    else -> activate()
                }
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
            }
        } else {
            nav = nav.move(direction)
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

    private fun label(c: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int, face: Typeface, spacing: Float = 0f, align: Paint.Align = Paint.Align.LEFT) {
        text.textSize = size; text.color = color; text.typeface = face; text.textAlign = align; text.letterSpacing = if (size > 0) spacing / size else 0f
        c.drawText(value, x, baseline, text)
    }

    private fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)


    companion object {
        private const val DESIGN_W = 1920f
        private const val DESIGN_H = 1080f
        private const val COLUMNS = 5
        private const val CARD_W = 312f
        private const val CARD_H = 187f
        private const val ROW_STEP = 270f
        private const val FOCUS_MS = 170L
        private const val GRID_TOP = 300f
        private const val VISIBLE_ROWS = 3
        private const val GREET_X = 112f
        private const val GREET_SIZE = 30f
        private const val DATE_X = 470f
        private const val DATE_SIZE = 30f
    }
}
