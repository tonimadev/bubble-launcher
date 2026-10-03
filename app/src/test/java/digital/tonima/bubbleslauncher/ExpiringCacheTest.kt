package digital.tonima.bubbleslauncher

import digital.tonima.bubbleslauncher.data.ExpiringCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpiringCacheTest {
    private var now = 0L
    private fun cache(ttl: Long = 1000) = ExpiringCache<String>(ttl) { now }

    @Test fun empty_returnsNull() = assertNull(cache().get())

    @Test fun returnsValueWithinTtl() {
        val c = cache()
        c.put("x")
        now = 999
        assertEquals("x", c.get())
    }

    @Test fun expiresAtTtl() {
        val c = cache()
        c.put("x")
        now = 1000
        assertNull(c.get())
    }

    @Test fun invalidate_clears() {
        val c = cache()
        c.put("x")
        c.invalidate()
        assertNull(c.get())
    }

    @Test fun put_restartsTtl() {
        val c = cache()
        c.put("a")
        now = 900
        c.put("b")
        now = 1800
        assertEquals("b", c.get())
    }
}
