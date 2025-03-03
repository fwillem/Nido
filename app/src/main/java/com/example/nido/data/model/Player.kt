package com.example.nido.data.model

import com.example.nido.game.GameManager

interface Player {
    val id: String
    val name: String
    val avatar: String
    val playerType: PlayerType
    var score: Int
    val hand: Hand

    fun play(gameManager: GameManager): Combination?

    fun copy(
        id: String = this.id,
        name: String = this.name,
        avatar: String = this.avatar,
        score: Int = this.score,
        hand: Hand = this.hand
    ): Player



}
