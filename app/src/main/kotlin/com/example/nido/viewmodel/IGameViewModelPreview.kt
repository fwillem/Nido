package com.example.nido.game

import androidx.compose.runtime.State
import com.example.nido.data.SavedPlayer
import com.example.nido.data.model.Hand
import com.example.nido.game.GameState
import com.example.nido.utils.Debug
import kotlinx.coroutines.flow.StateFlow

interface IGameViewModelPreview {
    val gameState: StateFlow<GameState>
    val savedPlayers: StateFlow<List<SavedPlayer>>
    val savedPointLimit: StateFlow<Int>
    val savedDebug : StateFlow<Debug>

    // Methods that MainScreen needs to call
    fun updateGameState(newState: GameState)
    fun updatePlayerHand(playerIndex: Int, newHand: Hand)
    fun savePlayers(players: List<SavedPlayer>)
    fun savePointLimit(limit: Int)
    fun saveDebug (debug: Debug)

    // Add any other methods from GameViewModel that MainScreen directly calls
    // and that your FakeGameViewModelForPreview would need to implement.
    // For now, the above cover what's visible in your current GameViewModel.
}
