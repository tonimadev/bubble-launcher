package digital.tonima.bubbleslauncher.data

/**
 * Single-value cache with a time-to-live. [clock] is injectable so tests don't sleep.
 * Not thread-safe by itself; callers synchronise (see AppRepository).
 */
class ExpiringCache<T>(
    private val ttlMs: Long,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private var value: T? = null
    private var storedAt = 0L

    fun get(): T? {
        val v = value ?: return null
        return if (clock() - storedAt < ttlMs) v else null
    }

    fun put(v: T) {
        value = v
        storedAt = clock()
    }

    fun invalidate() {
        value = null
    }
}
