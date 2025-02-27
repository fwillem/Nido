package com.example.nido.game.multiplayer

import com.example.nido.data.model.Combination
import com.example.nido.data.model.Hand
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.game.GameManager

class RemotePlayer (
    override val id: String,
    override val name: String,
    override val avatar: String,
    override var score: Int = 0,
    override val hand: Hand = Hand()
) : Player{

    override val playerType: PlayerType = PlayerType.REMOTE

    override fun play(gameManager: GameManager): Combination? {
        // TODO: Implement remote player logic. This will likely involve
        // network communication and waiting for the remote player's move.
        // For now, return null.
        return null
    }
}