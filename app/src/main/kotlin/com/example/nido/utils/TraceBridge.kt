package com.example.nido.utils

import com.example.nido.events.DialogEvent
import java.util.concurrent.atomic.AtomicReference

/**
 * Pont non-composable pour relayer des DialogEvent depuis des couches non Compose (ex: TRACE())
 * vers la couche UI. La couche UI enregistre un listener et le désenregistre au bon moment.
 */
object TraceDialogBridge {
    private val listenerRef = AtomicReference<((DialogEvent) -> Unit)?>(null)

    fun setListener(listener: ((DialogEvent) -> Unit)?) {
        listenerRef.set(listener)
    }

    fun emit(event: DialogEvent) {
        listenerRef.get()?.invoke(event)
    }
}

