package com.example.nido.events

import com.example.nido.utils.TraceLogLevel

/**
 * AppDialogEvent
 *
 * Dialogs that are app-wide, regardless of the current screen.
 * These are consumed globally by NidoApp.
 */
sealed interface AppDialogEvent {

    /**
     * Confirm quitting the whole application.
     */
    data object QuitApp : AppDialogEvent

    /**
     * Show a fatal error screen (invoked by TRACE/UiEventBridge).
     * @param tag   Source tag for the error. (tries to provide name of the function or context where the error occurred)
     * @param message Lazy message supplier to avoid building strings unnecessarily.
     */
    data class BlueScreenOfDeath(
        val tag: String,
        val message: () -> String
    ) : AppDialogEvent
}
