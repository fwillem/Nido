package com.example.nido.game

import androidx.compose.runtime.State

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    val gameManager: GameManager = GameManager // Access the GameManager object

    private val _gameState = mutableStateOf(GameState())
    val gameState: State<GameState> = _gameState




    // Function to update GameState (example)
    fun updateGameState(newState: GameState) {
        println("🔄 Updating GameState: ${newState.players}")

        _gameState.value = _gameState.value.copy(
            players = if (newState.players.isNotEmpty()) newState.players else _gameState.value.players,
            deck = if (newState.deck.isNotEmpty()) newState.deck else _gameState.value.deck,
            discardPile = newState.discardPile,
            currentPlayerIndex = newState.currentPlayerIndex,
            currentCombinationOnMat = newState.currentCombinationOnMat,
            screen = newState.screen,
            soundOn = newState.soundOn,
            showConfirmExitDialog = newState.showConfirmExitDialog
        )


        println("✅ GameState Updated Successfully: ${_gameState.value.players}")
    }


}