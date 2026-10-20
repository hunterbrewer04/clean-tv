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

object TvInputs {
    fun passthroughUri(inputId: String): String =
        "content://android.media.tv/passthrough/" + URLEncoder.encode(inputId, "UTF-8").replace("+", "%20")
}
