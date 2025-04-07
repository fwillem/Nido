package com.example.nido.game

import com.example.nido.data.model.PlayerType

// Top-level game phases
sealed class GamePhase {
    object Idle : GamePhase() // First state
    data class Round(val roundPhase: RoundPhase) : GamePhase() // Game Started, round in progress
    object GameOver : GamePhase() // Game over
}

// Phases within a round
sealed class RoundPhase {
    data class PlayerTurn(                      // Round started, Player's turn (either Local, Remote or AI
        val playerId: String,
        val turnInfo: TurnInfo
    ) : RoundPhase()
    object RoundOver : RoundPhase()             // End of round, ready to start a new round
}

// Sub-states for a player's turn
sealed class TurnState(val localOnly: Boolean = false) {
    object FirstTurn : TurnState()       // First turn of the round, player is forced to play one card
    object WaitingForSelection : TurnState(localOnly = true)    // Player is waiting to select cards (LOCAL ONLY)
    object Selecting : TurnState(localOnly = true)              // Actively selecting cards (LOCAL ONLY)
    object WaitForConfirmingMove : TurnState(localOnly = true)         // Ready to commit a move i.e. the selected cards form a valid combination(LOCAL ONLY)
    object NoValidMove : TurnState()            // No valid move available, will skip (after timeout)
    object NewTurn : TurnState()                // Special case: all others skipped thus the turn is re-started, player can play full hand or one card
    object AIProcessing : TurnState()           // AI is thinking (this is a transient state guarded by a timer for UX matter)
    object RemoteProcessing : TurnState()       // Remote player move pending
}

data class TurnInfo(
    val state: TurnState,
    val canSkip: Boolean = true,                // false = player must play, he cannot skip
    val canGoAllIn: Boolean = false,            // Use can play all its card if he does have a valid combination, whatever the number opf cards currently on the mat
    val displaySkip: Boolean = false,           // tells if skip button should be displayed
    val displayPlay: Boolean = false,           // tells if play button should be displayed
    val displaySkipCounter: Boolean = false,    // tells if the skip button with counter should be displayed
)

// Helper for debug/runtime checks
fun verifyStateForPlayer(playerType: PlayerType, state: TurnState) {
    if (playerType != PlayerType.LOCAL && state.localOnly) {
        throw IllegalStateException("State $state is LOCAL ONLY but current player is $playerType")
    }
}
