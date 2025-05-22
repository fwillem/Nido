package com.example.nido.game

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.game.GameState
import com.example.nido.data.model.Hand

class GameViewModel : ViewModel() {

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
        if (oldState.soundOn != newState.soundOn) changes.add("soundOn: ${oldState.soundOn} ➝ ${newState.soundOn}")
        if (oldState.gameEvent != newState.gameEvent) changes.add("gameEvent: ${oldState.gameEvent} ➝ ${newState.gameEvent}")
        if (oldState.skipCount != newState.skipCount) changes.add("skipCount: ${oldState.skipCount} ➝ ${newState.skipCount}")
        if (oldState.pointLimit != newState.pointLimit) changes.add("pointLimit: ${oldState.pointLimit} ➝ ${newState.pointLimit}")
        if (oldState.turnId != newState.turnId) changes.add("turnId: ${oldState.turnId} ➝ ${newState.turnId}")
        if (oldState.turnInfo != newState.turnInfo) changes.add("turnInfo: ${oldState.turnInfo} ➝ ${newState.turnInfo}")
        if (oldState.playerId != newState.playerId) changes.add("playerId: ${oldState.playerId} ➝ ${newState.playerId}")






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
            turnInfo = newState.turnInfo,
            playerId = newState.playerId,
            pointLimit = newState.pointLimit,
            soundOn = newState.soundOn,
            gameEvent = newState.gameEvent,
            turnId = newState.turnId,
            )


    }

    fun updatePlayerHand(playerIndex: Int, newHand: Hand) {
        _gameState.value = _gameState.value.copy(
            players = _gameState.value.players.mapIndexed { index, player ->
                if (index == playerIndex) player.copy(hand = newHand) else player
            }
        )

        TRACE(DEBUG) { "\uD83D\uDD04  Updated Player($playerIndex) hand: ${newHand.cards}" }
    }



}
