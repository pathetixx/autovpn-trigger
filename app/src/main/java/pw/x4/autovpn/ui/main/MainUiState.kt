package pw.x4.autovpn.ui.main

import pw.x4.autovpn.domain.model.AppInfo
import pw.x4.autovpn.domain.model.AutomationSettings

/** Состояние выданных «критических» разрешений. */
data class PermissionState(
    val usageAccess: Boolean = false,
    val notifications: Boolean = false,
    // true = в белом списке батареи (по умолчанию true, чтобы не мигать карточкой до проверки).
    val batteryUnrestricted: Boolean = true,
)

/** Полный иммутабельный снимок состояния главного экрана. */
data class MainUiState(
    val apps: List<AppInfo> = emptyList(),          // уже отфильтрованный по поиску список
    val triggerPackages: Set<String> = emptySet(),  // выбранные триггеры (для чекбоксов)
    val searchQuery: String = "",
    val settings: AutomationSettings = AutomationSettings(),
    val vpnAppLabel: String? = null,                // человекочитаемое имя выбранного VPN
    val permissions: PermissionState = PermissionState(),
)
