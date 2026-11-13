package com.phataklive.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.phataklive.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Phatak Live ships as an APK on GitHub Releases, so the app looks for updates itself: once per launch it
 * reads the repo's latest release, compares its tag with the running version, and if newer offers the
 * release notes ("What's new") and a one-tap update. The APK is downloaded into the app's cache and handed
 * to Android's installer, which installs it over the current app (same package, same signing key):
 * nothing to uninstall, settings kept.
 */
object UpdateChecker {

    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/JyotirmoyLaha/phatak-live/releases/latest"
    private const val APK_PREFIX = "PhatakLive-"
    private const val CACHE_SUBDIR = "updates"

    data class UpdateInfo(val version: String, val releaseUrl: String, val apkUrl: String?, val apkSize: Long, val notes: String)

    sealed interface DownloadState {
        data object Idle : DownloadState
        data class Downloading(val fraction: Float) : DownloadState
        data class Ready(val file: File) : DownloadState
        data class Failed(val message: String) : DownloadState
    }

    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()

    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available = _available.asStateFlow()

    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download = _download.asStateFlow()

    @Volatile private var cancelled = false

    /** Looks for a newer release. Silent on any error: no network just means no update notice. */
    suspend fun check() = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(LATEST_RELEASE_URL).header("Accept", "application/vnd.github+json").build()
            val body = http.newCall(req).execute().use { r -> if (r.isSuccessful) r.body?.string() else null } ?: return@runCatching
            val rel = JSONObject(body)
            if (rel.optBoolean("draft") || rel.optBoolean("prerelease")) return@runCatching
            val latest = rel.getString("tag_name").removePrefix("v")
            if (!isNewer(latest, BuildConfig.VERSION_NAME)) return@runCatching
            val assets = rel.optJSONArray("assets")
            var apkUrl: String? = null
            var apkSize = 0L
            for (i in 0 until (assets?.length() ?: 0)) {
                val a = assets!!.getJSONObject(i)
                val name = a.optString("name")
                if (name.startsWith(APK_PREFIX, true) && name.endsWith(".apk", true) && a.optString("state") == "uploaded") {
                    apkUrl = a.optString("browser_download_url"); apkSize = a.optLong("size"); break
                }
            }
            _available.value = UpdateInfo(latest, rel.optString("html_url"), apkUrl, apkSize, rel.optString("body"))
        }
    }

    /** A downloaded APK is only offered in the session that downloaded it; each cold start begins clean. */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        File(context.cacheDir, CACHE_SUBDIR).listFiles()?.forEach { it.delete() }
    }

    suspend fun downloadApk(context: Context): Unit = withContext(Dispatchers.IO) {
        val info = _available.value ?: return@withContext
        val url = info.apkUrl ?: return@withContext
        cancelled = false
        _download.value = DownloadState.Downloading(0f)
        runCatching {
            val dir = File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "phatak-live-${info.version}.apk")
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                check(r.isSuccessful) { "HTTP ${r.code}" }
                val b = r.body ?: error("empty download")
                val total = b.contentLength().takeIf { it > 0 } ?: info.apkSize.takeIf { it > 0 }
                b.byteStream().use { input ->
                    target.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            if (cancelled) { _download.value = DownloadState.Idle; return@withContext }
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            total?.let { _download.value = DownloadState.Downloading((done.toFloat() / it).coerceIn(0f, 1f)) }
                        }
                    }
                }
            }
            _download.value = DownloadState.Ready(target)
        }.onFailure { e ->
            _download.value = if (cancelled) DownloadState.Idle else DownloadState.Failed(e.message ?: "download failed")
        }
    }

    fun cancelDownload() { cancelled = true }
    fun resetDownload() { _download.value = DownloadState.Idle }
    fun dismiss() { _available.value = null }

    /**
     * Opens Android's installer for the downloaded APK. Sideloaded apps need the per-app "Install unknown apps"
     * permission once; without it the user is sent to that switch first and taps Install again afterwards.
     */
    fun install(context: Context, file: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        @Suppress("DEPRECATION")
        context.startActivity(
            Intent(Intent.ACTION_INSTALL_PACKAGE)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }

    /** Dotted numeric comparison ("1.10" > "1.9"); a "-beta" build counts as older than the same plain release. */
    internal fun isNewer(latest: String, current: String): Boolean {
        fun parse(v: String): Pair<List<Int>, Boolean> {
            val dash = v.indexOf('-')
            val base = if (dash >= 0) v.substring(0, dash) else v
            return base.split(".").map { it.toIntOrNull() ?: 0 } to (dash >= 0)
        }
        val (l, lPre) = parse(latest)
        val (c, cPre) = parse(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val a = l.getOrElse(i) { 0 }; val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return cPre && !lPre
    }

}
