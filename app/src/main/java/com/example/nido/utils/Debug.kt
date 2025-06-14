package com.example.nido.utils

import kotlinx.serialization.Serializable

@Serializable
data class Debug (
    val displayAIsHands: Boolean = false,  // tells if the AI's hands should be displayed (debug mode value)
    val doNotAutoPlayerAI: Boolean = false   // When set to true , AIs  don't automatically plays (when timer expires),  a play button shall be displayed for the user to trigger AI play
)