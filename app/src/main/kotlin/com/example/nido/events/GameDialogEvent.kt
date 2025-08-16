package com.example.nido.events

import com.example.nido.data.model.Card
import com.example.nido.data.model.Player

/**
 * In-game dialogs (handled only while on MainScreen).
 */
sealed interface GameDialogEvent {
    data object QuitGame : GameDialogEvent

    data class CardSelection(
        val candidateCards: List<Card>,
        val selectedCards: List<Card>,
        val onConfirm: (Card) -> Unit,
        val onCancel: () -> Unit
    ) : GameDialogEvent

    data class RoundOver(
        val winner: Player,
        val playersHandScore: List<Pair<Player, Int>>
    ) : GameDialogEvent

    data class GameOver(
        val playerRankings: List<Pair<Player, Int>>
    ) : GameDialogEvent
}

