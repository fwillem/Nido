package com.example.nido.game

import com.example.nido.data.model.PlayerType

// Top-level game phases
sealed class GamePhase {
    object Menu : GamePhase()
    object Setup : GamePhase()
    object Dealing : GamePhase()
    data class Round(val roundPhase: RoundPhase) : GamePhase()
    object GameOver : GamePhase()
}

// Phases within a round
sealed class RoundPhase {
    object RoundStart : RoundPhase()            // Round just started, before first move
    data class PlayerTurn(
        val playerId: String,
        val turnInfo: TurnInfo
    ) : RoundPhase()
    object RoundOver : RoundPhase()             // End of round
}

// Sub-states for a player's turn
sealed class TurnState(val localOnly: Boolean = false) {
    object WaitingForSelection : TurnState(localOnly = true)    // Player is waiting to select cards (LOCAL ONLY)
    object Selecting : TurnState(localOnly = true)              // Actively selecting cards (LOCAL ONLY)
    object ConfirmingMove : TurnState(localOnly = true)         // Ready to commit a move (LOCAL ONLY)
    object NoValidMove : TurnState()            // No valid move available, will skip (after timeout)
    object AllInOpportunity : TurnState()       // Special case: all others skipped, player must play full hand or one card
    object AIProcessing : TurnState()           // AI is thinking (UX delay)
    object RemoteProcessing : TurnState()       // Remote player move pending
}


data class TurnInfo(
    val state: TurnState,
    val canSkip: Boolean = true                // false = player must play
)

// Helper for debug/runtime checks
fun verifyStateForPlayer(playerType: PlayerType, state: TurnState) {
    if (playerType != PlayerType.LOCAL && state.localOnly) {
        throw IllegalStateException("State $state is LOCAL ONLY but current player is $playerType")
    }
}
