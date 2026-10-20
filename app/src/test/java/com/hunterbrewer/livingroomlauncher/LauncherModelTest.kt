package com.hunterbrewer.livingroomlauncher

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class LauncherModelTest {
    @Test fun upFromFirstGridRowEntersHeader() {
        val state = NavigationState.grid(index = 3, columns = 5, itemCount = 12)
        assertEquals(NavigationState.header(2, 5, 12), state.move(Direction.UP))
    }

    @Test fun downFromHeaderReturnsToSameGridColumn() {
        val state = NavigationState.header(index = 1, columns = 5, itemCount = 12)
        assertEquals(NavigationState.grid(1, 5, 12), state.move(Direction.DOWN))
    }

    @Test fun emptyCatalogKeepsFocusInHeader() {
        val header = NavigationState.header(index = 1, columns = 5, itemCount = 0)
        assertEquals(header, header.move(Direction.DOWN))
    }

    @Test fun emptyCatalogGridNeverProducesNegativeHeaderIndex() {
        val grid = NavigationState.grid(index = 0, columns = 5, itemCount = 0)
        val up = grid.move(Direction.UP)
        assertEquals(Zone.HEADER, up.zone)
        assertTrue(up.index >= 0)
    }

    @Test fun gridNavigationClampsAtIncompleteLastRow() {
        val state = NavigationState.grid(index = 9, columns = 5, itemCount = 12)
        assertEquals(11, state.move(Direction.DOWN).index)
    }

    @Test fun inputMenuDoesNotMovePastEnds() {
        val top = NavigationState.inputs(0, 6)
        val bottom = NavigationState.inputs(5, 6)
        assertEquals(0, top.move(Direction.UP).index)
        assertEquals(5, bottom.move(Direction.DOWN).index)
    }

    @Test fun reorderMovesFocusedAppAndPreservesAllPackages() {
        val packages = listOf("jellyfin", "netflix", "disney", "prime")
        assertEquals(listOf("netflix", "disney", "jellyfin", "prime"), AppOrder.move(packages, 0, 2))
        assertEquals(packages.sorted(), AppOrder.move(packages, 0, 2).sorted())
    }

    @Test fun savedOrderKeepsKnownOrderAndAppendsNewApps() {
        val installed = listOf("prime", "new-app", "netflix", "jellyfin")
        val saved = listOf("jellyfin", "netflix", "gone-app", "prime")
        assertEquals(listOf("jellyfin", "netflix", "prime", "new-app"), AppOrder.merge(installed, saved))
    }

    @Test fun greetingUsesLocalHourBoundaries() {
        assertEquals("GOOD NIGHT", TimeLabels.greeting(4))
        assertEquals("GOOD MORNING", TimeLabels.greeting(5))
        assertEquals("GOOD AFTERNOON", TimeLabels.greeting(12))
        assertEquals("GOOD EVENING", TimeLabels.greeting(17))
    }

    @Test fun hdmiUriEscapesTvInputId() {
        assertEquals(
            "content://android.media.tv/passthrough/com.hisense.tv.hitvinput%2F.hdmi.HdmiTvInputService%2FHW4",
            TvInputs.passthroughUri("com.hisense.tv.hitvinput/.hdmi.HdmiTvInputService/HW4")
        )
    }
}
