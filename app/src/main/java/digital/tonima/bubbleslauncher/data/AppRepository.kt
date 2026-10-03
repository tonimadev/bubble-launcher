package digital.tonima.bubbleslauncher.data

import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process.myUserHandle
import android.os.UserManager
import android.os.UserHandle
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.bubbleslauncher.model.AppInfo
import digital.tonima.bubbleslauncher.ui.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
    private val usageStatsManager: UsageStatsManager
) {
    companion object {
        private const val TAG = "AppRepository"
        private const val USAGE_TTL_MS = 30_000L
    }

    // Launchable apps (the expensive part: queryIntentActivities + loadLabel for every app)
    // are cached until the system reports a package change.
    private val launchableCache = ConcurrentHashMap<Profile, List<AppInfo>>()
    private val usageCache = ExpiringCache<Map<String, Long>>(USAGE_TTL_MS)

    private val _packageChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Emits whenever an app is installed, removed, updated or (un)available in any profile. */
    val packageChanges: SharedFlow<Unit> = _packageChanges.asSharedFlow()

    private val launcherCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            changed(*packageNames)
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            changed(*packageNames)
    }

    init {
        try {
            context.getSystemService(LauncherApps::class.java)?.registerCallback(launcherCallback)
        } catch (t: Throwable) {
            Log.w(TAG, "Could not register LauncherApps callback", t)
        }
    }

    private fun changed(vararg packages: String) {
        packages.forEach { AppIconCache.evict(it) }
        invalidate()
        _packageChanges.tryEmit(Unit)
    }

    /** Drops cached app lists and usage so the next load hits the system again. */
    fun invalidate() {
        launchableCache.clear()
        usageCache.invalidate()
    }

    suspend fun getApps(): List<AppInfo> = getAppsForProfile(Profile.PERSONAL)

    suspend fun getAppsForProfile(profile: Profile): List<AppInfo> = withContext(Dispatchers.IO) {
        val launchable = launchableCache[profile] ?: loadLaunchable(profile).also { launchableCache[profile] = it }
        val usage = cachedUsage()
        launchable.map { it.copy(totalTimeInForeground = usage[it.packageName] ?: 0L) }
    }

    private fun loadLaunchable(profile: Profile): List<AppInfo> = when (profile) {
        Profile.PERSONAL -> {
            val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            packageManager.queryIntentActivities(intent, 0)
                .map { ri ->
                    AppInfo(
                        packageName = ri.activityInfo.packageName,
                        appName = ri.loadLabel(packageManager).toString(),
                        totalTimeInForeground = 0L,
                        componentName = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name),
                        userHandle = null
                    )
                }
                // An app can register several CATEGORY_LAUNCHER activities (e.g. Motorola apps);
                // keep one per package to avoid duplicate keys in the UI.
                .distinctBy { it.packageName }
        }
        Profile.WORK -> try {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
            val userManager = context.getSystemService(UserManager::class.java)
            val current = myUserHandle()
            val workProfiles = (userManager?.userProfiles ?: emptyList()).filter { it != current }
            workProfiles.flatMap { userHandle ->
                (launcherApps?.getActivityList(null, userHandle) ?: emptyList()).map { lai ->
                    AppInfo(
                        packageName = lai.applicationInfo.packageName,
                        appName = lai.label?.toString() ?: lai.applicationInfo.packageName,
                        totalTimeInForeground = 0L,
                        componentName = lai.componentName,
                        userHandle = userHandle
                    )
                }
            }.distinctBy { it.packageName to it.userHandle }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load work profile apps", e)
            emptyList()
        }
    }

    @Synchronized
    private fun cachedUsage(): Map<String, Long> =
        usageCache.get() ?: getUsageStatsLast7Days().also { usageCache.put(it) }

    private fun getUsageStatsLast7Days(): Map<String, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis
        return try {
            usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
                .mapValues { it.value.totalTimeInForeground }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun hasWorkProfile(): Boolean = withContext(Dispatchers.IO) {
        try {
            val userManager = context.getSystemService(UserManager::class.java)
            val current = myUserHandle()
            (userManager?.userProfiles ?: emptyList()).any { it != current }
        } catch (_: Throwable) {
            false
        }
    }
}
