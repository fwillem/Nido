package com.example.nido.utils

import android.util.Log
import com.example.nido.events.AppDialogEvent
import com.example.nido.events.UiEventBridge

/**
 * Log levels for TRACE.
 */
enum class TraceLogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    FATAL
}

/**
 * Coarse domain for Logcat filtering.
 * Used to build the Android Log tag, e.g. "Nido-NET", "Nido-LOBBY", etc.
 *
 * IMPORTANT:
 * - If no domain is provided to TRACE(), the tag remains exactly "Nido".
 */
enum class TraceDomain(val key: String) {
    NET("NET"),
    LOBBY("LOBBY"),
    GAME("GAME"),
    RED("RED"),
    UI("UI"),
    AI("AI"),
    DATA("DATA")
}

/** Maximum tag length for Android Studio Logcat. */
const val tagMaxLen = 23

/**
 * Default/global tag prefix.
 */
const val GLOBAL_TAG = "@Nido"

/**
 * Thread-local flag to detect when TRACE() is running.
 * This prevents the custom println() from printing duplicate output
 * when called within TRACE().
 */
val isInsideTrace = ThreadLocal.withInitial { false }

/**
 * Optional runtime override:
 * If true, even VERBOSE/DEBUG/INFO will include caller context (stack inspection).
 *
 * Default false for CPU efficiency.
 * Turn it on temporarily from anywhere (e.g. debug screen) without changing TRACE call sites.
 */
@Volatile
var FORCE_CALLER_FOR_LOW_LEVELS: Boolean = false

/** Returns an emoji based on the trace log level. */
fun emojiForLevel(level: TraceLogLevel): String = when (level) {
    /*
    TraceLogLevel.VERBOSE -> "Nido 🟡"
    TraceLogLevel.DEBUG -> "Nido 🟡"
    TraceLogLevel.INFO -> "Nido 🟡"
    TraceLogLevel.WARNING -> "Nido ⚠"
    TraceLogLevel.ERROR -> "Nido ❌"
    TraceLogLevel.FATAL -> "\uD83D\uDC80" // Skull emoji

     */
    else -> ""
}

/**
 * Attempts to generate a meaningful caller context by inspecting the current stack trace.
 * Uses the caller's class and method name (formatted as "ClassName:methodName").
 *
 * NOTE: This is CPU/alloc expensive, so TRACE() will only call it for WARNING+ by default.
 */
fun getTag(): String {
    val stackTrace = Throwable().stackTrace

    // Find the first frame that is outside this Trace utility.
    // This is more reliable than a fixed index (lambda/inline/coroutines shift frames).
    val caller = stackTrace.firstOrNull { e ->
        val cn = e.className
        // Exclude this file's top-level functions class and any utils trace frames
        cn != "com.example.nido.utils.TraceKt" &&
                !cn.startsWith("com.example.nido.utils.Trace") &&
                // Try to prefer real app frames
                cn.startsWith("com.example.nido.")
    } ?: stackTrace.getOrNull(1)

    val fullTag = caller
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
 * IMPORTANT:
 * TRACE() is a public inline function. Kotlin requires that any function it calls
 * is also public API (otherwise it could inline private/internal symbols into other call sites).
 */
fun buildLogcatTag(domain: TraceDomain?): String {
    // If domain is not provided, keep the original "Nido" tag (best noise gate).
    if (domain == null) return GLOBAL_TAG

    // Otherwise, append a stable suffix for filtering, e.g. "Nido-NET"
    return "$GLOBAL_TAG-${domain.key}".take(tagMaxLen)
}

/**
 * Logs a message at the specified trace log level.
 *
 * CPU optimization:
 * - For VERBOSE/DEBUG/INFO: NO stack inspection by default.
 * - For WARNING/ERROR/FATAL: caller context is included via stack inspection.
 * - You can force caller context for low levels using FORCE_CALLER_FOR_LOW_LEVELS=true
 *   or includeCaller=true at a specific call site.
 *
 * Compatibility:
 * - Existing calls like TRACE(level) { ... } keep working (domain defaults to null → tag "Nido").
 *
 * @param level the log level.
 * @param domain coarse domain to build Logcat tag (e.g. Nido-NET). If null, tag remains "Nido".
 * @param tag Context override (rare). If empty, TRACE may auto-generate it when caller info is enabled.
 * @param includeCaller if true, forces caller context (stack inspection) even for VERBOSE/DEBUG/INFO.
 * @param message a lambda that returns the log message.
 */
inline fun TRACE(
    level: TraceLogLevel,
    domain: TraceDomain? = null,
    tag: String = "",
    includeCaller: Boolean = false,
    message: () -> String
) {
    val logcatTag = buildLogcatTag(domain)

    // Decide whether we should include caller info (stack inspection).
    val needCaller: Boolean =
        includeCaller || FORCE_CALLER_FOR_LOW_LEVELS || (level >= TraceLogLevel.WARNING)

    // Compute caller context only if requested.
    val resolvedContext: String = when {
        tag.isNotEmpty() -> tag
        needCaller -> getTag()
        else -> ""
    }

    // Ensure ThreadLocal always resets even if message() throws.
    isInsideTrace.set(true)
    try {
        val content = message()

        // Avoid noisy "[]"
        val contextPrefix = if (resolvedContext.isNotEmpty()) "[$resolvedContext] " else ""
        val decoratedMessage = "${emojiForLevel(level)} $contextPrefix$content"
        val plainMessage = "$contextPrefix$content"

        when (level) {
            TraceLogLevel.VERBOSE -> Log.v(logcatTag, decoratedMessage)
            TraceLogLevel.DEBUG -> Log.d(logcatTag, decoratedMessage)
            TraceLogLevel.INFO -> Log.i(logcatTag, decoratedMessage)
            TraceLogLevel.WARNING -> Log.w(logcatTag, decoratedMessage)
            TraceLogLevel.ERROR -> Log.e(logcatTag, decoratedMessage)
            TraceLogLevel.FATAL -> {
                Log.wtf(logcatTag, decoratedMessage)
                UiEventBridge.emit(
                    AppDialogEvent.BlueScreenOfDeath(
                        tag = logcatTag,
                        message = { plainMessage }
                    )
                )
            }
        }
    } finally {
        isInsideTrace.set(false)
    }
}

/**
 * Custom println function that prevents duplicate logs when used inside TRACE().
 * If TRACE() is active (as determined by [isInsideTrace]), it returns the message
 * without printing; otherwise, it prints to the standard output.
 */
fun println(message: String): String {
    return if (isInsideTrace.get()) {
        message
    } else {
        message.also { kotlin.io.println(it) }
    }
}
