package com.example.nido.game

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nido.data.SavedPlayer
import com.example.nido.data.NidoPreferences
import com.example.nido.data.model.Hand
import com.example.nido.data.model.PlayerType
import com.example.nido.utils.Constants
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import kotlinx.coroutines.launch

class GameViewModel(app: Application) : AndroidViewModel(app), IGameViewModelPreview {

    // Persistent State via DataStore
    private val context = app.applicationContext

    private val _savedPlayersState = mutableStateOf<List<SavedPlayer>>(emptyList())
    override val savedPlayers: State<List<SavedPlayer>> = _savedPlayersState

    private val _savedPointLimitState = mutableStateOf(Constants.GAME_MAX_POINT_LIMIT)
    override val savedPointLimit: State<Int> = _savedPointLimitState

    private val _gameState = mutableStateOf(GameState())
    override val gameState: State<GameState> = _gameState



    init {
        // Load DataStore values as soon as ViewModel is created!
        viewModelScope.launch {
            NidoPreferences.playersFlow(context).collect { playersOrNull ->
                if (playersOrNull == null) {
                    TRACE(INFO) { "No players found in DataStore. Using default player: ${Constants.DEFAULT_LOCAL_PLAYER_NAME} ${Constants.DEFAULT_LOCAL_PLAYER_AVATAR} (LOCAL)." }
                    _savedPlayersState.value = listOf(SavedPlayer(Constants.DEFAULT_LOCAL_PLAYER_NAME, Constants.DEFAULT_LOCAL_PLAYER_AVATAR, PlayerType.LOCAL))
                } else {
                    TRACE(DEBUG) { "Loaded players from DataStore: $playersOrNull" }
                    _savedPlayersState.value = playersOrNull
                }
                TRACE(VERBOSE) { "Current savedPlayers state: ${_savedPlayersState.value}" }
            }
        }

        viewModelScope.launch {
            NidoPreferences.pointLimitFlow(context).collect { limitOrNull ->
                if (limitOrNull == null) {
                    TRACE(INFO) { "No point limit found in DataStore. Using default pointLimit = ${Constants.GAME_DEFAULT_POINT_LIMIT}." }
                    _savedPointLimitState.value = Constants.GAME_DEFAULT_POINT_LIMIT
                } else {
                    TRACE(DEBUG) { "Loaded pointLimit from DataStore: $limitOrNull" }
                    _savedPointLimitState.value = limitOrNull
                }
                TRACE(VERBOSE) { "Current savedPointLimit state: ${_savedPointLimitState.value}" }
            }
        }
    }


    override fun savePlayers(players: List<SavedPlayer>) {
        _savedPlayersState.value = players
        viewModelScope.launch {
            NidoPreferences.setPlayers(context, players)
            TRACE(DEBUG) { "💾 Saved players: $players" }
        }
    }

    override fun savePointLimit(limit: Int) {
        _savedPointLimitState.value = limit
        viewModelScope.launch {
            NidoPreferences.setPointLimit(context, limit)
            TRACE(DEBUG) { "💾 Saved pointLimit: $limit" }
        }
    }

    override fun updateGameState(newState: GameState) {
        val oldState = _gameState.value  // Get the previous state

        // Log the full new state.
        TRACE(VERBOSE) { "🔄 Updating GameState: $newState" }

        // Detect changes and log only those.
        val changes = mutableListOf<String>()

        // Compare players individually.
        oldState.players.zip(newState.players).forEach { (oldPlayer, newPlayer) ->
            val playerChanges = mutableListOf<String>()
            if (oldPlayer.hand != newPlayer.hand) playerChanges.add("🃏 hand: ${oldPlayer.hand} ➝ ${newPlayer.hand}")
            if (oldPlayer.score != newPlayer.score) playerChanges.add("🏆 score: ${oldPlayer.score} ➝ ${newPlayer.score}")
            if (oldPlayer.playerType != newPlayer.playerType) playerChanges.add("👤 playerType: ${oldPlayer.playerType} ➝ ${newPlayer.playerType}")

            if (playerChanges.isNotEmpty()) {
                changes.add("👤 Player '${oldPlayer.name}':\n  ${playerChanges.joinToString("\n  ")}")
            }
        }

        // Compare other fields in GameState.
        if (oldState.deck != newState.deck) changes.add("🎴 deck: ${oldState.deck} ➝ ${newState.deck}")
        if (oldState.selectedCards != newState.selectedCards) changes.add("🟩 selectedCards: ${oldState.selectedCards} ➝ ${newState.selectedCards}")
        if (oldState.discardPile != newState.discardPile) changes.add("🗑 discardPile: ${oldState.discardPile} ➝ ${newState.discardPile}")
        if (oldState.startingPlayerIndex != newState.startingPlayerIndex) changes.add("🔢 startingPlayerIndex: ${oldState.startingPlayerIndex} ➝ ${newState.startingPlayerIndex}")
        if (oldState.currentPlayerIndex != newState.currentPlayerIndex) changes.add("🎯 currentPlayerIndex: ${oldState.currentPlayerIndex} ➝ ${newState.currentPlayerIndex}")
        if (oldState.currentCombinationOnMat != newState.currentCombinationOnMat) changes.add("🟨 currentCombinationOnMat: ${oldState.currentCombinationOnMat} ➝ ${newState.currentCombinationOnMat}")
        if (oldState.soundOn != newState.soundOn) changes.add("🔊 soundOn: ${oldState.soundOn} ➝ ${newState.soundOn}")
        if (oldState.gameEvent != newState.gameEvent) changes.add("🎲 gameEvent: ${oldState.gameEvent} ➝ ${newState.gameEvent}")
        if (oldState.skipCount != newState.skipCount) changes.add("⏭ skipCount: ${oldState.skipCount} ➝ ${newState.skipCount}")
        if (oldState.pointLimit != newState.pointLimit) changes.add("🎯 pointLimit: ${oldState.pointLimit} ➝ ${newState.pointLimit}")
        if (oldState.turnId != newState.turnId) changes.add("🔄 turnId: ${oldState.turnId} ➝ ${newState.turnId}")
        if (oldState.turnInfo != newState.turnInfo) changes.add("💡 turnInfo: ${oldState.turnInfo} ➝ ${newState.turnInfo}")
        if (oldState.playerId != newState.playerId) changes.add("🆔 playerId: ${oldState.playerId} ➝ ${newState.playerId}")

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

    override fun updatePlayerHand(playerIndex: Int, newHand: Hand) {
        _gameState.value = _gameState.value.copy(
            players = _gameState.value.players.mapIndexed { index, player ->
                if (index == playerIndex) player.copy(hand = newHand) else player
            }
        )
        TRACE(DEBUG) { "🔃 Updated Player($playerIndex) hand: ${newHand.cards}" }
    }
}
