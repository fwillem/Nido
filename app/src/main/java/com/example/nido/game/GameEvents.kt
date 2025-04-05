package com.example.nido.game.events

import com.example.nido.data.model.Card
import com.example.nido.game.TurnInfo

sealed class GameEvent(val timestamp: Long = System.currentTimeMillis()) {
    object GameStarted : GameEvent()
    object CardsDealt : GameEvent()
    data class NewRoundStarted(val startingPlayerId: String) : GameEvent()
    data class PlayerTurnStarted(val playerId: String, val turnInfo: TurnInfo) : GameEvent()
    data class CardPlayed(
        val playerId: String,
        val playedCards: List<Card>,
        val cardKept: Card? = null
    ) : GameEvent()
    data class PlayerSkipped(val playerId: String) : GameEvent()
    object RoundEnded : GameEvent()
    object GameEnded : GameEvent()
    // Add additional events as needed.
}