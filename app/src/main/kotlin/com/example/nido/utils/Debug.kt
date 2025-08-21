package com.example.nido.utils

import kotlinx.serialization.Serializable


@Serializable
data class Debug (
    val displayAIsHands: Boolean = false,  // (debug mode value) : tells if the AI's hands should be displayed
    val doNotAutoPlayerAI: Boolean = false,   // (debug mode value) : When set to true , AIs  don't automatically plays (when timer expires),  a play button shall be displayed for the user to trigger AI play
    val language: String = AppLanguage.ENGLISH.code,
    val aiTimerDuration : Int = 1200, // Duration of the AI thinking timer in milliseconds
)