package com.example.nido.events

import com.example.nido.data.model.Card
import com.example.nido.data.model.Player

/**
 * GameDialogEvent
 *
 * Dialogs that only make sense during an active game.
 * These are consumed by MainScreen.
 */
sealed interface GameDialogEvent {

    /**
     * The round has ended.
     * @param winner The player who won the round.
     * @param playersHandScore List of (Player, handScore) at round end.
     */
    data class RoundOver(
        val winner: Player,
        val playersHandScore: List<Pair<Player, Int>>
    ) : GameDialogEvent

    /**
     * The whole game has ended, provide final rankings.
     * @param playerRankings List of (Player, totalScore) sorted by rank.
     */
    data class GameOver(
        val playerRankings: List<Pair<Player, Int>>
    ) : GameDialogEvent

    /**
     * Ask the local player to pick a card among candidates.
     * @param candidateCards Cards the UI may offer for selection.
     * @param selectedCards Current preselection (if any).
     * @param onConfirm Callback with the chosen card.
     * @param onCancel Callback when the user cancels.
     */
    data class CardSelection(
        val candidateCards: List<Card>,
        val selectedCards: List<Card>,
        val onConfirm: (Card) -> Unit,
        val onCancel: () -> Unit
    ) : GameDialogEvent

    /**
     * Confirm leaving the current match (not the whole app).
     */
    data object QuitGame : GameDialogEvent
}
