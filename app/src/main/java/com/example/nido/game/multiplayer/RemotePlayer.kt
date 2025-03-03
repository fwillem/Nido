package com.example.nido.game.multiplayer

import com.example.nido.data.model.*
import com.example.nido.game.GameManager

class RemotePlayer(
    override val id: String,
    override val name: String,
    override val avatar: String,
    override var score: Int = 0,
    override val hand: Hand = Hand()
) : Player {
    override val playerType: PlayerType = PlayerType.REMOTE

    override fun play(gameManager: GameManager): Combination? {
        return null // Network-based play implementation will be added later
    }

    override fun copy(
        id: String,
        name: String,
        avatar: String,
        score: Int,
        hand: Hand
    ): Player {
        return RemotePlayer(id, name, avatar, score, hand)
    }

    override fun toString(): String {
        return "RemotePlayer(id='$id', name='$name', avatar='$avatar', playerType=$playerType, score=$score, hand=$hand)"
    }
}
