package com.example.nido.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import com.example.nido.data.SavedPlayer
import com.example.nido.data.model.Hand
import com.example.nido.data.model.PlayerType
import com.example.nido.game.GameState
import com.example.nido.game.IGameViewModelPreview
import com.example.nido.utils.Constants.GAME_DEFAULT_POINT_LIMIT
import com.example.nido.utils.Debug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class FakeGameViewModelForPreview(
    initialGameState: GameState,
    initialSavedPlayers: List<SavedPlayer> = listOf(SavedPlayer("PreviewUser", "👤", PlayerType.LOCAL)),
    initialPointLimit: Int = GAME_DEFAULT_POINT_LIMIT,
    initialDebug: Debug = Debug()
) : IGameViewModelPreview {
    private val _gameState = MutableStateFlow(initialGameState)
    override val gameState: StateFlow<GameState> = _gameState

    private val _savedPlayers = MutableStateFlow(initialSavedPlayers)
    override val savedPlayers: StateFlow<List<SavedPlayer>> = _savedPlayers

    private val _savedPointLimit = MutableStateFlow(initialPointLimit)
    override val savedPointLimit: StateFlow<Int> = _savedPointLimit

    private val _savedDebug = MutableStateFlow(initialDebug)
    override val savedDebug: StateFlow<Debug> = _savedDebug


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
