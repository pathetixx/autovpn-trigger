package pw.x4.autovpn.ui.diagnostics

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pw.x4.autovpn.AutoVpnApp
import pw.x4.autovpn.data.installed.ComponentType
import pw.x4.autovpn.data.installed.ExposedComponent
import pw.x4.autovpn.domain.vpn.LaunchMode
import pw.x4.autovpn.domain.vpn.VpnIntentConfig

/**
 * Экран поиска «тихого коннекта»: сканирует экспортированные компоненты выбранного VPN
 * и даёт их по очереди протестировать. SERVICE/RECEIVER срабатывают без окна — если после
 * теста VPN поднялся и приложение не всплыло, компонент «Назначается» как цель автозапуска.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(vpnPackage: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as AutoVpnApp).container }
    val scope = rememberCoroutineScope()
    var components by remember { mutableStateOf<List<ExposedComponent>>(emptyList()) }
    var scanned by remember { mutableStateOf(false) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Диагностика коннекта") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            if (vpnPackage.isNullOrBlank()) {
                Text("Сначала выбери VPN-приложение на главном экране.")
                return@Column
            }

            Text("Цель: $vpnPackage", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Тестируй SERVICE и RECEIVER — они срабатывают без окна. Если после «Тест» VPN " +
                    "подключился, а приложение НЕ всплыло — жми «Назначить»: автозапуск будет дёргать " +
                    "именно этот компонент в фоне.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    components = container.appComponentScanner.scanExported(vpnPackage)
                    scanned = true
                    toast("Найдено компонентов: ${components.size}")
                }) { Text("Сканировать") }
                OutlinedButton(onClick = {
                    scope.launch { container.settingsRepository.setConnectTarget(LaunchMode.OPEN_APP, null) }
                    toast("Сброшено: просто открывать приложение")
                }) { Text("Сброс") }
            }
            Spacer(Modifier.height(12.dp))

            if (scanned && components.isEmpty()) {
                Text(
                    "Экспортированных незащищённых компонентов нет — тихого внешнего коннекта " +
                        "у этого приложения не предусмотрено.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            LazyColumn {
                items(components, key = { it.type.name + it.className }) { component ->
                    ComponentRow(
                        component = component,
                        onTest = {
                            val ok = container.vpnTrigger.launch(
                                vpnPackage,
                                VpnIntentConfig(mode = component.type.toLaunchMode(), componentClass = component.className),
                            )
                            toast(if (ok) "Отправлено → ${component.type}" else "Не удалось отправить")
                        },
                        onAssign = {
                            scope.launch {
                                container.settingsRepository.setConnectTarget(
                                    component.type.toLaunchMode(),
                                    component.className,
                                )
                            }
                            toast("Назначено как коннект")
                        },
                    )
                }
            }
        }
    }
}

private fun ComponentType.toLaunchMode(): LaunchMode = when (this) {
    ComponentType.ACTIVITY -> LaunchMode.ACTIVITY
    ComponentType.SERVICE -> LaunchMode.SERVICE
    ComponentType.RECEIVER -> LaunchMode.BROADCAST
}

@Composable
private fun ComponentRow(
    component: ExposedComponent,
    onTest: () -> Unit,
    onAssign: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                component.type.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(component.className, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onTest) { Text("Тест") }
                OutlinedButton(onClick = onAssign) { Text("Назначить") }
            }
        }
    }
}
