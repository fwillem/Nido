package com.example.nido.data.model
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Hand
import com.example.nido.game.GameManager

enum class PlayerType {
    LOCAL, AI, REMOTE
}

// Changed to an interface
interface Player {
    val id: Int
    val name: String
    val avatar: String
    val playerType: PlayerType
    var score: Int
    val hand: Hand // Keep Hand in the interface

    fun play(gameManager: GameManager): Combination? // Changed parameter to GameManager
}

