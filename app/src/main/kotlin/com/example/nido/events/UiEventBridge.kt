package com.example.nido.events

import java.util.concurrent.atomic.AtomicReference

/**
 * UiEventBridge
 *
 * A tiny one-way bridge for NON-Compose / NON-reducer code (trace, network, services)
 * to ask the UI layer to show something (dialogs, snackbars, banners...).
 *
 * IMPORTANT:
 * - This bridge is NOT for GameEvent (gameplay). Those must go through the reducer.
 * - The listener is set/unset by the UI layer (e.g., MainActivity) and should forward
 *   to GameManager.setAppDialogEvent(...) or setGameDialogEvent(...).
 */
object UiEventBridge {
    private val listenerRef = AtomicReference<(Any) -> Unit>(null)

    /** UI layer registers/unregisters a single listener */
    fun setListener(listener: ((Any) -> Unit)?) {
        listenerRef.set(listener)
    }

    /** Any layer can emit a UI event (AppDialogEvent or GameDialogEvent). */
    fun emit(event: Any) {
        listenerRef.get()?.invoke(event)
    }
}

