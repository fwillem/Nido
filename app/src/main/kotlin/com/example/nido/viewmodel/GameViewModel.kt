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
import com.example.nido.utils.Debug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameViewModel(app: Application) : AndroidViewModel(app), IGameViewModelPreview {

    // Persistent State via DataStore
    private val context = app.applicationContext

    private val _savedPlayers = MutableStateFlow<List<SavedPlayer>>(emptyList())
    override val savedPlayers: StateFlow<List<SavedPlayer>> = _savedPlayers

    private val _savedPointLimit = MutableStateFlow(Constants.GAME_MAX_POINT_LIMIT)
    override val savedPointLimit: StateFlow<Int> = _savedPointLimit

    private val _savedDebug = MutableStateFlow(Debug())
    override val savedDebug: StateFlow<Debug> = _savedDebug

    // Désormais, le ViewModel expose simplement l'état du GameManager
    override val gameState = GameManager.gameState


    init {
        // Load DataStore values as soon as ViewModel is created!
        viewModelScope.launch {
            NidoPreferences.playersFlow(context).collect { playersOrNull ->
                if (playersOrNull == null) {
                    TRACE(INFO) { "No players found in DataStore. Using default players." }
                    _savedPlayers.value = listOf(
                        SavedPlayer(
                            Constants.DEFAULT_LOCAL_PLAYER_NAME,
                            Constants.DEFAULT_LOCAL_PLAYER_AVATAR, PlayerType.LOCAL
                        ),
                        SavedPlayer(
                            Constants.DEFAULT_SECOND_PLAYER_NAME,
                            Constants.DEFAULT_SECOND_PLAYER_AVATAR,
                            PlayerType.AI // Assuming the second player is AI by default
                        )
                    )
                } else {
                    _savedPlayers.value = playersOrNull
                }
            }
        }

        viewModelScope.launch {
            NidoPreferences.pointLimitFlow(context).collect { limitOrNull ->
                if (limitOrNull == null) {
                    TRACE(INFO) { "No point limit found in DataStore. Using default pointLimit = ${Constants.GAME_DEFAULT_POINT_LIMIT}." }
                    _savedPointLimit.value = Constants.GAME_DEFAULT_POINT_LIMIT
                } else {
                    TRACE(DEBUG) { "Loaded pointLimit from DataStore: $limitOrNull" }
                    _savedPointLimit.value = limitOrNull
                }
                TRACE(VERBOSE) { "Current savedPointLimit state: ${_savedPointLimit.value}" }
            }
        }

        viewModelScope.launch {
            NidoPreferences.debugFlow(context).collect { debugOrNull ->
                if (debugOrNull == null) {
                    TRACE(INFO) { "No debug found in DataStore. Using default debug = ${Debug()}." }
                    _savedDebug.value = Debug()
                } else {
                    TRACE(DEBUG) { "Loaded pointLimit from DataStore: $debugOrNull" }
                    _savedDebug.value = debugOrNull
                }
                TRACE(VERBOSE) { "Current savedDebug state: ${_savedDebug.value}" }
            }
        }


    }


    override fun savePlayers(players: List<SavedPlayer>) {
        _savedPlayers.value = players
        viewModelScope.launch {
            NidoPreferences.setPlayers(context, players)
            TRACE(DEBUG) { "💾 Saved players: $players" }
        }
    }

    override fun savePointLimit(limit: Int) {
        _savedPointLimit.value = limit
        viewModelScope.launch {
            NidoPreferences.setPointLimit(context, limit)
            TRACE(DEBUG) { "💾 Saved pointLimit: $limit" }
        }
    }

    override fun saveDebug(debug: Debug) {
        _savedDebug.value = debug

        viewModelScope.launch {
            NidoPreferences.setDebug(context, debug)
            TRACE(DEBUG) { "💾 Saved Debug: $debug" }
        }
    }



    override fun updateGameState(newState: GameState) {
        val oldState = gameState.value  // Get the previous state

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
        if (oldState.discardPile != newState.discardPile) changes.add("🗑 discardPile: ${oldState.discardPile} ➝ ${newState.discardPile}")
        if (oldState.startingPlayerIndex != newState.startingPlayerIndex) changes.add("🔢 startingPlayerIndex: ${oldState.startingPlayerIndex} ➝ ${newState.startingPlayerIndex}")
        if (oldState.currentPlayerIndex != newState.currentPlayerIndex) changes.add("🎯 currentPlayerIndex: ${oldState.currentPlayerIndex} ➝ ${newState.currentPlayerIndex}")
        if (oldState.currentPlayerId != newState.currentPlayerId) changes.add("🆔 currentPlayerId: ${oldState.currentPlayerId} ➝ ${newState.currentPlayerId}")
        if (oldState.currentCombinationOnMat != newState.currentCombinationOnMat) changes.add("🟨 currentCombinationOnMat: ${oldState.currentCombinationOnMat} ➝ ${newState.currentCombinationOnMat}")
        if (oldState.gameDialogEvent != newState.gameDialogEvent) changes.add("🎲 gameDialogEvent: ${oldState.gameDialogEvent} ➝ ${newState.gameDialogEvent}")
        if (oldState.appDialogEvent != newState.appDialogEvent) changes.add("🎲 appDialogEvent: ${oldState.appDialogEvent} ➝ ${newState.appDialogEvent}")
        if (oldState.skipCount != newState.skipCount) changes.add("⏭ skipCount: ${oldState.skipCount} ➝ ${newState.skipCount}")
        if (oldState.pointLimit != newState.pointLimit) changes.add("🎯 pointLimit: ${oldState.pointLimit} ➝ ${newState.pointLimit}")
        if (oldState.soundEffectVolume != newState.soundEffectVolume) changes.add("🔊 soundEffectVolume: ${oldState.soundEffectVolume} ➝ ${newState.soundEffectVolume}")
        if (oldState.soundMusicVolume != newState.soundMusicVolume) changes.add("🎵 soundMusicVolume: ${oldState.soundMusicVolume} ➝ ${newState.soundMusicVolume}")
        if (oldState.pendingSounds != newState.pendingSounds) changes.add("🔊 pendingSounds: ${oldState.pendingSounds} ➝ ${newState.pendingSounds}")
        if (oldState.pendingMusic != newState.pendingMusic) changes.add("🎵 pendingMusic: ${oldState.pendingMusic} ➝ ${newState.pendingMusic}")
        if (oldState.aiTimerDuration != newState.aiTimerDuration) changes.add("⏱ aiTimerDuration: ${oldState.aiTimerDuration} ➝ ${newState.aiTimerDuration}")

        if (oldState.turnId != newState.turnId) changes.add("🔄 turnId: ${oldState.turnId} ➝ ${newState.turnId}")
        if (oldState.turnHintMsg != newState.turnHintMsg) changes.add("💬 turnHintMsg: ${oldState.turnHintMsg} ➝ ${newState.turnHintMsg}")
        if (oldState.bannerMsg != newState.bannerMsg) changes.add("📢 bannerMsg: ${oldState.bannerMsg} ➝ ${newState.bannerMsg}")
        if (oldState.turnInfo != newState.turnInfo) changes.add("💡 turnInfo: ${oldState.turnInfo} ➝ ${newState.turnInfo}")

        // Print only changed values.
        if (changes.isNotEmpty()) {
            TRACE(VERBOSE) { "🔄 GameState changes:\n${changes.joinToString("\n")}" }
        } else {
            TRACE(VERBOSE) { "🔄 No changes in GameState." }
        }

        // Apply the new state.
        GameManager.updateGameState(
            players = if (newState.players.isNotEmpty()) newState.players else oldState.players,
            deck = if (newState.deck.isNotEmpty()) newState.deck else oldState.deck,
            discardPile = newState.discardPile,
            startingPlayerIndex = newState.startingPlayerIndex,
            currentPlayerIndex = newState.currentPlayerIndex,
            currentPlayerId = newState.currentPlayerId,
            currentCombinationOnMat = newState.currentCombinationOnMat,
            skipCount = newState.skipCount,
            turnInfo = newState.turnInfo,
            pointLimit = newState.pointLimit,
            soundEffectVolume = newState.soundEffectVolume,
            soundMusicVolume = newState.soundMusicVolume,
            pendingSounds = newState.pendingSounds,
            pendingMusic = newState.pendingMusic,
            appDialogEvent = newState.appDialogEvent,
            gameDialogEvent = newState.gameDialogEvent,
            turnId = newState.turnId,
        )
    }

    override fun updatePlayerHand(playerIndex: Int, newHand: Hand) {
        GameManager.updatePlayerHand(playerIndex, newHand)
        TRACE(DEBUG) { "🔃 Updated Player($playerIndex) hand: ${newHand.cards}" }
    }
}
