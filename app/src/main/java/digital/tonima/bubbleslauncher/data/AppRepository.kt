package digital.tonima.bubbleslauncher.data

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.UserManager
import android.os.UserHandle
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.bubbleslauncher.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
    private val usageStatsManager: UsageStatsManager
) {

    suspend fun getApps(): List<AppInfo> = getAppsForProfile(Profile.PERSONAL)

    suspend fun getAppsForProfile(profile: Profile): List<AppInfo> = withContext(Dispatchers.IO) {
        val usageStats = try {
            getUsageStatsLast7Days()
        } catch (_: Exception) {
            emptyMap<String, Long>()
        }

        when (profile) {
            Profile.PERSONAL -> {
                val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                val apps = packageManager.queryIntentActivities(intent, 0)
                apps.map { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName
                    val appName = resolveInfo.loadLabel(packageManager).toString()
                    val icon = resolveInfo.loadIcon(packageManager)
                    val timeInForeground = usageStats[packageName] ?: 0L
                    AppInfo(packageName, appName, icon, timeInForeground)
                }
            }
            Profile.WORK -> {
                try {
                    val launcherApps = context.getSystemService(LauncherApps::class.java)
                    val userManager = context.getSystemService(UserManager::class.java)
                    val profiles: List<UserHandle> = userManager?.userProfiles ?: emptyList()
                    val personal = profiles.firstOrNull()
                    val workProfiles = profiles.filter { it != personal }
                    val result = mutableListOf<AppInfo>()
                    workProfiles.forEach { userHandle ->
                        val activities = launcherApps?.getActivityList(null, userHandle) ?: emptyList()
                        activities.forEach { lai ->
                            val packageName = lai.applicationInfo.packageName
                            val appName = lai.label?.toString() ?: packageName
                            val icon = lai.applicationInfo.loadIcon(packageManager)
                            val timeInForeground = usageStats[packageName] ?: 0L
                            result.add(AppInfo(packageName, appName, icon, timeInForeground))
                        }
                    }
                    result
                } catch (e: Exception) {
                    Log.w("AppRepository", "Failed to load work profile apps", e)
                    emptyList()
                }
            }
        }
    }

    private fun getUsageStatsLast7Days(): Map<String, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis

        return try {
            val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
            stats.mapValues { it.value.totalTimeInForeground }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}