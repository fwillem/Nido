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
    object RoundOver : GameEvent()
    object GameOver : GameEvent()
    data class AITimerExpired(val turnId: Int) : GameEvent()
    object QuitGame : GameEvent()

    // Add additional events as needed.
}