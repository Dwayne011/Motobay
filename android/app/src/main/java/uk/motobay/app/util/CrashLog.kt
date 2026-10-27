package uk.motobay.app.util

import android.content.Context
import android.content.Intent
import android.os.Build
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
class CrashLog(context: Context, private val versionName: String) {
    private val file = File(context.filesDir, "last-crash.txt")

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { file.writeText(describe(thread, error)) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(): String? = file.takeIf { it.exists() }?.readText()

    fun clear() {
        file.delete()
    }

    fun shareIntent(report: String): Intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "MotoBays crash report")
            putExtra(Intent.EXTRA_TEXT, report)
        },
        "Share crash report",
    )

    private fun describe(thread: Thread, error: Throwable): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK).format(Date())
        return "MotoBays $versionName · Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL}\n$time on thread ${thread.name}\n\n$trace"
    }
}
