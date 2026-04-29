package digital.tonima.bubbleslauncher.model

import android.content.ComponentName
import android.os.UserHandle

data class AppInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long,
    val componentName: ComponentName? = null,
    val userHandle: UserHandle? = null
)