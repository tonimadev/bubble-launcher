package digital.tonima.bubbleslauncher

import digital.tonima.bubbleslauncher.model.AppFolder
import digital.tonima.bubbleslauncher.model.FolderCodec
import digital.tonima.bubbleslauncher.model.FolderOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderTest {
    private val work = AppFolder("f1", "Work", listOf("a", "b"))
    private val fun_ = AppFolder("f2", "Fun", listOf("c"))

    @Test fun codec_roundTrips() {
        val list = listOf(work, fun_, AppFolder("f3", "Ünïcode, with comma", listOf("x.y.z")))
        assertEquals(list, FolderCodec.decode(FolderCodec.encode(list)))
    }

    @Test fun codec_emptyAndNull() {
        assertEquals(emptyList<AppFolder>(), FolderCodec.decode(null))
        assertEquals(emptyList<AppFolder>(), FolderCodec.decode(""))
        assertEquals("", FolderCodec.encode(emptyList()))
    }

    @Test fun codec_skipsCorruptRecords() {
        val good = FolderCodec.encode(listOf(work))
        val decoded = FolderCodec.decode("garbage" + FolderCodec.RECORD_SEP + good)
        assertEquals(listOf(work), decoded)
    }

    @Test fun create_addsFolderWithApp_andRemovesFromOldFolder() {
        val r = FolderOps.create(listOf(work), "New", "a", "f9")
        assertEquals(listOf("b"), r.first { it.id == "f1" }.packages)
        assertEquals(AppFolder("f9", "New", listOf("a")), r.last())
    }

    @Test fun create_rejectsBlankName() =
        assertEquals(listOf(work), FolderOps.create(listOf(work), "   ", "z", "f9"))

    @Test fun sanitize_stripsSeparatorsTrimsAndCaps() {
        val dirty = " a" + FolderCodec.FIELD_SEP + "b" + FolderCodec.RECORD_SEP + " "
        assertEquals("ab", FolderOps.sanitizeName(dirty))
        assertEquals(FolderOps.MAX_NAME_LENGTH, FolderOps.sanitizeName("x".repeat(100)).length)
    }

    @Test fun addApp_movesBetweenFolders() {
        val r = FolderOps.addApp(listOf(work, fun_), "f2", "a")
        assertEquals(listOf("b"), r.first { it.id == "f1" }.packages)
        assertEquals(listOf("c", "a"), r.first { it.id == "f2" }.packages)
    }

    @Test fun addApp_unknownFolder_noChange() =
        assertEquals(listOf(work), FolderOps.addApp(listOf(work), "nope", "z"))

    @Test fun addApp_isIdempotent() = assertEquals(listOf(work), FolderOps.addApp(listOf(work), "f1", "a"))

    @Test fun removeApp_dropsEmptyFolder() {
        assertEquals(listOf(work), FolderOps.removeApp(listOf(work, fun_), "c"))
        assertEquals(
            listOf(work, fun_.copy(packages = emptyList())),
            FolderOps.removeApp(listOf(work, fun_), "c", dropEmpty = false)
        )
    }

    @Test fun rename_andDelete() {
        assertEquals("Job", FolderOps.rename(listOf(work), "f1", " Job ").first().name)
        assertEquals(listOf(work), FolderOps.rename(listOf(work), "f1", "  "))
        assertEquals(listOf(fun_), FolderOps.delete(listOf(work, fun_), "f1"))
    }

    @Test fun folderOf() {
        assertEquals("f2", FolderOps.folderOf(listOf(work, fun_), "c")?.id)
        assertNull(FolderOps.folderOf(listOf(work), "zzz"))
    }
}
