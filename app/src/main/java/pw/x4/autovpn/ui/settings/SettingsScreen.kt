package pw.x4.autovpn.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pw.x4.autovpn.BuildConfig
import pw.x4.autovpn.data.update.UpdateInfo
import pw.x4.autovpn.ui.main.MainViewModel
import pw.x4.autovpn.ui.main.UpdateState

@Composable
fun SettingsScreen(viewModel: MainViewModel, onOpenDebug: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle("Обновления")
        SwitchRow(
            title = "Автообновление",
            subtitle = "Проверять новые версии при запуске",
            checked = state.settings.autoUpdateEnabled,
            onCheckedChange = viewModel::setAutoUpdateEnabled,
        )
        Button(
            onClick = viewModel::checkForUpdate,
            enabled = updateState !is UpdateState.Checking,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Проверить наличие обновлений") }
        UpdateStatus(updateState, onInstall = viewModel::installUpdate)

        HorizontalDivider()

        SectionTitle("Поведение")
        SwitchRow(
            title = "Уважать ручной VPN",
            subtitle = "Не выключать VPN, включённый вручную в Happ — он будет работать и вне правил",
            checked = state.settings.respectManualVpn,
            onCheckedChange = viewModel::setRespectManualVpn,
        )

        HorizontalDivider()

        SectionTitle("Отладка")
        OutlinedButton(onClick = onOpenDebug, modifier = Modifier.fillMaxWidth()) {
            Text("Открыть Debug")
        }

        Text(
            "Версия ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun UpdateStatus(state: UpdateState, onInstall: (UpdateInfo) -> Unit) {
    when (state) {
        is UpdateState.Checking -> StatusText("Проверяю…")
        is UpdateState.UpToDate -> StatusText("Установлена последняя версия")
        is UpdateState.Installing -> StatusText("Скачиваю обновление…")
        is UpdateState.Error -> StatusText("Ошибка: ${state.message}")
        is UpdateState.Available -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusText("Доступна версия ${state.info.versionName}")
            Button(
                onClick = { onInstall(state.info) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Обновить") }
        }
        UpdateState.Idle -> Unit
    }
}

@Composable
private fun StatusText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
