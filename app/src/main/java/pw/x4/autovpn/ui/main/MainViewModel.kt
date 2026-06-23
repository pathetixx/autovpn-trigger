package pw.x4.autovpn.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pw.x4.autovpn.AutoVpnApp
import pw.x4.autovpn.data.update.UpdateInfo
import pw.x4.autovpn.domain.model.AppInfo
import pw.x4.autovpn.service.AppMonitorService
import pw.x4.autovpn.util.PermissionUtils

/** Состояние процесса проверки/установки обновления. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data object Installing : UpdateState
    data class Error(val message: String) : UpdateState
}

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

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    init {
        loadInstalledApps()
        refreshPermissions()
        maybeAutoCheckUpdate()
    }

    private fun loadInstalledApps() = viewModelScope.launch {
        installedApps.value = container.installedAppsProvider.loadLaunchableApps()
    }

    fun refreshPermissions() {
        permissionState.value = PermissionState(
            usageAccess = PermissionUtils.hasUsageAccess(getApplication()),
            notifications = PermissionUtils.hasNotificationPermission(getApplication()),
            batteryUnrestricted = PermissionUtils.isIgnoringBatteryOptimizations(getApplication()),
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

    fun setAutomationEnabled(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setAutomationEnabled(enabled)
        if (enabled) AppMonitorService.start(getApplication()) else AppMonitorService.stop(getApplication())
    }

    fun setAutoUpdateEnabled(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setAutoUpdateEnabled(enabled)
    }

    fun setRespectManualVpn(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setRespectManualVpn(enabled)
    }

    // ── Обновления ──────────────────────────────────────────────────────────

    private fun maybeAutoCheckUpdate() = viewModelScope.launch {
        if (container.settingsRepository.settings.first().autoUpdateEnabled) checkForUpdate()
    }

    fun checkForUpdate() = viewModelScope.launch {
        _updateState.value = UpdateState.Checking
        _updateState.value = runCatching { container.updateManager.checkForUpdate() }
            .fold(
                onSuccess = { info -> if (info == null) UpdateState.UpToDate else UpdateState.Available(info) },
                onFailure = { UpdateState.Error(it.message ?: "ошибка сети") },
            )
    }

    fun installUpdate(info: UpdateInfo) = viewModelScope.launch {
        _updateState.value = UpdateState.Installing
        val ok = container.updateManager.downloadAndInstall(info)
        if (!ok) _updateState.value = UpdateState.Error("не удалось скачать обновление")
    }
}
