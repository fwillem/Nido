package com.example.nido.game

import com.example.nido.game.GameManager
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.data.model.Hand
import com.example.nido.data.model.Combination




data class LocalPlayer(  //  data class
    override val id: String,
    override val name: String,
    override val avatar: String,
    override var score: Int = 0,
    override val hand: Hand = Hand() // Provide default values
) : Player {
    override val playerType: PlayerType = PlayerType.LOCAL

    override fun play(gameManager: GameManager): Combination? {
        // Return null, UI handles LocalPlayer moves
        return null
    }
}