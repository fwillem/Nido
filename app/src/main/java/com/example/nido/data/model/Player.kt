package com.example.nido.data.model

import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand


enum class PlayerType { LOCAL, AI, REMOTE }



open class Player(
    val id: String, // Unique for remote players
    val name: String,
    val avatar: Int,
    val playerType: PlayerType,
    var score: Int = 0,       // 🔹 Added score property (mutable)
    val hand: Hand = Hand() // Each player has a hand of cards
) {
    open fun play(): Boolean {
        return false // Default implementation (handled in subclasses)
    }
}
