package digital.tonima.bubbleslauncher.model

import android.graphics.drawable.Drawable
import android.content.ComponentName
import android.os.UserHandle

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val totalTimeInForeground: Long,
    val componentName: ComponentName? = null,
    val userHandle: UserHandle? = null
)