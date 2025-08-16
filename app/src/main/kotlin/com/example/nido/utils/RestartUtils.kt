package com.example.nido.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.nido.MainActivity
import com.example.nido.utils.TraceLogLevel.INFO
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


fun copyDebugReport(
    activity: Activity,
    tag: String,
    message: String,
    gameStateDump: String
) {
    val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    val sys = "Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT}) · Locale: ${Locale.getDefault()}"
    val payload = buildString {
        appendLine("Nido Debug Report • $now")
        appendLine(sys)
        appendLine()
        appendLine("TAG: $tag")
        appendLine("MESSAGE: $message")
        appendLine()
        appendLine("--- GAME STATE ---")
        appendLine(gameStateDump.trim())
    }
    val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Nido BSOD", payload))
    TRACE(INFO) { "copyDebugReport(): $tag" }
}

/**
 * Hard restart: finishes the current task and relaunches [MainActivity].
 * Use for fatal errors or global setting changes (e.g., locale).
 *
 * @param activity The current Activity (used to finish & relaunch the task).
 * @param forceLanding If true, passes an extra so app can route to landing at startup.
 * @param target The Activity class to relaunch. Defaults to [MainActivity].
 */
fun hardRestartApp(
    activity: Activity,
    forceLanding: Boolean = true,
    target: Class<out Activity> = MainActivity::class.java
) {
    TRACE(INFO) { "hardRestartApp(): relaunching ${target.simpleName}, forceLanding=$forceLanding" }

    val intent = Intent(activity, target).apply {
        if (forceLanding) putExtra("force_landing", true)
        // Clear the current task and start a new one
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

    // Finish current activity and start fresh
    activity.finish()
    activity.startActivity(intent)
    // Remove transition animation for a snappier feel (optional)
    activity.overridePendingTransition(0, 0)
}
