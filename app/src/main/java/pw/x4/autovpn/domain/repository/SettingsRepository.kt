package pw.x4.autovpn.domain.repository

import kotlinx.coroutines.flow.Flow
import pw.x4.autovpn.domain.model.AutomationSettings
import pw.x4.autovpn.domain.vpn.LaunchMode

interface SettingsRepository {
    val settings: Flow<AutomationSettings>
    suspend fun setAutomationEnabled(enabled: Boolean)
    suspend fun setVpnPackage(packageName: String?)

    /** Сохранить способ коннекта (режим + компонент), найденный в диагностике. */
    suspend fun setConnectTarget(mode: LaunchMode, component: String?)
}
