package pw.x4.autovpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF22D3EE)

private val DarkColors = darkColorScheme(
    primary = Accent,
    background = Color(0xFF0E0F13),
    surface = Color(0xFF15171D),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E7490),
)

@Composable
fun AutoVpnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
