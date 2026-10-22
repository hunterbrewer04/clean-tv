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

/** Vendor remote keycodes the stock launcher used to answer. Values verified on a TCL Google TV (BeyondTV4). */
object AppKeys {
    const val SETTINGS = "settings"
    const val INPUTS = "inputs"
    private val targets = mapOf(
        178 to INPUTS,                                 // KEYCODE_TV_INPUT: TCL routes it via the stock launcher too
        4062 to "com.netflix.ninja",                   // TCL_NETFLIX
        4065 to "com.google.android.youtube.tv",       // TCL_YOUTUBE
        4099 to "com.amazon.amazonvideo.livingroom",   // TCL_PRIMEVIDEO
        4160 to "com.apple.atve.androidtv.appletv",    // TCL_APPLETV
        4077 to SETTINGS                               // TCL_SET (gear): stock opened its quick panel
    )
    fun targetFor(keyCode: Int): String? = targets[keyCode]
}

/** A physical source the Inputs menu can switch to. */
data class InputSource(val id: String, val label: String, val detail: String, val isTuner: Boolean)

/**
 * Turns whatever the TV's TvInputManager reports into the Inputs menu, so no brand needs hardcoded IDs.
 * Pure (no android.media.tv types) so the selection rules run in JVM unit tests.
 */
object TvInputs {
    // Mirrors android.media.tv.TvInputInfo.TYPE_* (stable public API values).
    const val TYPE_TUNER = 0
    const val TYPE_OTHER = 1000
    const val TYPE_COMPOSITE = 1001
    const val TYPE_SVIDEO = 1002
    const val TYPE_SCART = 1003
    const val TYPE_COMPONENT = 1004
    const val TYPE_VGA = 1005
    const val TYPE_DVI = 1006
    const val TYPE_HDMI = 1007
    const val TYPE_DISPLAY_PORT = 1008

    /** Menu order: HDMI first (what people use), tuner last. TYPE_OTHER (AirPlay, app services) never shows. */
    private val rank = listOf(TYPE_HDMI, TYPE_DISPLAY_PORT, TYPE_DVI, TYPE_VGA, TYPE_COMPONENT, TYPE_COMPOSITE, TYPE_SVIDEO, TYPE_SCART, TYPE_TUNER)

    private fun kindName(type: Int) = when (type) {
        TYPE_HDMI -> "HDMI"; TYPE_DISPLAY_PORT -> "DisplayPort"; TYPE_DVI -> "DVI"; TYPE_VGA -> "VGA"
        TYPE_COMPONENT -> "Component"; TYPE_COMPOSITE -> "AV"; TYPE_SVIDEO -> "S-Video"; TYPE_SCART -> "SCART"
        TYPE_TUNER -> "Live TV"; else -> "Input"
    }

    /** What the launcher needs to know about one TvInputInfo. */
    data class Raw(
        val id: String,
        val label: String?,
        val type: Int,
        val parentId: String?,
        val hidden: Boolean,
        val systemApp: Boolean,
        val passthrough: Boolean
    )

    /** Synthetic last entry that opens the TV's own source picker (works even when passthrough doesn't). */
    const val PANEL_ID = "panel"

    fun select(inputs: List<Raw>): List<InputSource> {
        val physical = inputs.filter { raw ->
            when {
                raw.hidden -> false                    // user hid it in TV settings
                raw.parentId != null -> false           // HDMI-CEC devices hang off their port; show the port once
                raw.type == TYPE_TUNER -> raw.systemApp // apps like Fubo/Peacock register "tuner" inputs; skip them
                raw.type in rank -> raw.passthrough || raw.systemApp
                else -> false                           // TYPE_OTHER: AirPlay receivers, app services
            }
        }.sortedWith(compareBy<Raw>({ rank.indexOf(it.type) }, { naturalKey(it.id) })) // hardware (port) order
        // Many TVs expose several tuner inputs (antenna, cable, ATSC 3.0). One Live TV entry is enough, and it must be
        // the panel's real tuner: preloaded system apps (Google TV's Movies app, vendor IPTV) also register as tuners.
        // Hardware inputs get framework-generated ids ending in "/HW<n>", so prefer those.
        val tuners = physical.filter { it.type == TYPE_TUNER }
        val firstTuner = tuners.firstOrNull { isHardwareId(it.id) } ?: tuners.firstOrNull { !it.id.startsWith("com.google.") }
        val kept = physical.filter { it.type != TYPE_TUNER || it === firstTuner }

        // Firmware often labels every port just "HDMI"; number duplicates so the menu stays usable.
        // Tuner labels are whatever the vendor called its service ("TV", "DTV", "Antenna"); always say "Live TV".
        val baseLabels = kept.map { if (it.type == TYPE_TUNER) kindName(TYPE_TUNER) else it.label?.trim().orEmpty().ifEmpty { kindName(it.type) } }
        val counts = baseLabels.groupingBy { it }.eachCount()
        val seen = HashMap<String, Int>()
        return kept.mapIndexed { i, raw ->
            val base = baseLabels[i]
            val n = (seen[base] ?: 0) + 1
            seen[base] = n
            val label = if (counts.getValue(base) > 1) "$base $n" else base
            InputSource(raw.id, label, detailFor(label, raw.type), raw.type == TYPE_TUNER)
        }
    }

    /** Right-hand hint: the port kind when the label was renamed ("PS5" -> "HDMI"), otherwise nothing redundant. */
    private fun detailFor(label: String, type: Int): String = when {
        type == TYPE_TUNER -> "Antenna / Cable"
        type == TYPE_COMPOSITE -> "Composite"
        label.startsWith(kindName(type), ignoreCase = true) -> ""
        else -> kindName(type)
    }

    private val hardwareId = Regex("/HW\\d+$")
    fun isHardwareId(id: String) = hardwareId.containsMatchIn(id)

    /** "HW10" sorts after "HW9": digit runs compare numerically. */
    private fun naturalKey(s: String): String =
        Regex("\\d+").replace(s.lowercase()) { it.value.trimStart('0').padStart(10, '0') }

    fun passthroughUri(inputId: String): String =
        "content://android.media.tv/passthrough/" + URLEncoder.encode(inputId, "UTF-8").replace("+", "%20")
}
