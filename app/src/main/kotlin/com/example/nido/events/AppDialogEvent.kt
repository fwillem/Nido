package com.example.nido.events

import com.example.nido.utils.TraceLogLevel

/**
 * App-wide dialogs (handled globally in NidoApp).
 * Examples: Quit the whole app, BSOD, connection lost, mandatory update, etc.
 */
sealed interface AppDialogEvent {
    data object QuitApp : AppDialogEvent

    data class BlueScreenOfDeath(
        val level: TraceLogLevel,
        val tag: String,
        val message: () -> String
    ) : AppDialogEvent
}

