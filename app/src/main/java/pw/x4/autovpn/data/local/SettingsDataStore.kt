package pw.x4.autovpn.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// Один экземпляр DataStore на процесс (delegate кэширует его сам).
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "automation_settings")

object SettingsKeys {
    val AUTOMATION_ENABLED = booleanPreferencesKey("automation_enabled")
    val VPN_PACKAGE = stringPreferencesKey("vpn_package")
}
