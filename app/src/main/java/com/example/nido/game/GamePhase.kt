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

data class TurnInfo(
    val canSkip: Boolean = false,                // false = player must play, he cannot skip
    val canGoAllIn: Boolean = false,            // Use can play all its card if he does have a valid combination, whatever the number opf cards currently on the mat
    val displaySkip: Boolean = false,           // tells if skip button should be displayed
    val displayPlay: Boolean = false,           // tells if play button should be displayed
    val displaySkipCounter: Boolean = false,    // tells if the skip button with counter should be displayed
    val displayRemove: Boolean = false          // tells if the
)

