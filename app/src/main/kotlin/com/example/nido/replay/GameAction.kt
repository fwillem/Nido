package com.example.nido.replay

import com.example.nido.data.model.Card

/**
 * A sealed hierarchy of all possible game actions.
 * Each action has its own data payload + a timestamp.
 */
sealed class GameAction(
    open val timestamp: Long
) {

    data class CardPlayedAction(
        override val timestamp: Long,
        val playerId: String,
        val playedCards: List<Card>,
        val cardKept: Card? = null
    ) : GameAction(timestamp)

    data class SkippedAction(
        override val timestamp: Long,
        val playerId: String
    ) : GameAction(timestamp)

    data class NewRoundAction(
        override val timestamp: Long,
        val startingPlayerId: String
    ) : GameAction(timestamp)

    data class DealAction(
        override val timestamp: Long,
        val playerId: String,
        val dealtCards: List<Card>
    ) : GameAction(timestamp)

    data class RoundOverAction(
        override val timestamp: Long,
        val winnerId: String
    ) : GameAction(timestamp)

    data class GameOverAction(
        override val timestamp: Long,
        val winnerRanking: List<String>
    ) : GameAction(timestamp)

    // Fallback for things we haven’t modeled yet
    data class GenericAction(
        override val timestamp: Long,
        val description: String
    ) : GameAction(timestamp)
}
