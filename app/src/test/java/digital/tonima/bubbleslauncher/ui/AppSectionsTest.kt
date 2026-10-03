package digital.tonima.bubbleslauncher.ui

import digital.tonima.bubbleslauncher.model.AppFolder
import digital.tonima.bubbleslauncher.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class AppSectionsTest {

    private fun app(pkg: String, minutes: Long = 0, name: String = pkg) =
        AppInfo(pkg, name, TimeUnit.MINUTES.toMillis(minutes))

    private fun compute(
        apps: List<AppInfo>,
        pinned: List<String> = emptyList(),
        hidden: Set<String> = emptySet(),
        essential: Set<String> = emptySet(),
        focus: Boolean = false,
        query: String = "",
        dockOrder: List<String> = emptyList(),
        folders: List<AppFolder> = emptyList()
    ) = AppSections.compute(apps, pinned, hidden, essential, focus, query, dockOrder, folders)

    @Test
    fun splitsIntoPinnedInUseAndOther() {
        val s = compute(
            listOf(app("a", 5), app("b", 0), app("c", 10), app("d", 0)),
            pinned = listOf("d")
        )
        assertEquals(listOf("d"), s.pinned.map { it.packageName })
        assertEquals(listOf("c", "a"), s.inUse.map { it.packageName })
        assertEquals(listOf("b"), s.other.map { it.packageName })
    }

    @Test
    fun pinnedFollowsUserOrderNotAppOrder() {
        val s = compute(listOf(app("a"), app("b"), app("c")), pinned = listOf("c", "a", "b"))
        assertEquals(listOf("c", "a", "b"), s.pinned.map { it.packageName })
    }

    @Test
    fun pinnedAppWithUsageIsNotDuplicatedInUse() {
        val s = compute(listOf(app("a", 30)), pinned = listOf("a"))
        assertEquals(1, s.pinned.size)
        assertTrue(s.inUse.isEmpty())
    }

    @Test
    fun hiddenAppsAreExcluded() {
        val s = compute(listOf(app("a"), app("b")), hidden = setOf("a"))
        assertEquals(listOf("b"), s.other.map { it.packageName })
    }

    @Test
    fun focusModeKeepsOnlyEssential() {
        val s = compute(listOf(app("a"), app("b"), app("c", 3)), essential = setOf("c"), focus = true)
        assertTrue(s.pinned.isEmpty() && s.other.isEmpty())
        assertEquals(listOf("c"), s.inUse.map { it.packageName })
    }

    @Test
    fun essentialIgnoredWhenFocusModeOff() {
        val s = compute(listOf(app("a"), app("b")), essential = setOf("a"))
        assertEquals(2, s.other.size)
    }

    @Test
    fun searchIsCaseInsensitiveAndTrimmed() {
        val s = compute(listOf(app("a", name = "Camera"), app("b", name = "Chrome")), query = "  CAM ")
        assertEquals(listOf("a"), s.other.map { it.packageName })
    }

    @Test
    fun inUseIsLimited() {
        val apps = (1..10).map { app("p$it", minutes = it.toLong()) }
        val s = compute(apps)
        assertEquals(AppSections.IN_USE_LIMIT, s.inUse.size)
        assertEquals("p10", s.inUse.first().packageName)
    }

    @Test
    fun maxTimeIsAtLeastOneAndIgnoresHidden() {
        assertEquals(1L, compute(emptyList()).maxTime)
        assertEquals(1L, compute(listOf(app("a"))).maxTime)
        val s = compute(listOf(app("a", 100), app("b", 10)), hidden = setOf("a"))
        assertEquals(TimeUnit.MINUTES.toMillis(10), s.maxTime)
    }

    @Test
    fun bubbleSize_scalesBetween48And120() {
        assertEquals(48f, bubbleSizeDp(0, 100), 0.001f)
        assertEquals(120f, bubbleSizeDp(100, 100), 0.001f)
        assertEquals(84f, bubbleSizeDp(50, 100), 0.001f)
        assertEquals(48f, bubbleSizeDp(10, 0), 0.001f)
    }

    @Test
    fun formatUsage_formats() {
        assertEquals("<1m", formatUsage(30_000))
        assertEquals("5m", formatUsage(TimeUnit.MINUTES.toMillis(5)))
        assertEquals("2h5m", formatUsage(TimeUnit.MINUTES.toMillis(125)))
    }

    @Test
    fun usedAppsBeyondTheLimit_stillAppearInAllApps() {
        // Regression: apps ranked below "Used today" used to vanish from the grid entirely.
        val apps = (1..10).map { app("p$it", minutes = it.toLong()) }
        val s = compute(apps)
        val shown = (s.pinned + s.inUse + s.other).map { it.packageName }
        assertEquals(apps.map { it.packageName }.sorted(), shown.sorted())
    }

    @Test
    fun allApps_isAlphabeticalIgnoringCase() {
        val s = compute(listOf(app("1", name = "zebra"), app("2", name = "Apple"), app("3", name = "mango")))
        assertEquals(listOf("Apple", "mango", "zebra"), s.other.map { it.appName })
    }

    @Test
    fun dock_followsOrder_andLeavesTheGrid() {
        val s = compute(listOf(app("a"), app("b"), app("c")), dockOrder = listOf("c", "a"))
        assertEquals(listOf("c", "a"), s.dock.map { it.packageName })
        assertEquals(listOf("b"), s.other.map { it.packageName })
    }

    @Test
    fun dock_isCappedAtMax() {
        val apps = (1..8).map { app("p$it") }
        val s = compute(apps, dockOrder = apps.map { it.packageName })
        assertEquals(AppSections.MAX_DOCK, s.dock.size)
        assertEquals(8 - AppSections.MAX_DOCK, s.other.size)
    }

    @Test
    fun dockedApp_isFoundBySearch_andHiddenDockAppIsDropped() {
        val apps = listOf(app("a", name = "Camera"), app("b", name = "Chrome"))
        val searching = compute(apps, dockOrder = listOf("a"), query = "cam")
        assertEquals(listOf("a"), searching.dock.map { it.packageName })
        assertEquals(listOf("a"), searching.other.map { it.packageName })
        val hidden = compute(apps, dockOrder = listOf("a"), hidden = setOf("a"))
        assertTrue(hidden.dock.isEmpty())
    }

    @Test
    fun firstMatch_prefersPinnedThenUsedThenAlphabetical() {
        val apps = listOf(app("z", name = "Zed"), app("u", minutes = 3, name = "Used"), app("p", name = "Pinned"))
        assertEquals("p", compute(apps, pinned = listOf("p")).firstMatch()?.packageName)
        assertEquals("u", compute(apps).firstMatch()?.packageName)
        assertEquals("z", compute(listOf(app("z", name = "Zed"))).firstMatch()?.packageName)
        assertEquals(null, compute(emptyList()).firstMatch())
    }

    @Test
    fun folderApps_leaveTheGrid_andAppearInFolderTile() {
        val apps = listOf(app("a"), app("b"), app("c"))
        val s = compute(apps, folders = listOf(AppFolder("f", "Work", listOf("b", "a"))))
        assertEquals(listOf("c"), s.other.map { it.packageName })
        assertEquals(1, s.folders.size)
        assertEquals(listOf("b", "a"), s.folders.single().apps.map { it.packageName })
    }

    @Test
    fun searching_showsFolderAppsIndividually_andHidesTiles() {
        val apps = listOf(app("a", name = "Alpha"), app("b", name = "Beta"))
        val s = compute(apps, query = "alp", folders = listOf(AppFolder("f", "Work", listOf("a"))))
        assertTrue(s.folders.isEmpty())
        assertEquals(listOf("a"), s.other.map { it.packageName })
    }

    @Test
    fun folder_skipsHiddenAndUninstalled_andEmptyFolderDisappears() {
        val apps = listOf(app("a"), app("b"))
        val s = compute(
            apps,
            hidden = setOf("a"),
            folders = listOf(
                AppFolder("f1", "Mixed", listOf("a", "b", "gone")),
                AppFolder("f2", "AllGone", listOf("gone", "a"))
            )
        )
        assertEquals(listOf("f1"), s.folders.map { it.folder.id })
        assertEquals(listOf("b"), s.folders.single().apps.map { it.packageName })
    }

    @Test
    fun focusMode_limitsFolderMembersToEssential() {
        val apps = listOf(app("a"), app("b"))
        val s = compute(
            apps, essential = setOf("b"), focus = true,
            folders = listOf(AppFolder("f", "F", listOf("a", "b")))
        )
        assertEquals(listOf("b"), s.folders.single().apps.map { it.packageName })
    }
}
