package com.example.nido.data.model

import com.example.nido.data.model.Hand
import com.example.nido.data.model.Combination
import com.example.nido.game.GameContext


enum class PlayerType { LOCAL, AI, REMOTE }



abstract class Player(
    val id: String,
    val name: String,
    val avatar: Int,
    val playerType: PlayerType
) {
    var score: Int = 0
    val hand: Hand = Hand()  // Placed in the body to allow subclasses to override it

    abstract fun play(gameContext: GameContext): Combination?
}
