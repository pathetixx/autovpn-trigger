package pw.x4.autovpn.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pw.x4.autovpn.data.local.SettingsKeys
import pw.x4.autovpn.domain.model.AutomationSettings
import pw.x4.autovpn.domain.repository.SettingsRepository
import pw.x4.autovpn.domain.vpn.LaunchMode

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AutomationSettings> = dataStore.data.map { prefs ->
        AutomationSettings(
            automationEnabled = prefs[SettingsKeys.AUTOMATION_ENABLED] ?: false,
            vpnPackage = prefs[SettingsKeys.VPN_PACKAGE],
            connectMode = prefs[SettingsKeys.CONNECT_MODE]
                ?.let { runCatching { LaunchMode.valueOf(it) }.getOrNull() }
                ?: LaunchMode.OPEN_APP,
            connectComponent = prefs[SettingsKeys.CONNECT_COMPONENT],
        )
    }

    override suspend fun setAutomationEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.AUTOMATION_ENABLED] = enabled }
    }

    override suspend fun setVpnPackage(packageName: String?) {
        dataStore.edit { prefs ->
            if (packageName.isNullOrBlank()) prefs.remove(SettingsKeys.VPN_PACKAGE)
            else prefs[SettingsKeys.VPN_PACKAGE] = packageName
        }
    }

    override suspend fun setConnectTarget(mode: LaunchMode, component: String?) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.CONNECT_MODE] = mode.name
            if (component.isNullOrBlank()) prefs.remove(SettingsKeys.CONNECT_COMPONENT)
            else prefs[SettingsKeys.CONNECT_COMPONENT] = component
        }
    }
}
