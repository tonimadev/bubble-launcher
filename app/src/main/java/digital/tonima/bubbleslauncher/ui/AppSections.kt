package digital.tonima.bubbleslauncher.ui

import digital.tonima.bubbleslauncher.model.AppFolder
import digital.tonima.bubbleslauncher.model.AppInfo

/** Grid sections computed once per input change instead of on every recomposition. */
data class AppSections(
    /** Apps pinned to the always-visible dock, in dock order. Not repeated in the grid. */
    val dock: List<AppInfo>,
    /** Folder tiles (only while not searching). Their apps do not appear elsewhere in the grid. */
    val folders: List<FolderSection>,
    val pinned: List<AppInfo>,
    val inUse: List<AppInfo>,
    /** Alphabetical. */
    val other: List<AppInfo>,
    /** Largest foreground time among visible apps (>= 1 to avoid division by zero). */
    val maxTime: Long
) {
    /** What pressing "search" on the keyboard should launch: the first thing the grid shows. */
    fun firstMatch(): AppInfo? = pinned.firstOrNull() ?: inUse.firstOrNull() ?: other.firstOrNull()

    companion object {
        const val IN_USE_LIMIT = 6
        const val MAX_DOCK = 5

        val Empty = AppSections(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), 1L)

        /**
         * @param pinnedOrder pinned package names in user order; the pinned section follows it.
         * @param dockOrder docked package names in user order (first [MAX_DOCK] are used). Docked
         *   apps leave the grid, except while searching so they stay findable.
         */
        fun compute(
            apps: List<AppInfo>,
            pinnedOrder: List<String>,
            hidden: Set<String>,
            essential: Set<String>,
            focusMode: Boolean,
            query: String,
            dockOrder: List<String> = emptyList(),
            folders: List<AppFolder> = emptyList()
        ): AppSections {
            val q = query.trim().lowercase()
            val pinnedIndex = indexOf(pinnedOrder)
            val dockIndex = indexOf(dockOrder.take(MAX_DOCK))

            val dock = apps.filter { it.packageName in dockIndex && it.packageName !in hidden }
                .sortedBy { dockIndex.getValue(it.packageName) }

            val foldered = if (q.isEmpty()) folders.flatMapTo(HashSet()) { it.packages } else emptySet()

            val visible = apps.filter { app ->
                app.packageName !in hidden &&
                    (q.isNotEmpty() || app.packageName !in dockIndex) &&
                    app.packageName !in foldered &&
                    (!focusMode || app.packageName in essential) &&
                    (q.isEmpty() || app.appName.lowercase().contains(q))
            }

            val pinned = visible.filter { it.packageName in pinnedIndex }
                .sortedBy { pinnedIndex.getValue(it.packageName) }
            val rest = visible.filter { it.packageName !in pinnedIndex }
            val inUse = rest.filter { it.totalTimeInForeground > 0 }
                .sortedByDescending { it.totalTimeInForeground }
                .take(IN_USE_LIMIT)
            val inUsePackages = inUse.mapTo(HashSet()) { it.packageName }
            // Apps beyond the "used today" limit still belong in "All apps", otherwise they vanish.
            val other = rest.filter { it.packageName !in inUsePackages }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })

            val byPackage = apps.associateBy { it.packageName }
            val folderSections = if (q.isNotEmpty()) emptyList() else folders.mapNotNull { folder ->
                val members = folder.packages.mapNotNull { byPackage[it] }
                    .filter { it.packageName !in hidden && (!focusMode || it.packageName in essential) }
                if (members.isEmpty()) null else FolderSection(folder, members)
            }

            return AppSections(
                dock = dock,
                folders = folderSections,
                pinned = pinned,
                inUse = inUse,
                other = other,
                maxTime = visible.maxOfOrNull { it.totalTimeInForeground }?.coerceAtLeast(1L) ?: 1L
            )
        }

        private fun indexOf(order: List<String>): Map<String, Int> {
            val m = HashMap<String, Int>(order.size * 2)
            order.forEachIndexed { i, p -> m.putIfAbsent(p, i) }
            return m
        }
    }
}

/** A folder with its installed, visible apps resolved. */
data class FolderSection(val folder: AppFolder, val apps: List<AppInfo>)
