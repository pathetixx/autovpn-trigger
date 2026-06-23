package pw.x4.autovpn.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import pw.x4.autovpn.ui.debug.DebugScreen
import pw.x4.autovpn.ui.main.HomeScreen
import pw.x4.autovpn.ui.main.MainViewModel
import pw.x4.autovpn.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val viewModel: MainViewModel = viewModel()
    var tab by rememberSaveable { mutableIntStateOf(0) } // 0 = Главная, 1 = Настройки
    var showDebug by remember { mutableStateOf(false) }

    // Debug — отдельный полноэкранный sub-screen со своим back.
    if (showDebug) {
        DebugScreen(viewModel, onBack = { showDebug = false })
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (tab == 0) "AutoVPN" else "Настройки") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Главная") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Настройки") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeScreen(viewModel)
                else -> SettingsScreen(viewModel, onOpenDebug = { showDebug = true })
            }
        }
    }
}
