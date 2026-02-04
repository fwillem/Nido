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
 * - For FATAL logs, we emit an AppDialogEvent.BlueScreenOfDeath
 *   via UiEventBridge so the UI (NidoApp) can show a blocking dialog.
 *   If you prefer an immediate crash, uncomment the throw at the end.
 */

/**
 * Log levels for TRACE.
 */
enum class TraceLogLevel {
    VERBOSE,     // Very detailed traces
    DEBUG,       // Detailed traces
    INFO,        // Basic traces
    WARNING,     // Warning (abnormal situation, not planned)
    ERROR,       // Error, something went wrong but program can go on. Needs to be solved
    FATAL        // Fatal error, will raise an exception or BSOD dialog
}

/** Maximum tag length for Android Studio Logcat. */
const val tagMaxLen = 23

/**
 * Attempts to generate a meaningful tag by inspecting the current stack trace.
 * It uses the caller's class and method name (formatted as "ClassName:methodName").
 * If the tag exceeds [tagMaxLen], it will be truncated appropriately.
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

/** Returns an emoji based on the trace log level. */
fun emojiForLevel(level: TraceLogLevel): String = when (level) {
    TraceLogLevel.VERBOSE    -> "Nido 🟡"
    TraceLogLevel.DEBUG      -> "Nido 🟡"
    TraceLogLevel.INFO       -> "Nido 🟡"
    TraceLogLevel.WARNING    -> "Nido ⚠"
    TraceLogLevel.ERROR      -> "Nido ❌"
    TraceLogLevel.FATAL      -> "\uD83D\uDC80" // Skull emoji
}

/**
 * Thread-local flag to detect when TRACE() is running.
 * This prevents the custom println() from printing duplicate output
 * when called within TRACE().
 */
val isInsideTrace = ThreadLocal.withInitial { false }

/**
 * Fixed global tag for easy filtering.
 */
const val GLOBAL_TAG = "Nido"

/**
 * Logs a message at the specified trace log level.
 * Prevents duplicate logs by suppressing println() when used inside TRACE().
 *
 * @param level the log level.
 * @param tag NOT USED explicitly anymore for Logcat tag (we use GLOBAL_TAG), but used for context.
          Defaults to a generated string from [getTag] (Class:Method).
 * @param message a lambda that returns the log message.
 */
inline fun TRACE(
    level: TraceLogLevel,
    tag: String = getTag(), // This now represents the "Context" (Class:Method)
    message: () -> String
) {
    isInsideTrace.set(true)
    
    // Format: "🟡 [Class:Method] The message content"
    // We use GLOBAL_TAG for the Android Log Tag so you can filter by "tag:Nido"
    val contextInfo = "[$tag]" 
    val content = message()
    val decoratedMessage = "${emojiForLevel(level)} $contextInfo $content"
    val plainMessage = "$contextInfo $content"
    
    isInsideTrace.set(false) 

    when (level) {
        TraceLogLevel.VERBOSE -> Log.v(GLOBAL_TAG, decoratedMessage)
        TraceLogLevel.DEBUG   -> Log.d(GLOBAL_TAG, decoratedMessage) // Reverted to .d, now that we have a solid filter
        TraceLogLevel.INFO    -> Log.i(GLOBAL_TAG, decoratedMessage)
        TraceLogLevel.WARNING -> Log.w(GLOBAL_TAG, decoratedMessage)
        TraceLogLevel.ERROR   -> Log.e(GLOBAL_TAG, decoratedMessage)
        TraceLogLevel.FATAL   -> {
            Log.wtf(GLOBAL_TAG, decoratedMessage)
            UiEventBridge.emit(
                AppDialogEvent.BlueScreenOfDeath(
                    tag = GLOBAL_TAG,
                    message = { plainMessage }
                )
            )
        }
    }
}

/**
 * Custom println function that prevents duplicate logs when used inside TRACE().
 * If TRACE() is active (as determined by [isInsideTrace]), it returns the message
 * without printing; otherwise, it prints to the standard output.
 */
fun println(message: String): String {
    return if (isInsideTrace.get()) {
        message // Do not print if inside TRACE()
    } else {
        message.also { kotlin.io.println(it) }
    }
}

/** Spare: Emoji bank. */
object TraceEmojisBank {
    val emojis = arrayOf(
        "😀","😂","😎","😍","🥳","🟢","🟡","🟥","🟦","🟨",
        "🔹","❌","✅","🔄","🍾","⭐","🔥","✨","🌟"
    )
}
