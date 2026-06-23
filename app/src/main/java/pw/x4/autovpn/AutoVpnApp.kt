package pw.x4.autovpn

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import pw.x4.autovpn.data.installed.InstalledAppsProvider
import pw.x4.autovpn.data.local.AppDatabase
import pw.x4.autovpn.data.local.settingsDataStore
import pw.x4.autovpn.data.repository.SettingsRepositoryImpl
import pw.x4.autovpn.data.repository.TriggerRepositoryImpl
import pw.x4.autovpn.data.update.UpdateManager
import pw.x4.autovpn.data.vpn.BroadcastVpnTrigger
import pw.x4.autovpn.domain.repository.SettingsRepository
import pw.x4.autovpn.domain.repository.TriggerRepository
import pw.x4.autovpn.service.ForegroundAppDetector

/**
 * Лёгкий ручной DI вместо Hilt: меньше плагинов в сборке → меньше шансов уронить CI.
 * Все зависимости — ленивые синглтоны на процесс.
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
    val foregroundAppDetector by lazy {
        // getSystemService(Class) помечен @Nullable — на реальном устройстве сервис есть всегда.
        ForegroundAppDetector(appContext.getSystemService(UsageStatsManager::class.java)!!)
    }
    // Тихий коннект: broadcast-тумблер VPN (без UI).
    val broadcastVpnTrigger by lazy {
        BroadcastVpnTrigger(appContext)
    }
    val updateManager by lazy {
        UpdateManager(appContext)
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
