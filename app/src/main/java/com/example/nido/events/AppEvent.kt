package com.example.nido.events

import com.example.nido.data.model.Card
import com.example.nido.data.model.Player

sealed class AppEvent {
    // Game-related events.
    sealed class GameEvent : AppEvent() {
        data class RoundOver(
            val winner: Player,
            val oldScore: Int,
            val pointsAdded: Int,
            val newScore: Int,
            ) : GameEvent()

        data class GameOver(
            val playerRankings: List<Pair<Player, Int>>
        ) : GameEvent()

        data class CardSelection(
            val candidateCards: List<Card>,
            val selectedCards: List<Card>,
            val onConfirm: (Card) -> Unit,
            val onCancel: () -> Unit
        ) : GameEvent()
    }

    // Player-related events.
    sealed class PlayerEvent : AppEvent() {
        data class PlayerLeft(val player: Player) : PlayerEvent()
        data class ChatMessage(val sender: Player, val message: String) : PlayerEvent()
    }
}
