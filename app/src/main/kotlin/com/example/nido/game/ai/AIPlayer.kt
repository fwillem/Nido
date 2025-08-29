package com.example.nido.game.ai

import com.example.nido.data.model.*
import com.example.nido.game.IGameManager
import com.example.nido.utils.Constants
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.DEBUG
import java.util.UUID

val AIPLayerComments = listOf(
    "That's my choice",
    "What do you think about that",
    "I'm thinking about it",
    "This move will make me win"
)

enum class AILevel {
    BEGINNER,
    ADVANCED
}

class AIPlayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val avatar: String,
    override var score: Int = Constants.GAME_DEFAULT_POINT_LIMIT,
    override val hand: Hand = Hand(),
    val level: AILevel = AILevel.ADVANCED
) : Player {
    override val playerType: PlayerType = PlayerType.AI

    override fun play(gameManager: IGameManager): PlayerAction {
        return makeDecision(gameManager)
    }

    override fun copy(
        id: String,
        name: String,
        avatar: String,
        score: Int,
        hand: Hand,
    ): Player {
        return AIPlayer(id, name, avatar, score, hand, level)
    }

    override fun toString(): String {
        return "AIPlayer(id='$id', name='$name', avatar='$avatar', playerType=$playerType, score=$score, hand=$hand, level=$level)"
    }
}

fun AIPlayer.makeDecision(gameManager: IGameManager): PlayerAction {
    val decision = AIHeuristicsEngine.evaluateMove(
        this.hand.cards,
        gameManager.gameState.value.currentCombinationOnMat,
        this.level
    )

    return if (decision.shouldSkip) {
        TRACE(DEBUG) { "[AI-$level] Decision: skip" }
        PlayerAction(
            actionType = PlayerActionType.SKIP,
            comment = "AI skips turn"
        )
    } else {
        TRACE(DEBUG) {
            "[AI-$level] Decision: play ${decision.combinationToPlay?.cards?.joinToString()} and keep ${decision.cardToKeep}"
        }
        PlayerAction(
            actionType = PlayerActionType.PLAY,
            combination = decision.combinationToPlay,
            cardToKeep = decision.cardToKeep,
            comment = AIPLayerComments.random()
        )
    }
}
