package com.example.nido.events

import java.util.concurrent.atomic.AtomicReference

/**
 * UiEventBridge
 *
 * A tiny one-way bridge for NON-Compose / NON-reducer code (TRACE, network,
 * services, timers...) to request UI reactions (dialogs, snackbars, banners, etc.).
 *
 * Important:
 * - This bridge is NOT for GameEvent (gameplay). Those must go through the reducer/dispatcher.
 * - This bridge is ONLY for UI-facing events such as AppDialogEvent or GameDialogEvent.
 * - The listener is set/unset by the UI layer (MainActivity) and forwards
 *   to GameManager.setAppDialogEvent(...) or GameManager.setGameDialogEvent(...).
 */
object UiEventBridge {

    // Holds the current listener (set by MainActivity).
    private val listenerRef = AtomicReference<((Any) -> Unit)?>(null)

    /**
     * Register a listener (usually in MainActivity via DisposableEffect).
     * Pass null to unregister.
     */
    fun setListener(listener: ((Any) -> Unit)?) {
        listenerRef.set(listener)
    }

    /**
     * Emit a UI event from anywhere in the code (non-Compose, service, TRACE, etc.).
     * This will invoke the registered listener if any.
     */
    fun emit(event: Any) {
        listenerRef.get()?.invoke(event)
    }
}
