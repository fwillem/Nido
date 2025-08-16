package com.example.nido.utils

import android.util.Log
import com.example.nido.events.AppDialogEvent
import com.example.nido.events.UiEventBridge

/**
 * TRACE logging utility.
 *
 * Goals:
 * - Keep Logcat-friendly simplicity and inline efficiency.
 * - Add readable emojis and auto-tagging (Class:method).
 * - Avoid duplicate println() when TRACE() builds the string.
 * - Integrate with the UI event system for fatal errors (BSOD dialog).
 *
 * Architecture tie-in:
 * - For FATAL logs, we DO NOT throw here. We emit an AppDialogEvent.BlueScreenOfDeath
 *   via UiEventBridge so the UI (NidoApp) can show a blocking dialog.
 *   If you prefer an immediate crash, uncomment the throw at the end of the FATAL branch.
 */

/** Log levels for TRACE. */
enum class TraceLogLevel {
    VERBOSE,    // Very detailed traces
    DEBUG,      // Detailed traces
    INFO,       // Basic traces
    WARNING,    // Abnormal but recoverable
    ERROR,      // Error: app can continue, needs attention
    FATAL       // Fatal: routed to BSOD dialog + optional crash
}

/** Max tag length (some Android versions enforce 23 chars). */
const val tagMaxLen = 23

/**
 * Generate a meaningful tag from the caller's stack trace in the form "ClassName:methodName".
 * If it exceeds [tagMaxLen], truncate class/method parts intelligently.
 */
fun getTag(): String {
    val stackTrace = Throwable().stackTrace
    val fullTag = stackTrace.getOrNull(1)
        ?.let { "${it.className.substringAfterLast('.')}:${it.methodName}" }
        ?: "UnknownTag"

    return if (fullTag.length > tagMaxLen) {
        val methodPart = fullTag.substringAfter(":")
        val classPart = fullTag.substringBefore(":")
        if (methodPart.length >= tagMaxLen) {
            methodPart.take(tagMaxLen)
        } else {
            "${classPart.take(tagMaxLen - methodPart.length - 1)}:$methodPart"
        }
    } else {
        fullTag
    }
}

/** Emoji decoration by level (purely cosmetic). */
fun emojiForLevel(level: TraceLogLevel): String = when (level) {
    TraceLogLevel.VERBOSE -> "🟡"
    TraceLogLevel.DEBUG   -> "🟡"
    TraceLogLevel.INFO    -> "🟡"
    TraceLogLevel.WARNING -> "⚠"
    TraceLogLevel.ERROR   -> "❌"
    TraceLogLevel.FATAL   -> "\uD83D\uDC80" // skull
}

/**
 * Thread-local guard to prevent our custom println() from double-printing
 * when TRACE() itself generates the message string.
 */
private val isInsideTrace = ThreadLocal.withInitial { false }

/**
 * Log a message at the given level. For FATAL:
 * - Log with WTF
 * - Emit a BSOD dialog event via UiEventBridge (handled globally by NidoApp)
 * - (Optional) Throw to crash immediately — commented out to let the dialog show
 *
 * @param level   Log level.
 * @param tag     Optional tag; defaults to auto-generated from caller.
 * @param message Lazy message builder.
 */
inline fun TRACE(
    level: TraceLogLevel,
    tag: String = getTag(),
    message: () -> String
) {
    val decoratedMessage = "${emojiForLevel(level)} ${message()}"
    val plainMessage = "${message()}"

    when (level) {
        TraceLogLevel.VERBOSE -> Log.v(tag, decoratedMessage)
        TraceLogLevel.DEBUG   -> Log.d(tag, decoratedMessage)
        TraceLogLevel.INFO    -> Log.i(tag, decoratedMessage)
        TraceLogLevel.WARNING -> Log.w(tag, decoratedMessage)
        TraceLogLevel.ERROR   -> Log.e(tag, decoratedMessage)
        TraceLogLevel.FATAL   -> {
            Log.wtf(tag, decoratedMessage)

            // Route to the global BSOD dialog (AppDialogEvent handled by NidoApp).
            // This respects the two-pipes architecture: UI events go through UiEventBridge.
            UiEventBridge.emit(
                AppDialogEvent.BlueScreenOfDeath(
                    tag = tag,
                    message = { message }
                )
            )

            // If you prefer to crash immediately instead of showing the dialog, uncomment:
            // throw RuntimeException(decoratedMessage)
        }
    }
}


/** Spare emoji bank for future use. */
object TraceEmojisBank {
    val emojis = arrayOf(
        "😀","😂","😎","😍","🥳","🟢","🟡","🟥","🟦","🟨",
        "🔹","❌","✅","🔄","🍾","⭐","🔥","✨","🌟"
    )
}
