package uk.motobay.app.util

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import uk.motobay.app.data.Moderators
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the stack trace of the last crash on the device so it can be shared
 * from Settings. There is no crash-reporting service yet, and the moderator
 * testing the app needs a way to send back what went wrong.
 */
class CrashLog(private val context: Context, private val versionName: String) {
    private val file = File(context.filesDir, "last-crash.txt")

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { file.writeText(describe(thread, error)) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(): String? = file.takeIf { it.exists() }?.readText()

    private val prefs by lazy { context.getSharedPreferences("crashlog", Context.MODE_PRIVATE) }

    /** Remembered on every navigation, so a report can say where the rider was. */
    fun noteScreen(name: String) {
        prefs.edit().putString("last_screen", name).putLong("last_screen_at", System.currentTimeMillis()).apply()
    }

    private fun lastScreenLine(): String {
        val screen = prefs.getString("last_screen", null) ?: return "Last screen: unknown"
        val at = prefs.getLong("last_screen_at", 0L)
        return "Last screen: $screen (opened ${SimpleDateFormat("HH:mm:ss", Locale.UK).format(Date(at))})"
    }

    /**
     * Uncaught exceptions are only one way to die. Native crashes (the map is
     * native code), ANR kills and the system reclaiming memory never reach the
     * handler above, so on Android 11+ ask the system why the last process ended
     * and keep that as the report instead.
     */
    fun collectSystemExitReason(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || file.exists()) return
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val lastSeen = prefs.getLong("last_exit", 0L)
        val exit = runCatching { am.getHistoricalProcessExitReasons(context.packageName, 0, 5) }.getOrNull()
            ?.filter { it.timestamp > lastSeen }
            ?.maxByOrNull { it.timestamp } ?: return
        prefs.edit().putLong("last_exit", exit.timestamp).apply()
        if (exit.reason !in REPORTABLE) return
        val trace = runCatching { exit.traceInputStream?.use { it.readBytes() } }.getOrNull()?.let(::readableTrace)
        val report = buildString {
            append("MotoBays $versionName · Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK).format(Date(exit.timestamp)))
            append(" · process ended: ${reasonName(exit.reason)} (status ${exit.status})\n")
            append("${exit.description ?: "No description from the system"}\n")
            append("App was ${importanceName(exit.importance)} when it ended (importance ${exit.importance})\n")
            append("${lastScreenLine()}\n")
            append("Memory at the time: ${exit.pss / 1024} MB PSS, ${exit.rss / 1024} MB RSS\n\n")
            if (!trace.isNullOrBlank()) append(trace.take(40_000))
        }
        runCatching { file.writeText(report) }
    }

    /**
     * ANR traces are plain text. Native crash traces are a binary tombstone
     * (protobuf), so keep only the readable runs: the signal, the abort
     * message, thread names and the symbol names of each frame.
     */
    private fun readableTrace(bytes: ByteArray): String {
        val text = String(bytes, Charsets.ISO_8859_1)
        if (text.none { it.code < 9 || (it.code in 14..31) || it.code > 126 }) return text
        val runs = Regex("[\\x20-\\x7E]{5,}").findAll(text).map { it.value.trim() }
            .filter { r -> r.any { it.isLetter() } }
            .filterNot { it.startsWith("/apex/") || it.startsWith("/system/") || it.startsWith("/vendor/") || it.startsWith("[anon") || it.matches(Regex("[0-9a-f]{20,}")) }
            .toList()
        return "(native trace, readable parts only)\n" + runs.joinToString("\n")
    }

    private fun importanceName(importance: Int) = when {
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "ON SCREEN (foreground)"
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "visible but not focused"
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "running in the background"
        else -> "in the background, cached (not on screen)"
    }

    private fun reasonName(reason: Int) = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "crash (Java exception)"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "native crash"
        ApplicationExitInfo.REASON_ANR -> "not responding (ANR)"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "killed to free memory"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive resource usage"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "initialization failure"
        ApplicationExitInfo.REASON_SIGNALED -> "killed by signal"
        ApplicationExitInfo.REASON_OTHER -> "other"
        else -> "reason $reason"
    }

    private companion object {
        val REPORTABLE = setOf(
            ApplicationExitInfo.REASON_CRASH, ApplicationExitInfo.REASON_CRASH_NATIVE, ApplicationExitInfo.REASON_ANR,
            ApplicationExitInfo.REASON_LOW_MEMORY, ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE, ApplicationExitInfo.REASON_SIGNALED, ApplicationExitInfo.REASON_OTHER,
        )
    }

    fun clear() {
        file.delete()
    }

    /** Opens the mail app with the report addressed to the maintainer. */
    fun emailIntent(report: String): Intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(Moderators.SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "MotoBays crash report")
        putExtra(Intent.EXTRA_TEXT, report)
    }

    /** Fallback when no mail app is installed. */
    fun shareIntent(report: String): Intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(Moderators.SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "MotoBays crash report")
            putExtra(Intent.EXTRA_TEXT, report)
        },
        "Send crash report",
    )

    private fun describe(thread: Thread, error: Throwable): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK).format(Date())
        return "MotoBays $versionName · Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL}\n$time on thread ${thread.name}\n${lastScreenLine()}\n\n$trace"
    }
}
