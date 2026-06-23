package pw.x4.autovpn.data.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pw.x4.autovpn.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Информация о доступном релизе. */
data class UpdateInfo(
    val versionName: String,
    val apkUrl: String,
    val notes: String,
)

/**
 * OTA через публичные GitHub Releases (репо публичный → ассет качается без токена).
 * Зависимостей не тянем: HttpURLConnection + встроенный org.json.
 */
class UpdateManager(private val context: Context) {

    /** @return UpdateInfo, если на GitHub лежит версия НОВЕЕ установленной, иначе null. */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val json = httpGet(LATEST_RELEASE_API)
        val release = JSONObject(json)
        val remoteVersion = release.getString("tag_name").removePrefix("v")
        if (!isNewer(remoteVersion, BuildConfig.VERSION_NAME)) return@withContext null

        val assets = release.getJSONArray("assets")
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name").endsWith(".apk")) {
                apkUrl = asset.getString("browser_download_url")
                break
            }
        }
        apkUrl?.let { UpdateInfo(remoteVersion, it, release.optString("body")) }
    }

    /** Качает APK во временный файл и отдаёт его системному установщику. */
    suspend fun downloadAndInstall(info: UpdateInfo): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val apk = File(context.cacheDir, "update.apk")
            (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", USER_AGENT)
                instanceFollowRedirects = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }.inputStream.use { input -> apk.outputStream().use { input.copyTo(it) } }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    /** Сравнение semver "x.y.z": true, если remote строго больше current. */
    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split(".")
        val c = current.split(".")
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrNull(i)?.toIntOrNull() ?: 0
            val cv = c.getOrNull(i)?.toIntOrNull() ?: 0
            if (rv != cv) return rv > cv
        }
        return false
    }

    private companion object {
        const val LATEST_RELEASE_API =
            "https://api.github.com/repos/pathetixx/autovpn-trigger/releases/latest"
        const val USER_AGENT = "AutoVPN-Updater"
        const val TIMEOUT_MS = 12_000
    }
}
