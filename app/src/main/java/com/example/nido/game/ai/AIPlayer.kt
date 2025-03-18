package com.example.nido.game.ai

import com.example.nido.game.rules.GameRules
import com.example.nido.data.model.*
import com.example.nido.game.IGameManager
import com.example.nido.utils.Constants

val AIPLayerComments = listOf("That's my choice", "What do you think about that", "I'm thinking about it", "This move will make me win")

class AIPlayer(
    override val id: String,
    override val name: String,
    override val avatar: String,
    override var score: Int = 0,
    override val hand: Hand = Hand()
) : Player {
    override val playerType: PlayerType = PlayerType.AI

    override fun play(gameManager: IGameManager): PlayerAction {
        // Find all possible valid combinations from the current hand.
        val possibleMoves: List<Combination> = GameRules.findValidCombinations(hand.cards)

        // Get the current combination on the playmat.
        val playmatCombination = gameManager.gameState.value.currentCombinationOnMat

        // Look for a valid move that beats the current playmat combination.
        // TODO Refine the heuristic
        val chosenCombination = possibleMoves.find { GameRules.isValidMove(playmatCombination, it) }

        return if (chosenCombination != null) {
            // For AI, choose the first card of the current playmat (if available) as the card to keep.
            // TODO Refine the heuristic
            val cardToKeep = if (playmatCombination.cards.isNotEmpty()) playmatCombination.cards.first() else null

            PlayerAction(
                actionType = PlayerActionType.PLAY,
                combination = chosenCombination,
                cardToKeep = cardToKeep,
                comment = AIPLayerComments.random() // Randomly pick a comment TODO Refine the comment that will be picked
            )
        } else {
            // No valid move found: the AI skips its turn.
            PlayerAction(
                actionType = PlayerActionType.SKIP,
                comment = "AI skips turn"
            )
        }
    }

    override fun copy(
        id: String,
        name: String,
        avatar: String,
        score: Int,
        hand: Hand
    ): Player {
        return AIPlayer(id, name, avatar, score, hand)
    }

    override fun toString(): String {
        return "AIPlayer(id='$id', name='$name', avatar='$avatar', playerType=$playerType, score=$score, hand=$hand)"
    }
}
