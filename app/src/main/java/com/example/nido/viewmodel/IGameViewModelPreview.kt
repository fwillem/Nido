package com.example.nido.game

import androidx.compose.runtime.State
import com.example.nido.data.SavedPlayer
import com.example.nido.data.model.Hand
import com.example.nido.game.GameState

interface IGameViewModelPreview {
    val gameState: State<GameState>
    val savedPlayers: State<List<SavedPlayer>>
    val savedPointLimit: State<Int>

    // Methods that MainScreen needs to call
    fun updateGameState(newState: GameState) // Already in your GameViewModel
    fun updatePlayerHand(playerIndex: Int, newHand: Hand) // Already in your GameViewModel
    fun savePlayers(players: List<SavedPlayer>) // Already in your GameViewModel
    fun savePointLimit(limit: Int) // Already in your GameViewModel

    // Add any other methods from GameViewModel that MainScreen directly calls
    // and that your FakeGameViewModelForPreview would need to implement.
    // For now, the above cover what's visible in your current GameViewModel.
}
