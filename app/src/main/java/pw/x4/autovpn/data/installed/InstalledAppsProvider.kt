package pw.x4.autovpn.data.installed

import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pw.x4.autovpn.domain.model.AppInfo

/** Поставщик списка запускаемых приложений (есть иконка в лаунчере). */
class InstalledAppsProvider(private val packageManager: PackageManager) {

    suspend fun loadLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        // Берём только то, что реально запускается из лаунчера — системный мусор отсекается.
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo }
            .distinctBy { it.packageName }
            .map { info ->
                AppInfo(
                    packageName = info.packageName,
                    label = info.loadLabel(packageManager).toString(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
