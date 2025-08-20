package com.example.nido.replay

import com.example.nido.game.GameEvent
import java.time.Instant
import java.util.UUID

object GameRecorder {

    private var currentSession: GameSession? = null

    fun startNewSession(sessionId: String = UUID.randomUUID().toString()): GameSession {
        val session = GameSession(sessionId = sessionId)
        currentSession = session
        return session
    }

    fun endSession() {
        currentSession?.let {
            it.endTimestamp = Instant.now().toEpochMilli()
        }
        currentSession = null
    }

    fun record(event: GameEvent, playerId: String? = null) {
        val session = currentSession ?: return
        val timestamp = Instant.now().toEpochMilli()

        val action: GameAction = when (event) {
            is GameEvent.CardPlayed -> GameAction.CardPlayedAction(
                timestamp = timestamp,
                playerId = event.playerId,
                playedCards = event.playedCards,
                cardKept = event.cardKept
            )
            is GameEvent.PlayerSkipped -> GameAction.SkippedAction(
                timestamp = timestamp,
                playerId = playerId ?: "unknown"
            )
            is GameEvent.NewRoundStarted -> GameAction.NewRoundAction(
                timestamp = timestamp,
                startingPlayerId = playerId ?: "unknown"
            )
            is GameEvent.RoundOver -> GameAction.RoundOverAction(
                timestamp = timestamp,
                winnerId = playerId ?: "unknown"
            )
            is GameEvent.GameOver -> GameAction.GameOverAction(
                timestamp = timestamp,
                winnerRanking = emptyList() // TODO: fill in reducer
            )
            else -> GameAction.GenericAction(
                timestamp = timestamp,
                description = event::class.simpleName ?: "UnknownEvent"
            )
        }

        session.actions.add(action)
    }
}
