package com.example.nido.game

import com.example.nido.data.model.*

class LocalPlayer(
    override val id: String,
    override val name: String,
    override val avatar: String,
    override var score: Int = 0,
    override val hand: Hand = Hand()
) : Player {
    override val playerType: PlayerType = PlayerType.LOCAL

    override fun play(gameManager: GameManager): Combination? {
        return null // UI handles LocalPlayer moves
    }

    override fun copy(
        id: String,
        name: String,
        avatar: String,
        score: Int,
        hand: Hand
    ): Player {
        return LocalPlayer(id, name, avatar, score, hand)
    }
    override fun toString(): String {
        return "LocalPlayer(id='$id', name='$name', avatar='$avatar', playerType=$playerType, score=$score, hand=$hand)"
    }
}
