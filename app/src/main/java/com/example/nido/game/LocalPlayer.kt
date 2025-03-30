package com.example.nido.game


import com.example.nido.data.model.*
import com.example.nido.utils.Constants
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import java.util.UUID



class LocalPlayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val avatar: String,
    override var score: Int = Constants.GAME_DEFAULT_POINT_LIMIT,
    override val hand: Hand = Hand(),
    override val isLocallyManaged: Boolean = true
) : Player {
    override val playerType: PlayerType = PlayerType.LOCAL

    override fun play(gameManager: IGameManager): PlayerAction {
        // For LocalPlayer, the UI handles moves.
        // Return a default SKIP action
        TRACE(FATAL) { "LocalPlayer.play() should not be called!" }

        return PlayerAction(
            actionType = PlayerActionType.SKIP,
            comment = "Local move is handled by UI"
        )
    }

    override fun copy(
        id: String,
        name: String,
        avatar: String,
        score: Int,
        hand: Hand,
        isLocallyManaged: Boolean
    ): Player {
        return LocalPlayer(id, name, avatar, score, hand, isLocallyManaged)
    }

    override fun toString(): String {
        return "LocalPlayer(id='$id', name='$name', avatar='$avatar', playerType=$playerType, score=$score, hand=$hand, isLocallyManaged=$isLocallyManaged)"
    }
}
