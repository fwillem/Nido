package com.example.nido.game

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    val gameManager: GameManager = GameManager // Access the GameManager object

    // Wrap GameState in MutableState for reactivity
    val gameState: State<GameState>
        get() = getViewModel().gameState  // ✅ Now correctly returns State<GameState>


    // Function to update GameState (example)
    fun updateGameState(newState: GameState) {
        println("🔄 Updating GameState: ${newState.players}")

        _gameState.value = _gameState.value.copy().apply {
            players = if (newState.players.isNotEmpty()) newState.players else players
            deck = if (newState.deck.isNotEmpty()) newState.deck else deck
            discardPile = newState.discardPile
            currentPlayerIndex = newState.currentPlayerIndex
            currentCombinationOnMat = newState.currentCombinationOnMat
            screen = newState.screen
            soundOn = newState.soundOn
            showConfirmExitDialog = newState.showConfirmExitDialog
        }

        println("✅ GameState Updated Successfully: ${_gameState.value.players}")
    }


}