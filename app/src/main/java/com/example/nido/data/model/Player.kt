package com.example.nido.data.model

import com.example.nido.game.GameManager
import com.example.nido.data.model.PlayerType
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Hand


interface Player {
    val id: String
    val name: String
    val avatar: String
    val playerType: PlayerType
    var score: Int
    val hand: Hand

    fun play(gameManager: GameManager): Combination?
}