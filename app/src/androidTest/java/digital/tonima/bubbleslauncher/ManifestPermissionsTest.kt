package digital.tonima.bubbleslauncher

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManifestPermissionsTest {
    /**
     * Regression: without REQUEST_DELETE_PACKAGES the system uninstaller closes immediately for
     * ACTION_DELETE, so "Uninstall" in the menu appeared to do nothing.
     */
    @Test
    fun declaresRequestDeletePackages_soUninstallWorks() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions?.toList().orEmpty()
        assertTrue(requested.toString(), "android.permission.REQUEST_DELETE_PACKAGES" in requested)
    }
}
