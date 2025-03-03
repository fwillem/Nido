package com.example.nido.utils

import android.util.Log

/**
 * A simple logging utility that provides custom trace functionality with emojis
 * and more meaningful tags.
 */
object Trace {
    val emojis = arrayOf("😀", "😂", "😎", "😍", "🥳", "🟢", "🟡", "🟥", "🟦", "🟨", "🔹", "❌", "✅", "🔄")
}

enum class TraceLogLevel {
    SYSTEMATIC,  // Systematic traces
    DETAIL1,     // Very detailed traces
    DETAIL2,     // Detailed traces
    DETAIL3,     // Basic traces
    WARNING,     // Warning (abnormal situation, not planned)
    ERROR,       // Error, something went wrong but program can go on
    FATAL        // Fatal error, will raise an exception
}

/**
 * Attempts to generate a meaningful tag by inspecting the current stack trace.
 */
const val tagMaxLen = 23

/**
 * Attempts to generate a meaningful tag by inspecting the current stack trace.
 */
fun getTag(): String {
    val stackTrace = Throwable().stackTrace
    val fullTag = stackTrace.getOrNull(1) // Caller method (one step behind)
        ?.let { "${it.className.substringAfterLast('.')}:${it.methodName}" } // Changed "::" to ":"
        ?: "UnknownTag"

    return if (fullTag.length > tagMaxLen) {
        val methodPart = fullTag.substringAfter(":") // Extract method name
        val classPart = fullTag.substringBefore(":")

        // Ensure method name is kept and shorten the class name if needed
        if (methodPart.length >= tagMaxLen) {
            methodPart.take(tagMaxLen) // Worst case, keep only method
        } else {
            "${classPart.take(tagMaxLen - methodPart.length - 1)}:$methodPart" // Adjusted for ":" separator
        }
    } else {
        fullTag
    }
}

/*
fun getTag(): String {
    val stackTrace = Throwable().stackTrace
    for (element in stackTrace.drop(2)) {
        val className = element.className.substringAfterLast('.')
        if (!element.methodName.contains("lambda") &&
            !element.methodName.contains("invoke") &&
            !className.startsWith("kotlin.") &&
            !className.startsWith("androidx.")) {
            return className.take(23) + "." + element.methodName  // Limit to 23 chars
        }
    }
    return "UnknownTag"
}


 */

/*
fun getTag(): String {
    val stackTrace = Throwable().stackTrace
    // Skip the first two elements to bypass internal TRACE calls.
    for (element in stackTrace.drop(2)) {
        // Ignore lambda or generated methods.
        if (!element.methodName.contains("lambda") && !element.methodName.contains("invoke")) {
            val className = element.className.substringAfterLast('.')
            return "$className.${element.methodName}"
        }
    }
    return "UnknownTag"
}

 */



/**
 * Returns an emoji based on the trace log level.
 */
fun emojiForLevel(level: TraceLogLevel): String = when (level) {
    TraceLogLevel.SYSTEMATIC    -> "🟨"
    TraceLogLevel.DETAIL1    -> "🟡"
    TraceLogLevel.DETAIL2    -> "🟡"
    TraceLogLevel.DETAIL3    -> "🟡"
    TraceLogLevel.WARNING    -> "⚠"
    TraceLogLevel.ERROR      -> "❌"
    TraceLogLevel.FATAL      -> "\uD83D\uDC80"
}

/**
 * Logs a message at the specified trace log level.
 *
 * The message lambda is evaluated only if the log is printed.
 * A custom tag is generated automatically if not provided.
 *
 * @param level the level of the log.
 * @param tag an optional tag. Defaults to a generated tag.
 * @param message a lambda that returns the log message.
 */
inline fun TRACE(
    level: TraceLogLevel,
    tag: String = getTag(),
    message: () -> String
) {
    val decoratedMessage = "${emojiForLevel(level)} ${message()}"
    when (level) {
        TraceLogLevel.SYSTEMATIC -> Log.v(tag, decoratedMessage)
        TraceLogLevel.DETAIL1    -> Log.d(tag, decoratedMessage)
        TraceLogLevel.DETAIL2    -> Log.i(tag, decoratedMessage)
        TraceLogLevel.DETAIL3    -> Log.w(tag, decoratedMessage)
        TraceLogLevel.WARNING    -> Log.e(tag, decoratedMessage)
        TraceLogLevel.ERROR      -> Log.wtf(tag, decoratedMessage)
        TraceLogLevel.FATAL      -> {
            Log.wtf(tag, decoratedMessage)
            throw RuntimeException(decoratedMessage)
        }
    }
}
