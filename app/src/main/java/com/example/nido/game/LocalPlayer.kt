package com.example.nido.data.model

import com.example.nido.game.GameManager

data class LocalPlayer(  //  data class
    override val id: Int,
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