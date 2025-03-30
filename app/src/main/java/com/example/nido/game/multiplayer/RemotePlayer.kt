package com.example.nido.game.multiplayer

import com.example.nido.data.model.*
import com.example.nido.game.IGameManager
import com.example.nido.utils.Constants
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import java.util.UUID


class RemotePlayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val avatar: String,
    override var score: Int = Constants.GAME_DEFAULT_POINT_LIMIT,
    override val hand: Hand = Hand(),
    override val isLocallyManaged: Boolean = true
) : Player {
    override val playerType: PlayerType = PlayerType.REMOTE

    override fun play(gameManager: IGameManager): PlayerAction {
        // TODO need to implement it
        // Return a default SKIP action
        TRACE(ERROR) { "Not implemented yet" }

        return PlayerAction(
            actionType = PlayerActionType.SKIP,
            comment = "Remote move isn't supported yet !!!"
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
        return RemotePlayer(id, name, avatar, score, hand, isLocallyManaged)
    }

    override fun toString(): String {
        return "RemotePlayer(id='$id', name='$name', avatar='$avatar', playerType=$playerType, score=$score, hand=$hand, isLocallyManaged=$isLocallyManaged)"
    }
}
