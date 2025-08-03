package com.example.nido.utils

import kotlinx.serialization.Serializable

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    FRENCH("fr", "Français"),
    ITALIAN("it", "Italiano"),
    GERMAN("de", "Deutsch"),
    PORTUGUESE("pt", "Português"),
    SPANISH("es", "Español");

    companion object {
        fun fromCode(code: String): AppLanguage = values().find { it.code == code } ?: ENGLISH
        fun codes() = values().map { it.code }
        fun all() = values().toList()
    }
}

@Serializable
data class Debug (
    val displayAIsHands: Boolean = false,  // (debug mode value) : tells if the AI's hands should be displayed
    val doNotAutoPlayerAI: Boolean = false,   // (debug mode value) : When set to true , AIs  don't automatically plays (when timer expires),  a play button shall be displayed for the user to trigger AI play
    val language: String = AppLanguage.ENGLISH.code
)