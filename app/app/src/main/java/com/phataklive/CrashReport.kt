package com.phataklive

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import java.io.File
import java.time.Instant
import java.time.ZoneId

/**
 * If Phatak Live crashed or froze ("not responding") last time, the next launch offers to share a short report, so a
 * problem on any phone can be traced without a cable. Nothing is sent by itself: the user decides, through Android's
 * share sheet. Reports hold the app and Android version, the phone model and the error, never location or searches.
 */
object CrashReport {
    private const val FILE = "last_crash.txt"
    private const val PREFS = "crash"
    private const val SEEN = "exit_seen_ms"
    private const val MAX = 12_000

    private fun header(ctx: Context, what: String, whenMs: Long) =
        "Phatak Live ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Android ${Build.VERSION.RELEASE} · " +
            "${Build.MANUFACTURER} ${Build.MODEL}\n$what at ${Instant.ofEpochMilli(whenMs).atZone(ZoneId.of("Asia/Kolkata"))}\n\n"

    /** Keeps the stack trace of an uncaught exception for the next launch, then lets Android handle the crash as usual. */
    fun install(ctx: Context) {
        val app = ctx.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is Handler) return
        Thread.setDefaultUncaughtExceptionHandler(Handler(app, previous))
    }

    private class Handler(val ctx: Context, val previous: Thread.UncaughtExceptionHandler?) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread, e: Throwable) {
            runCatching {
                File(ctx.filesDir, FILE).writeText((header(ctx, "Crash on thread ${t.name}", System.currentTimeMillis()) + e.stackTraceToString()).take(MAX))
            }
            previous?.uncaughtException(t, e)
        }
    }

    /**
     * The report waiting to be shared, if any: a crash kept by [install], or (Android 11+) a crash, native crash or
     * freeze Android recorded for this app since the last launch, with the screen thread's stack for a freeze.
     */
    fun pending(ctx: Context): String? {
        File(ctx.filesDir, FILE).takeIf { it.exists() }?.let { f -> return runCatching { f.readText() }.getOrNull() }
        if (Build.VERSION.SDK_INT < 30) return null
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = prefs.getLong(SEEN, -1L)
        val am = ctx.getSystemService(ActivityManager::class.java) ?: return null
        val exits = runCatching { am.getHistoricalProcessExitReasons(ctx.packageName, 0, 5) }.getOrNull() ?: return null
        val newest = exits.maxOfOrNull { it.timestamp } ?: return null
        prefs.edit().putLong(SEEN, newest).apply()
        if (seen < 0) return null   // first launch with this feature: older history is not reported
        val bad = exits.filter { it.timestamp > seen }.firstOrNull {
            it.reason == ApplicationExitInfo.REASON_ANR || it.reason == ApplicationExitInfo.REASON_CRASH ||
                it.reason == ApplicationExitInfo.REASON_CRASH_NATIVE
        } ?: return null
        val what = when (bad.reason) {
            ApplicationExitInfo.REASON_ANR -> "Not responding"
            ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native crash"
            else -> "Crash"
        }
        val trace = runCatching { bad.traceInputStream?.bufferedReader()?.use { it.readText() } }.getOrNull()
        // a freeze trace lists every thread: the screen thread ("main") is the one that matters
        val main = trace?.let { tr ->
            val i = tr.indexOf("\"main\"")
            if (i < 0) tr else tr.substring(i, minOf(tr.length, i + 6_000))
        }
        return (header(ctx, what, bad.timestamp) + (bad.description ?: "") + "\n\n" + (main ?: "")).take(MAX)
    }

    fun clear(ctx: Context) {
        runCatching { File(ctx.filesDir, FILE).delete() }
    }
}
