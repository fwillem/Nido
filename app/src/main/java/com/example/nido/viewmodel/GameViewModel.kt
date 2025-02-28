package com.example.nido.game

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    val gameManager: GameManager = GameManager // Access the GameManager object

    // Wrap GameState in MutableState for reactivity
    private val _gameState: MutableState<GameState> = mutableStateOf(GameState())
    val gameState: GameState
        get() = _gameState.value // Expose a read-only version

    // Function to update GameState (example)
    fun updateGameState(newState: GameState) {
        println("updateGameState players: ${newState.players}") // Debug log
        println("updateGameState deck: ${newState.deck.size}") // Debug log }") // Debug log

        _gameState.value = newState
    }

}