package pw.x4.autovpn.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pw.x4.autovpn.AutoVpnApp
import pw.x4.autovpn.domain.model.AppInfo
import pw.x4.autovpn.service.AppMonitorService
import pw.x4.autovpn.util.PermissionUtils

/**
 * AndroidViewModel — нужен Application и для доступа к AppContainer, и для старта/останова
 * сервиса. Бизнес-логики тут минимум: ViewModel только склеивает потоки данных и проксирует
 * действия в репозитории.
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as AutoVpnApp).container

    private val searchQuery = MutableStateFlow("")
    private val installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val permissionState = MutableStateFlow(PermissionState())

    val uiState: StateFlow<MainUiState> = combine(
        installedApps,
        searchQuery,
        container.triggerRepository.triggerPackages,
        container.settingsRepository.settings,
        permissionState,
    ) { apps, query, triggers, settings, permissions ->
        MainUiState(
            apps = apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            },
            triggerPackages = triggers,
            searchQuery = query,
            settings = settings,
            vpnAppLabel = apps.firstOrNull { it.packageName == settings.vpnPackage }?.label,
            permissions = permissions,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), MainUiState())

    init {
        loadInstalledApps()
        refreshPermissions()
    }

    private fun loadInstalledApps() = viewModelScope.launch {
        installedApps.value = container.installedAppsProvider.loadLaunchableApps()
    }

    /** Дёргаем при возврате на экран — статус разрешений мог измениться в Настройках. */
    fun refreshPermissions() {
        permissionState.value = PermissionState(
            usageAccess = PermissionUtils.hasUsageAccess(getApplication()),
            notifications = PermissionUtils.hasNotificationPermission(getApplication()),
            batteryUnrestricted = PermissionUtils.isIgnoringBatteryOptimizations(getApplication()),
            canDrawOverlays = PermissionUtils.canDrawOverlays(getApplication()),
        )
    }

    fun onSearchChange(query: String) {
        searchQuery.value = query
    }

    fun toggleTrigger(packageName: String, enabled: Boolean) = viewModelScope.launch {
        container.triggerRepository.setTrigger(packageName, enabled)
    }

    fun selectVpn(packageName: String) = viewModelScope.launch {
        container.settingsRepository.setVpnPackage(packageName)
    }

    /** Главный тумблер: пишем флаг и одновременно поднимаем/гасим сервис мониторинга. */
    fun setAutomationEnabled(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setAutomationEnabled(enabled)
        if (enabled) AppMonitorService.start(getApplication())
        else AppMonitorService.stop(getApplication())
    }
}
