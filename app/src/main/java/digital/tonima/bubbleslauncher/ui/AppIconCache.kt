package digital.tonima.bubbleslauncher.ui

import android.content.Context
import android.content.pm.LauncherApps
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import digital.tonima.bubbleslauncher.model.AppInfo

/**
 * In-memory icon cache so icons scrolling back into view (or recomposed after a state change)
 * are not decoded again. Sized in bytes (ARGB_8888 => 4 bytes/pixel), capped to ~4 MB.
 */
object AppIconCache {
    private const val ICON_PX = 128
    private const val MAX_BYTES = 4 * 1024 * 1024

    private val cache = object : LruCache<String, ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    fun key(app: AppInfo): String = "${app.packageName}#${app.userHandle?.hashCode() ?: 0}"

    fun peek(app: AppInfo): ImageBitmap? = cache.get(key(app))

    /** Loads (blocking; call off the main thread) and caches the icon. */
    fun load(context: Context, app: AppInfo): ImageBitmap? {
        peek(app)?.let { return it }
        return try {
            val pm = context.packageManager
            val drawable = if (app.userHandle != null) {
                context.getSystemService(LauncherApps::class.java)
                    ?.getActivityList(app.packageName, app.userHandle)
                    ?.firstOrNull()?.getBadgedIcon(0)
                    ?: pm.getApplicationIcon(app.packageName)
            } else {
                pm.getApplicationIcon(app.packageName)
            }
            drawable.toBitmap(ICON_PX, ICON_PX).asImageBitmap().also { cache.put(key(app), it) }
        } catch (_: Throwable) {
            null
        }
    }

    fun evict(packageName: String) {
        cache.snapshot().keys.filter { it.startsWith("$packageName#") }.forEach { cache.remove(it) }
    }

    fun clear() = cache.evictAll()
}

/** Cached app icon for composables: instant on a cache hit, otherwise loaded off the main thread. */
@androidx.compose.runtime.Composable
fun rememberAppIcon(app: AppInfo): ImageBitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val icon by androidx.compose.runtime.produceState<ImageBitmap?>(
        initialValue = AppIconCache.peek(app),
        app.packageName,
        app.userHandle
    ) {
        if (value == null) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { AppIconCache.load(context, app) }
        }
    }
    return icon
}
