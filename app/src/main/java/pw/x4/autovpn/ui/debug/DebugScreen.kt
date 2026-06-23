package pw.x4.autovpn.ui.debug

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pw.x4.autovpn.AutoVpnApp
import pw.x4.autovpn.ui.main.MainViewModel
import pw.x4.autovpn.util.VpnStateChecker

/** Простая панель отладки: реальное состояние VPN + ручной тест broadcast-тумблера. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as AutoVpnApp).container }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var vpnActive by remember { mutableStateOf(VpnStateChecker.isVpnActive(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vpnActive = VpnStateChecker.isVpnActive(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoRow("VPN активен сейчас", if (vpnActive) "ДА" else "нет")
            InfoRow("VPN-пакет", state.settings.vpnPackage ?: "— не выбран")
            InfoRow("Broadcast-экшен", state.settings.toggleAction)
            InfoRow("Триггеров выбрано", state.triggerPackages.size.toString())

            Button(
                onClick = {
                    val pkg = state.settings.vpnPackage
                    if (pkg == null) {
                        Toast.makeText(context, "Сначала выбери VPN-приложение", Toast.LENGTH_SHORT).show()
                    } else {
                        container.broadcastVpnTrigger.sendToggle(pkg, state.settings.toggleAction)
                        Toast.makeText(context, "Тумблер отправлен", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Тест тумблера VPN (отправить broadcast)") }

            OutlinedButton(
                onClick = { vpnActive = VpnStateChecker.isVpnActive(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Обновить состояние") }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}
