package com.example.nido.game

import com.example.nido.data.model.Card
sealed class GameEvent() {
    object NewRoundStarted : GameEvent()
    data class CardPlayed(
        val playerId: String,
        val playedCards: List<Card>,
        val cardKept: Card? = null
    ) : GameEvent()
    object PlayerSkipped : GameEvent()

    object NextTurn : GameEvent()
    data class RoundOver(val localPlayerWon: Boolean) : GameEvent()
    data class GameOver(val localPlayerWon: Boolean) : GameEvent()
    data class AITimerExpired(val turnId: Int) : GameEvent()
    object QuitGame : GameEvent()

    // Network Events (Guest side)
    data class NetworkStateSync(
        val turnId: Int,
        val currentPlayerIndex: Int,
        val playersPublic: List<Any>, // Simplified for now, will map to proper structs
        val myHand: List<Card>?,
        val rawSnapshotId: String? = null // For trace/debug
    ) : GameEvent()
    
    // Add additional events as needed.
}