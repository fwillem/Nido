package com.example.nido.utils

import android.util.Log

/**
 * A simple logging utility that provides custom trace functionality with emojis
 * and more meaningful tags. The aim of this utility is:
 * - Best of the original (simplicity, Logcat compatibility, inline efficiency)
 * - Modern improvements (emojis, auto-tagging for caller identification, thread safety, println integration)
 * - Completely future-proof – Can be extended (print to file, remote debugging, conditional traces) without modifying existing code
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
    FATAL        // Fatal error, will raise an exception
}

/**
 * Maximum tag length for Android Studio Logcat.
 * Some Android API levels enforce a maximum tag length (commonly 23 characters).
 */
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

/**
 * Returns an emoji based on the trace log level.
 */
fun emojiForLevel(level: TraceLogLevel): String = when (level) {
    TraceLogLevel.VERBOSE    -> "🟡"
    TraceLogLevel.DEBUG      -> "🟡"
    TraceLogLevel.INFO       -> "🟡"
    TraceLogLevel.WARNING    -> "⚠"
    TraceLogLevel.ERROR      -> "❌"
    TraceLogLevel.FATAL      -> "\uD83D\uDC80" // Death's head emoji
}

/**
 * Thread-local flag to detect when TRACE() is running.
 * This prevents the custom println() from printing duplicate output when called within TRACE().
 */
val isInsideTrace = ThreadLocal.withInitial { false }

/**
 * Logs a message at the specified trace log level.
 * Prevents duplicate logs by suppressing println() when used inside TRACE().
 *
 * @param level the log level.
 * @param tag an optional tag. Defaults to a generated tag from [getTag].
 * @param message a lambda that returns the log message.
 */
inline fun TRACE(
    level: TraceLogLevel,
    tag: String = getTag(),
    message: () -> String
) {
    isInsideTrace.set(true)  // Prevent duplicate printing via println()
    val decoratedMessage = "${emojiForLevel(level)} ${message()}"
    isInsideTrace.set(false) // Restore normal println() behavior

    when (level) {
        TraceLogLevel.VERBOSE -> Log.v(tag, decoratedMessage)
        TraceLogLevel.DEBUG    -> Log.d(tag, decoratedMessage)
        TraceLogLevel.INFO     -> Log.i(tag, decoratedMessage)
        TraceLogLevel.WARNING  -> Log.w(tag, decoratedMessage)
        TraceLogLevel.ERROR    -> Log.e(tag, decoratedMessage)
        TraceLogLevel.FATAL    -> {
            Log.wtf(tag, decoratedMessage)
            throw RuntimeException(decoratedMessage)
        }
    }
}

/**
 * Custom println function that prevents duplicate logs when used inside TRACE().
 * If TRACE() is active (as determined by [isInsideTrace]), it returns the message
 * without printing; otherwise, it prints to the standard output.
 */
fun println(message: String): String {
    return if (isInsideTrace.get() ?: false) {
        message // Do not print if inside TRACE()
    } else {
        message.also { kotlin.io.println(it) } // Normal println behavior, but also returns the message
    }
}

/**
 * Spare: Emoji bank.
 */
object TraceEmojis {
    val emojis = arrayOf("😀", "😂", "😎", "😍", "🥳", "🟢", "🟡", "🟥", "🟦", "🟨", "🔹", "❌", "✅", "🔄")
}
