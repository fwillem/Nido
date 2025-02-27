package com.example.nido.data.model

import com.example.nido.game.GameManager

interface Player {
    val id: Int
    val name: String
    val avatar: String
    val playerType: PlayerType
    var score: Int
    val hand: Hand

    fun play(gameManager: GameManager): Combination?
}