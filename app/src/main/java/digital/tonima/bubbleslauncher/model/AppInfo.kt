package digital.tonima.bubbleslauncher.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val totalTimeInForeground: Long
)