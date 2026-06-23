package pw.x4.autovpn

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import pw.x4.autovpn.data.installed.AppComponentScanner
import pw.x4.autovpn.data.installed.InstalledAppsProvider
import pw.x4.autovpn.data.local.AppDatabase
import pw.x4.autovpn.data.local.settingsDataStore
import pw.x4.autovpn.data.repository.SettingsRepositoryImpl
import pw.x4.autovpn.data.repository.TriggerRepositoryImpl
import pw.x4.autovpn.data.vpn.IntentVpnTrigger
import pw.x4.autovpn.domain.repository.SettingsRepository
import pw.x4.autovpn.domain.repository.TriggerRepository
import pw.x4.autovpn.domain.vpn.VpnTrigger
import pw.x4.autovpn.service.ForegroundAppDetector

/**
 * Лёгкий ручной DI вместо Hilt: меньше плагинов в сборке → меньше шансов уронить CI,
 * проще держать приложение «лёгким». Все зависимости — ленивые синглтоны на процесс.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database by lazy { AppDatabase.get(appContext) }

    val triggerRepository: TriggerRepository by lazy {
        TriggerRepositoryImpl(database.triggerDao())
    }
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(appContext.settingsDataStore)
    }
    val installedAppsProvider by lazy {
        InstalledAppsProvider(appContext.packageManager)
    }
    val appComponentScanner by lazy {
        AppComponentScanner(appContext.packageManager)
    }
    val foregroundAppDetector by lazy {
        // getSystemService(Class) помечен @Nullable — на реальном устройстве сервис есть всегда.
        ForegroundAppDetector(appContext.getSystemService(UsageStatsManager::class.java)!!)
    }
    val vpnTrigger: VpnTrigger by lazy {
        IntentVpnTrigger(appContext)
    }
}

class AutoVpnApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
