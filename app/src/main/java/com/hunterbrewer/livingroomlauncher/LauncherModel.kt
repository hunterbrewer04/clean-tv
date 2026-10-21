package com.hunterbrewer.livingroomlauncher

import java.net.URLEncoder

enum class Direction { LEFT, RIGHT, UP, DOWN }
enum class Zone { GRID, HEADER, INPUTS }

data class NavigationState(
    val zone: Zone,
    val index: Int,
    val columns: Int,
    val itemCount: Int,
    val inputCount: Int = 0
) {
    fun move(direction: Direction): NavigationState = when (zone) {
        Zone.HEADER -> when (direction) {
            Direction.LEFT -> copy(index = (index - 1).coerceAtLeast(0))
            Direction.RIGHT -> copy(index = (index + 1).coerceAtMost(2))
            Direction.DOWN -> if (itemCount <= 0) this else grid(index.coerceAtMost(itemCount - 1), columns, itemCount)
            Direction.UP -> this
        }
        Zone.INPUTS -> when (direction) {
            Direction.UP -> copy(index = (index - 1).coerceAtLeast(0))
            Direction.DOWN -> copy(index = (index + 1).coerceAtMost(inputCount - 1))
            else -> this
        }
        Zone.GRID -> {
            val row = index / columns
            val column = index % columns
            when (direction) {
                Direction.LEFT -> copy(index = if (column > 0) index - 1 else index)
                Direction.RIGHT -> copy(index = if (column < columns - 1 && index + 1 < itemCount) index + 1 else index)
                Direction.UP -> if (row <= 0) header(column.coerceIn(0, 2), columns, itemCount) else copy(index = index - columns)
                Direction.DOWN -> {
                    val candidate = index + columns
                    copy(index = if (candidate < itemCount) candidate else if (row < (itemCount - 1) / columns) itemCount - 1 else index)
                }
            }
        }
    }

    companion object {
        fun grid(index: Int, columns: Int, itemCount: Int) = NavigationState(Zone.GRID, index, columns, itemCount)
        fun header(index: Int, columns: Int, itemCount: Int) = NavigationState(Zone.HEADER, index, columns, itemCount)
        fun inputs(index: Int, count: Int) = NavigationState(Zone.INPUTS, index, 1, 0, count)
    }
}

object AppOrder {
    fun <T> move(items: List<T>, from: Int, to: Int): List<T> {
        if (from !in items.indices || to !in items.indices || from == to) return items
        val result = items.toMutableList()
        val item = result.removeAt(from)
        result.add(to, item)
        return result
    }

    fun merge(installed: List<String>, saved: List<String>): List<String> {
        val installedSet = installed.toSet()
        val kept = saved.filter { it in installedSet }.distinct().toMutableList()
        kept += installed.filter { it !in kept }
        return kept
    }
}

object TimeLabels {
    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "GOOD MORNING"
        in 12..16 -> "GOOD AFTERNOON"
        in 17..23 -> "GOOD EVENING"
        else -> "GOOD NIGHT"
    }
}

/** Pure ARGB math (no android.graphics) so it runs in JVM unit tests. */
object ColorMath {
    const val FALLBACK: Int = 0xFF282F3E.toInt()

    /** Average the opaque pixels of an icon, then darken so white labels and the shine stay readable. */
    fun cardBackground(pixels: IntArray): Int {
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        for (p in pixels) {
            if ((p ushr 24) and 0xFF < 128) continue
            r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF; n++
        }
        if (n == 0L) return FALLBACK
        var ar = (r / n).toInt(); var ag = (g / n).toInt(); var ab = (b / n).toInt()
        val peak = maxOf(ar, ag, ab)
        if (peak > 150) { ar = ar * 150 / peak; ag = ag * 150 / peak; ab = ab * 150 / peak }
        return (0xFF shl 24) or (ar shl 16) or (ag shl 8) or ab
    }
}

enum class MenuOption { MOVE, UNINSTALL }

/** Hold-OK menu. Built-in/system apps can't be uninstalled for the user, so they only get Move. */
object AppMenu {
    fun options(removable: Boolean): List<MenuOption> =
        if (removable) listOf(MenuOption.MOVE, MenuOption.UNINSTALL) else listOf(MenuOption.MOVE)
}

object TextFit {
    /**
     * Letter spacing (in em) that makes a line's ink span [target] px.
     * [width0] = measured width with no spacing, [widthAtSample] = width at [sampleEm] spacing.
     * Android pads half the spacing before the first and after the last glyph; pass the text size as
     * [trimPerEm] so that padding is excluded from the visible width.
     */
    fun spacingToFill(width0: Float, widthAtSample: Float, sampleEm: Float, target: Float, maxEm: Float, trimPerEm: Float = 0f): Float {
        val growthPerEm = (widthAtSample - width0) / sampleEm - trimPerEm
        if (growthPerEm <= 0f) return 0f
        return ((target - width0) / growthPerEm).coerceIn(0f, maxEm)
    }
}

object TvInputs {
    fun passthroughUri(inputId: String): String =
        "content://android.media.tv/passthrough/" + URLEncoder.encode(inputId, "UTF-8").replace("+", "%20")
}
