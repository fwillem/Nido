package com.example.nido.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.nido.data.SavedPlayer
import com.example.nido.data.model.Hand
import com.example.nido.data.model.PlayerType
import com.example.nido.game.GameState
import com.example.nido.game.IGameViewModelPreview
import com.example.nido.utils.Constants.GAME_DEFAULT_POINT_LIMIT
import com.example.nido.utils.Debug


class FakeGameViewModelForPreview(
    initialGameState: GameState,
    initialSavedPlayers: List<SavedPlayer> = listOf(SavedPlayer("PreviewUser", "👤", PlayerType.LOCAL)),
    initialPointLimit: Int = GAME_DEFAULT_POINT_LIMIT,
    initialDebug: Debug = Debug()
) : IGameViewModelPreview { // <- implements interface
    private val _gameState = mutableStateOf(initialGameState)
    override val gameState: State<GameState> = _gameState

    private val _savedPlayers = mutableStateOf(initialSavedPlayers)
    override val savedPlayers: State<List<SavedPlayer>> = _savedPlayers

    private val _savedPointLimit = mutableStateOf(initialPointLimit)
    override val savedPointLimit: State<Int> = _savedPointLimit

    private val _savedDebug = mutableStateOf(initialDebug)
    override val savedDebug: State<Debug> = _savedDebug


    override fun updateGameState(newState: GameState) {
        _gameState.value = newState
    }

    override fun updatePlayerHand(playerIndex: Int, newHand: Hand) {
        // No-op for preview (implement if you want interaction in the preview)
    }

    override fun savePlayers(players: List<SavedPlayer>) {
        _savedPlayers.value = players
    }

    override fun savePointLimit(limit: Int) {
        _savedPointLimit.value = limit
    }

    override fun saveDebug(debug: Debug) {
        _savedDebug.value = debug
    }

}
