package pw.x4.autovpn.ui.main

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pw.x4.autovpn.domain.model.AppInfo
import pw.x4.autovpn.util.PermissionUtils

@Composable
fun HomeScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Статус разрешений может смениться в системных Настройках — обновляем на возврате.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshPermissions() }
    val usageAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshPermissions() }
    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshPermissions() }

    var showVpnPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        MasterToggleRow(
            enabled = state.settings.automationEnabled,
            onToggle = viewModel::setAutomationEnabled,
        )

        if (!state.permissions.usageAccess) {
            PermissionCard(
                text = "Нужен «Доступ к данным об использовании», чтобы определять открытое приложение.",
                actionLabel = "Открыть настройки",
                onAction = { usageAccessLauncher.launch(PermissionUtils.usageAccessSettingsIntent()) },
            )
        }
        if (!state.permissions.notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionCard(
                text = "Разрешите уведомления — без них Android может убить фоновую службу.",
                actionLabel = "Разрешить",
                onAction = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            )
        }
        if (!state.permissions.batteryUnrestricted) {
            PermissionCard(
                text = "Отключите энергосбережение для приложения — иначе система душит фоновую службу.",
                actionLabel = "Отключить",
                onAction = {
                    batteryLauncher.launch(PermissionUtils.requestIgnoreBatteryOptimizationsIntent(context))
                },
            )
        }

        VpnServiceRow(
            vpnLabel = state.vpnAppLabel ?: state.settings.vpnPackage,
            onPick = { showVpnPicker = true },
        )

        HorizontalDivider()

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchChange,
            singleLine = true,
            label = { Text("Поиск приложений") },
            modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
        )

        Text(
            "Выберите приложения, при открытии которых поднимать VPN:",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.apps, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    checked = app.packageName in state.triggerPackages,
                    onCheckedChange = { viewModel.toggleTrigger(app.packageName, it) },
                )
            }
        }
    }

    if (showVpnPicker) {
        VpnPickerDialog(
            apps = state.apps,
            onSelect = {
                viewModel.selectVpn(it.packageName)
                showVpnPicker = false
            },
            onDismiss = { showVpnPicker = false },
        )
    }
}

@Composable
private fun MasterToggleRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Автоматизация", style = MaterialTheme.typography.titleMedium)
            Text(
                if (enabled) "Включена — слежу за приложениями" else "Выключена",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun PermissionCard(text: String, actionLabel: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun VpnServiceRow(vpnLabel: String?, onPick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("VPN-служба", style = MaterialTheme.typography.titleMedium)
            Text(
                vpnLabel ?: "не выбрана",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OutlinedButton(onClick = onPick) { Text("Выбрать") }
    }
}

@Composable
private fun AppRow(app: AppInfo, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun VpnPickerDialog(apps: List<AppInfo>, onSelect: (AppInfo) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите VPN-приложение") },
        text = {
            LazyColumn {
                items(apps, key = { it.packageName }) { app ->
                    TextButton(onClick = { onSelect(app) }, modifier = Modifier.fillMaxWidth()) {
                        Text(app.label, modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
