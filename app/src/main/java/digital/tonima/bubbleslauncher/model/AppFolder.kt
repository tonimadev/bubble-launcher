package digital.tonima.bubbleslauncher.model

/** A user-made group of apps. An app belongs to at most one folder. */
data class AppFolder(
    val id: String,
    val name: String,
    val packages: List<String>
)

/** Pure folder operations (no Android types), kept separate so they are unit-testable. */
object FolderOps {
    const val MAX_NAME_LENGTH = 24

    /** Strips the codec separators and control chars, trims and caps the length. */
    fun sanitizeName(raw: String): String =
        raw.filter { it != FolderCodec.FIELD_SEP && it != FolderCodec.RECORD_SEP && !it.isISOControl() }
            .trim()
            .take(MAX_NAME_LENGTH)

    fun create(folders: List<AppFolder>, name: String, pkg: String, id: String): List<AppFolder> {
        val clean = sanitizeName(name)
        if (clean.isEmpty()) return folders
        return removeApp(folders, pkg, dropEmpty = false) + AppFolder(id, clean, listOf(pkg))
    }

    /** Moves [pkg] into [folderId], removing it from any other folder first. */
    fun addApp(folders: List<AppFolder>, folderId: String, pkg: String): List<AppFolder> {
        val target = folders.firstOrNull { it.id == folderId } ?: return folders
        if (pkg in target.packages) return folders
        return removeApp(folders, pkg, dropEmpty = false).map {
            if (it.id == folderId) it.copy(packages = it.packages + pkg) else it
        }
    }

    /** Takes [pkg] out of its folder. Folders left empty disappear by default. */
    fun removeApp(folders: List<AppFolder>, pkg: String, dropEmpty: Boolean = true): List<AppFolder> =
        folders.map { it.copy(packages = it.packages - pkg) }.filter { !dropEmpty || it.packages.isNotEmpty() }

    fun rename(folders: List<AppFolder>, folderId: String, name: String): List<AppFolder> {
        val clean = sanitizeName(name)
        if (clean.isEmpty()) return folders
        return folders.map { if (it.id == folderId) it.copy(name = clean) else it }
    }

    fun delete(folders: List<AppFolder>, folderId: String): List<AppFolder> =
        folders.filter { it.id != folderId }

    fun folderOf(folders: List<AppFolder>, pkg: String): AppFolder? =
        folders.firstOrNull { pkg in it.packages }
}

/** Compact, dependency-free text encoding for DataStore: records "id US name US pkg,pkg". */
object FolderCodec {
    const val RECORD_SEP = '\u001E'
    const val FIELD_SEP = '\u001F'

    fun encode(folders: List<AppFolder>): String =
        folders.joinToString(RECORD_SEP.toString()) {
            listOf(it.id, it.name, it.packages.joinToString(",")).joinToString(FIELD_SEP.toString())
        }

    fun decode(text: String?): List<AppFolder> {
        if (text.isNullOrEmpty()) return emptyList()
        return text.split(RECORD_SEP).mapNotNull { record ->
            val f = record.split(FIELD_SEP)
            if (f.size < 3 || f[0].isEmpty() || f[1].isEmpty()) return@mapNotNull null
            AppFolder(f[0], f[1], f[2].split(",").filter { it.isNotEmpty() })
        }
    }
}
