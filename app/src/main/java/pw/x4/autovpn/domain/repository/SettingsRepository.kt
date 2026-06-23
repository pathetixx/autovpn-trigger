package pw.x4.autovpn.domain.repository

import kotlinx.coroutines.flow.Flow
import pw.x4.autovpn.domain.model.AutomationSettings

interface SettingsRepository {
    val settings: Flow<AutomationSettings>
    suspend fun setAutomationEnabled(enabled: Boolean)
    suspend fun setVpnPackage(packageName: String?)
}
