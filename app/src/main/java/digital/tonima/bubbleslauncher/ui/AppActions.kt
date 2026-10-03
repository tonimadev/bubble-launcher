package digital.tonima.bubbleslauncher.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import digital.tonima.bubbleslauncher.R
import digital.tonima.bubbleslauncher.model.AppInfo

/** Single home for the intents that used to be copy-pasted across the UI. */
object AppActions {

    fun launch(context: Context, app: AppInfo) {
        try {
            if (app.componentName != null && app.userHandle != null) {
                context.getSystemService(LauncherApps::class.java)
                    ?.startMainActivity(app.componentName, app.userHandle, null, null)
            } else {
                context.packageManager.getLaunchIntentForPackage(app.packageName)
                    ?.let(context::startActivity)
            }
        } catch (_: Throwable) {
            openAppInfo(context, app)
        }
    }

    /** App shortcuts (the "New tab" style actions). Empty unless we are the default launcher. */
    fun shortcuts(context: Context, app: AppInfo, max: Int = 4): List<AppShortcut> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        return try {
            val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return emptyList()
            if (!launcherApps.hasShortcutHostPermission()) return emptyList()
            val query = LauncherApps.ShortcutQuery()
                .setPackage(app.packageName)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                )
            launcherApps.getShortcuts(query, app.userHandle ?: Process.myUserHandle())
                ?.filter { it.isEnabled && it.shortLabel != null }
                ?.sortedBy { it.rank }
                ?.take(max)
                ?.map { AppShortcut(it, it.shortLabel.toString()) }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun startShortcut(context: Context, shortcut: AppShortcut) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        try {
            context.getSystemService(LauncherApps::class.java)?.startShortcut(shortcut.info, null, null)
        } catch (_: Exception) { /* shortcut no longer valid */ }
    }

    fun openAppInfo(context: Context, app: AppInfo) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${app.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, "market://details?id=${app.packageName}".toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) { /* nothing can handle it */ }
        }
    }

    fun uninstall(context: Context, app: AppInfo) {
        val isSystem = try {
            context.packageManager.getApplicationInfo(app.packageName, 0).flags and ApplicationInfo.FLAG_SYSTEM != 0
        } catch (_: Exception) {
            false
        }
        if (isSystem) {
            Toast.makeText(context, R.string.confirm_uninstall_system_app, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            context.startActivity(
                Intent(Intent.ACTION_DELETE, "package:${app.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) { /* ignore */ }
    }
}

/** A launcher shortcut plus its already-resolved label (keeps API-25-only calls inside guarded code). */
class AppShortcut(val info: ShortcutInfo, val label: String)
