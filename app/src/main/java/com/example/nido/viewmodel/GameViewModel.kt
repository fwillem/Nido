package com.example.nido.game

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.game.GameState

class GameViewModel : ViewModel() {
    val gameManager: GameManager = GameManager // Access the GameManager object

    private val _gameState = mutableStateOf(GameState())
    val gameState: State<GameState> = _gameState

    fun updateGameState(newState: GameState) {
        val oldState = _gameState.value  // Get the previous state

        // Log the full new state.
        TRACE(VERBOSE) { "🔄 Updating GameState: $newState" }

        // Detect changes and log only those.
        val changes = mutableListOf<String>()

        // Compare players individually.
        oldState.players.zip(newState.players).forEach { (oldPlayer, newPlayer) ->
            val playerChanges = mutableListOf<String>()
            if (oldPlayer.hand != newPlayer.hand) playerChanges.add("hand: ${oldPlayer.hand} ➝ ${newPlayer.hand}")
            if (oldPlayer.score != newPlayer.score) playerChanges.add("score: ${oldPlayer.score} ➝ ${newPlayer.score}")
            if (oldPlayer.playerType != newPlayer.playerType) playerChanges.add("playerType: ${oldPlayer.playerType} ➝ ${newPlayer.playerType}")

            if (playerChanges.isNotEmpty()) {
                changes.add("Player '${oldPlayer.name}':\n  ${playerChanges.joinToString("\n  ")}")
            }
        }

        // Compare other fields in GameState.
        if (oldState.deck != newState.deck) changes.add("deck: ${oldState.deck} ➝ ${newState.deck}")
        if (oldState.selectedCards != newState.selectedCards) changes.add("selectedCards: ${oldState.selectedCards} ➝ ${newState.selectedCards}")
        if (oldState.discardPile != newState.discardPile) changes.add("discardPile: ${oldState.discardPile} ➝ ${newState.discardPile}")
        if (oldState.startingPlayerIndex != newState.startingPlayerIndex) changes.add("startingPlayerIndex: ${oldState.startingPlayerIndex} ➝ ${newState.startingPlayerIndex}")
        if (oldState.currentPlayerIndex != newState.currentPlayerIndex) changes.add("currentPlayerIndex: ${oldState.currentPlayerIndex} ➝ ${newState.currentPlayerIndex}")
        if (oldState.currentCombinationOnMat != newState.currentCombinationOnMat) changes.add("currentCombinationOnMat: ${oldState.currentCombinationOnMat} ➝ ${newState.currentCombinationOnMat}")
        if (oldState.screen != newState.screen) changes.add("screen: ${oldState.screen} ➝ ${newState.screen}")
        if (oldState.soundOn != newState.soundOn) changes.add("soundOn: ${oldState.soundOn} ➝ ${newState.soundOn}")
        if (oldState.showConfirmExitDialog != newState.showConfirmExitDialog) changes.add("showConfirmExitDialog: ${oldState.showConfirmExitDialog} ➝ ${newState.showConfirmExitDialog}")
        if (oldState.skipCount != newState.skipCount) changes.add("skipCount: ${oldState.skipCount} ➝ ${newState.skipCount}")

        // Print only changed values.
        if (changes.isNotEmpty()) {
            TRACE(DEBUG) { "🔄 GameState changes:\n${changes.joinToString("\n")}" }
        } else {
            TRACE(DEBUG) { "🔄 No changes in GameState." }
        }

        // Apply the new state.
        _gameState.value = _gameState.value.copy(
            players = if (newState.players.isNotEmpty()) newState.players else _gameState.value.players,
            deck = if (newState.deck.isNotEmpty()) newState.deck else _gameState.value.deck,
            selectedCards = newState.selectedCards,
            discardPile = newState.discardPile,
            startingPlayerIndex = newState.startingPlayerIndex,
            currentPlayerIndex = newState.currentPlayerIndex,
            currentCombinationOnMat = newState.currentCombinationOnMat,
            skipCount = newState.skipCount,
            screen = newState.screen,
            soundOn = newState.soundOn,
            showConfirmExitDialog = newState.showConfirmExitDialog
        )
    }
}
