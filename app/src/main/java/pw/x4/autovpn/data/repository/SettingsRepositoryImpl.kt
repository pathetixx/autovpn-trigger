package pw.x4.autovpn.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pw.x4.autovpn.data.local.SettingsKeys
import pw.x4.autovpn.domain.model.AutomationSettings
import pw.x4.autovpn.domain.repository.SettingsRepository

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AutomationSettings> = dataStore.data.map { prefs ->
        AutomationSettings(
            automationEnabled = prefs[SettingsKeys.AUTOMATION_ENABLED] ?: false,
            vpnPackage = prefs[SettingsKeys.VPN_PACKAGE],
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
}
