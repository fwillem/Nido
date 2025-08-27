// GameManager.kt (FULL)
package com.example.nido.game

import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.data.model.PlayerActionType
import com.example.nido.data.model.Hand
import com.example.nido.events.AppDialogEvent
import com.example.nido.events.GameDialogEvent
import com.example.nido.game.engine.GameEventDispatcher
import com.example.nido.game.multiplayer.MultiplayerMode
import com.example.nido.game.multiplayer.MultiplayerState
import com.example.nido.game.multiplayer.NetworkManager
import com.example.nido.game.multiplayer.RoomCoordinator
import com.example.nido.game.rules.GameRules
import com.example.nido.replay.GameRecorder
import com.example.nido.utils.Constants
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Single source of truth for game state and lightweight multiplayer orchestration.
 * UI talks only to GameManager via interfaces.
 */
private val isDispatching = AtomicBoolean(false)

object GameManager : IGameManager {

    // -------------------------------------------------------------------------
    // State exposure
    // -------------------------------------------------------------------------
    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    override fun initialize(viewModel: GameViewModel) { /* no-op for now */ }

    // -------------------------------------------------------------------------
    // Game lifecycle
    // -------------------------------------------------------------------------

    /**
     * Initialize a new game session (local state). This does not touch networking.
     * Lobby/listeners are driven by hostQuickRoom()/joinQuickRoom().
     */
    override fun startNewGame(
        selectedPlayers: List<Player>,
        selectedPointLimit: Int,
        doNotAutoPlayAI: Boolean,
        AITimerDuration: Int
    ) {
        TRACE(DEBUG) {
            "selectedPlayers=$selectedPlayers, pointLimit=$selectedPointLimit, doNotAutoPlayAI=$doNotAutoPlayAI, aiDelay=$AITimerDuration"
        }

        // Start a new local recording session (used by replay).
        val session = GameRecorder.startNewSession()

        // Random first player (simple for now).
        val startingPlayerIndex = (0 until selectedPlayers.size).random()

        // Apply scores for the new game.
        val initializedPlayers = GameRules.initializePlayerScores(selectedPlayers, selectedPointLimit)

        // Caution, we need to keep the value of multiplayerState if any.

        val initial = GameState(
            players = initializedPlayers,
            doNotAutoPlayAI = doNotAutoPlayAI,
            pointLimit = selectedPointLimit,
            startingPlayerIndex = startingPlayerIndex,
            currentPlayerIndex = startingPlayerIndex,
            aiTimerDuration = AITimerDuration,
            currentSession = session,
            multiplayerState = gameState.value.multiplayerState // !! keep existing multiplayer state if any
        )
        _gameState.value = initial
        TRACE(INFO) { "Initial GameState set." }

        // Now start the first round (shuffle & deal is handled by reducer/effects).
        startNewRound()
    }

    override fun startNewRound() {
        TRACE(DEBUG) { "startNewRound()" }
        dispatcher.enqueueEvent(GameEvent.NewRoundStarted)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun getCurrentPlayer(): Player =
        gameState.value.players[gameState.value.currentPlayerIndex]

    /** Firebase uid if signed in, else null. */
    private fun myFirebaseUidOrNull(): String? = Firebase.auth.currentUser?.uid

    /** Prefer the uid from the multiplayer lobby context; fallback to Firebase uid. */
    private fun myNetworkingUidOrNull(): String? =
        gameState.value.multiplayerState?.myUid ?: myFirebaseUidOrNull()

    /** Active game/lobby id from MultiplayerState. */
    private fun activeGameIdOrNull(): String? =
        gameState.value.multiplayerState?.currentGameId

    // -------------------------------------------------------------------------
    // Turn actions
    // -------------------------------------------------------------------------

    override fun skipTurn() {
        TRACE(DEBUG) { "skipTurn()" }
        dispatcher.enqueueEvent(GameEvent.PlayerSkipped)
    }

    override fun playCombination(selectedCards: List<Card>, cardToKeep: Card?) {
        if (selectedCards.isEmpty()) {
            TRACE(FATAL) { "No cards selected" }
            return
        }

        val event = GameEvent.CardPlayed(
            playerId = getCurrentPlayer().id,
            playedCards = selectedCards,
            cardKept = cardToKeep
        )
        dispatcher.enqueueEvent(event)
    }

    override fun getAIMove() {
        val aiPlayer = getCurrentPlayer()
        if (aiPlayer.playerType != PlayerType.AI) {
            TRACE(ERROR) { "Not AI's turn!" }
            return
        }

        TRACE(DEBUG) { "AI is playing (${aiPlayer.name})" }
        val action = aiPlayer.play(this)
        if (action.actionType == PlayerActionType.PLAY) {
            val combo = action.combination
            if (combo == null) {
                TRACE(FATAL) { "Combination cannot be null when actionType is PLAY for ${aiPlayer.name}" }
                return
            }
            TRACE(DEBUG) { "${aiPlayer.name} plays: $combo and keeps: ${action.cardToKeep}" }
            playCombination(combo.cards, action.cardToKeep)
        } else {
            TRACE(INFO) { "${aiPlayer.name} has no move" }
            skipTurn()
        }
    }

    override fun processSkip() {
        val current = getCurrentPlayer()
        if (current.playerType == PlayerType.AI) {
            TRACE(WARNING) { "AI should not skip through processSkip()" }
        } else {
            TRACE(INFO) { "Local player ${current.name} skips" }
            skipTurn()
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    override fun isValidMove(selectedCards: List<Card>): Boolean {
        val currentCombination = gameState.value.currentCombinationOnMat
        val selectedCombination = Combination(selectedCards.toMutableList())
        return GameRules.isValidMove(
            currentCombination,
            selectedCombination,
            getCurrentPlayer().hand.cards
        )
    }

    override fun isGameOver(): Boolean =
        GameRules.isGameOver(gameState.value.players, gameState.value.pointLimit)

    override fun getGameWinners(): List<Player> =
        GameRules.getGameWinners(gameState.value.players)

    override fun getPlayerRankings(): List<Pair<Player, Int>> =
        GameRules.getPlayerRankings(gameState.value.players)

    override fun getPlayerHandScores(): List<Pair<Player, Int>> =
        GameRules.getPlayerHandScores(gameState.value.players)

    override fun getCurrentPlayerHandSize(): Int =
        getCurrentPlayer().hand.cards.size

    override fun isCurrentPlayerLocal(): Boolean =
        getCurrentPlayer().playerType == PlayerType.LOCAL

    override fun currentPlayerHasValidCombination(): Boolean {
        val handCards = getCurrentPlayer().hand.cards
        val possibleMoves = GameRules.findValidCombinations(handCards)
        val playmatCombination = gameState.value.currentCombinationOnMat
        return possibleMoves.any { GameRules.isValidMove(playmatCombination, it, handCards) }
    }

    // -------------------------------------------------------------------------
    // Dialogs / notices / sfx
    // -------------------------------------------------------------------------

    override fun setAppDialogEvent(event: AppDialogEvent) {
        updateGameState(appDialogEvent = event)
    }

    override fun clearAppDialogEvent() {
        updateGameState(appDialogEvent = null)
    }

    override fun setGameDialogEvent(event: GameDialogEvent) {
        updateGameState(gameDialogEvent = event)
    }

    override fun clearGameDialogEvent() {
        updateGameState(gameDialogEvent = null)
    }

    private fun launchAITimer(turnId: Int, duration: Int) {
        // TODO: do not use GlobalScope in production; use a proper scope owned by GameManager.
        GlobalScope.launch {
            delay(duration.toLong())
            dispatcher.enqueueEvent(GameEvent.AITimerExpired(turnId))
        }
    }

    private fun enqueueNotice(notice: UiNotice) {
        val cur = gameState.value
        updateGameState(pendingNotices = cur.pendingNotices + notice)
    }

    private fun enqueueSound(effect: SoundEffect) {
        val cur = gameState.value
        updateGameState(pendingSounds = cur.pendingSounds + effect)
    }

    override fun consumeSound(effect: SoundEffect) {
        val cur = gameState.value
        updateGameState(pendingSounds = cur.pendingSounds - effect)
    }

    private fun enqueueMusic(cmd: MusicCommand) {
        val cur = gameState.value
        updateGameState(pendingMusic = cur.pendingMusic + cmd)
    }

    override fun consumeMusic(cmd: MusicCommand) {
        val cur = gameState.value
        updateGameState(pendingMusic = cur.pendingMusic - cmd)
    }

    override fun consumeNotice(notice: UiNotice) {
        val cur = gameState.value
        updateGameState(pendingNotices = cur.pendingNotices - notice)
    }

    // -------------------------------------------------------------------------
    // Networking: inbound handling
    // -------------------------------------------------------------------------

    private fun handleInbound(msg: NetworkManager.InboundMessage) {
        TRACE(WARNING) { "NET inbound: type=${msg.type} from=${msg.fromId} text=${msg.text}" }

        // If we don't know the peer yet, remember the sender as our known remote.
        gameState.value.multiplayerState?.let { ms ->
            if (ms.knownRemoteUid == null) {
                setMultiplayerState(ms.copy(knownRemoteUid = msg.fromId))
            }
        }

        val notice = UiNotice(
            message = when (msg.type) {
                "ping" -> "Ping received from ${msg.fromId}"
                "chat" -> "Chat from ${msg.fromId}: ${msg.text ?: ""}"
                else   -> "Message ${msg.type} from ${msg.fromId}"
            },
            kind = NoticeKind.Success
        )
        updateGameState(pendingNotices = gameState.value.pendingNotices + notice)
    }


    // -------------------------------------------------------------------------
    // Multiplayer lobby state
    // -------------------------------------------------------------------------

    override fun setMultiplayerState(state: MultiplayerState?) {
        updateGameState(multiplayerState = state)
    }

    override fun getMultiplayerState(): MultiplayerState? =
        gameState.value.multiplayerState

    // Host a new waiting room and connect the listener through RoomCoordinator.
    override fun hostQuickRoom(myUid: String) {
        RoomCoordinator.hostWaitingGame(
            ownerUid = myUid,
            onInboundMessage = ::handleInbound,
            onConnected = { res ->
                setMultiplayerState(
                    MultiplayerState(
                        mode = MultiplayerMode.HOST,
                        myUid = myUid,
                        currentGameId = res.gameId,
                        knownRemoteUid = null
                    )
                )
                enqueueNotice(UiNotice(message = "Hosting game ${res.gameId}", kind = NoticeKind.Info))
            },
            onError = { msg ->
                enqueueNotice(UiNotice(message = "Host failed: $msg", kind = NoticeKind.Error))
            }
        )
    }

    // Join the first waiting room and connect the listener through RoomCoordinator.
    override fun joinQuickRoom(myUid: String) {
        RoomCoordinator.joinFirstWaitingGame(
            myUid = myUid,
            onInboundMessage = ::handleInbound,
            onConnected = { res ->
                setMultiplayerState(
                    MultiplayerState(
                        mode = MultiplayerMode.JOINER,
                        myUid = myUid,
                        currentGameId = res.gameId,
                        knownRemoteUid = res.ownerId
                    )
                )
                enqueueNotice(UiNotice(message = "Joined game ${res.gameId}", kind = NoticeKind.Success))
            },
            onError = { msg ->
                enqueueNotice(UiNotice(message = "Join failed: $msg", kind = NoticeKind.Warning))
            }
        )
    }

    // -------------------------------------------------------------------------
    // Outbound messages
    // -------------------------------------------------------------------------

    override fun chatWithRemotePlayer(remotePlayerId: String, text: String) {
        val gameId = activeGameIdOrNull() ?: return
        val fromId = myNetworkingUidOrNull() ?: return
        NetworkManager.sendChat(
            gameId = gameId,
            fromId = fromId,
            toId = remotePlayerId,
            text = text
        )
        TRACE(INFO) { "Chat sent to $remotePlayerId: $text" }
    }

    private fun pingRemotePlayer(remotePlayerId: String) {
        val gameId = activeGameIdOrNull() ?: return
        val fromId = myNetworkingUidOrNull() ?: return
        NetworkManager.sendPing(
            gameId = gameId,
            fromId = fromId,
            toId = remotePlayerId
        )
        TRACE(INFO) { "Ping sent to $remotePlayerId" }
    }

    private fun defaultLoopbackPeerIdOrNull(): String? {
        val me = myNetworkingUidOrNull() ?: return null
        return when (me) {
            Constants.RemoteTestIds.ANONYMOUS_UID_REMOTE_PLAYER_XIAOMI ->
                Constants.RemoteTestIds.ANONYMOUS_UID_REMOTE_PLAYER_VD
            Constants.RemoteTestIds.ANONYMOUS_UID_REMOTE_PLAYER_VD ->
                Constants.RemoteTestIds.ANONYMOUS_UID_REMOTE_PLAYER_XIAOMI
            else -> gameState.value.multiplayerState?.knownRemoteUid
        }
    }



    override fun pingTestPeerIfPossible() {
        val target = defaultLoopbackPeerIdOrNull()
        if (target == null) {
            TRACE(WARNING) { "No default peer available; cannot ping." }
            return
        }
        pingRemotePlayer(target)
    }

    // Symmetric: first try to join a waiting room; if none, host one.
// Use this from MainActivity after Firebase sign-in.
    override fun autoQuickConnect(myUid: String) {
        RoomCoordinator.joinFirstWaitingGame(
            myUid = myUid,
            onInboundMessage = ::handleInbound,
            onConnected = { res ->
                setMultiplayerState(
                    MultiplayerState(
                        mode = MultiplayerMode.JOINER,
                        myUid = myUid,
                        currentGameId = res.gameId,
                        knownRemoteUid = res.ownerId // host uid learned from lobby
                    )
                )
                updateGameState(
                    pendingNotices = gameState.value.pendingNotices +
                            UiNotice(message = "Joined game ${res.gameId}", kind = NoticeKind.Success)
                )
                // (Optional) Immediately ping the host so the host learns our uid too.
                NetworkManager.sendPing(
                    gameId = res.gameId,
                    fromId = myUid,
                    toId = res.ownerId ?: return@joinFirstWaitingGame
                )
            },
            onError = { _ ->
                // Could not join → host a room
                RoomCoordinator.hostWaitingGame(
                    ownerUid = myUid,
                    onInboundMessage = ::handleInbound,
                    onConnected = { host ->
                        setMultiplayerState(
                            MultiplayerState(
                                mode = MultiplayerMode.HOST,
                                myUid = myUid,
                                currentGameId = host.gameId,
                                knownRemoteUid = null // will be learned on first inbound message
                            )
                        )
                        updateGameState(
                            pendingNotices = gameState.value.pendingNotices +
                                    UiNotice(message = "Hosting game ${host.gameId}", kind = NoticeKind.Info)
                        )
                    },
                    onError = { msg ->
                        updateGameState(
                            pendingNotices = gameState.value.pendingNotices +
                                    UiNotice(message = "Auto-connect failed: $msg", kind = NoticeKind.Error)
                        )
                    }
                )
            }
        )
    }


    // -------------------------------------------------------------------------
    // IGameManager: remaining required methods
    // -------------------------------------------------------------------------

    override fun updatePlayerHand(playerIndex: Int, hand: Hand) {
        val current = gameState.value
        if (playerIndex !in current.players.indices) {
            TRACE(ERROR) { "Invalid playerIndex: $playerIndex" }
            return
        }
        val updatedPlayers = current.players.mapIndexed { idx, p ->
            if (idx == playerIndex) p.copy(hand = hand) else p
        }
        updateGameState(players = updatedPlayers)
        TRACE(DEBUG) { "Updated Player($playerIndex) hand: ${hand.cards}" }
    }

    override fun hasPlayedAllRemainingCards(): Boolean {
        val currentPlayer = getCurrentPlayer()
        return currentPlayer.hand.cards.all { it.isSelected }
    }

    // -------------------------------------------------------------------------
    // State update helpers
    // -------------------------------------------------------------------------

    /** Replace whole state (used by reducer/event dispatcher). */
    fun updateGameState(newState: GameState) {
        _gameState.value = newState
    }

    /** Partial update helper (do not call from reducer/event logic). */
    fun updateGameState(
        players: List<Player> = gameState.value.players,
        deck: androidx.compose.runtime.snapshots.SnapshotStateList<Card> = gameState.value.deck,
        discardPile: androidx.compose.runtime.snapshots.SnapshotStateList<Card> = gameState.value.discardPile,
        startingPlayerIndex: Int = gameState.value.startingPlayerIndex,
        currentPlayerIndex: Int = gameState.value.currentPlayerIndex,
        currentPlayerId: String = gameState.value.currentPlayerId,
        multiplayerState: MultiplayerState? = gameState.value.multiplayerState,
        currentCombinationOnMat: Combination = gameState.value.currentCombinationOnMat,
        skipCount: Int = gameState.value.skipCount,
        turnInfo: TurnInfo = gameState.value.turnInfo,
        pointLimit: Int = gameState.value.pointLimit,
        aiTimerDuration: Int = gameState.value.aiTimerDuration,
        soundEffectVolume: SoundVolume = gameState.value.soundEffectVolume,
        soundMusicVolume: SoundVolume = gameState.value.soundMusicVolume,
        pendingSounds: List<SoundEffect> = gameState.value.pendingSounds,
        pendingMusic: List<MusicCommand> = gameState.value.pendingMusic,
        appDialogEvent: AppDialogEvent? = gameState.value.appDialogEvent,
        gameDialogEvent: GameDialogEvent? = gameState.value.gameDialogEvent,
        pendingNotices: List<UiNotice> = gameState.value.pendingNotices,
        turnId: Int = gameState.value.turnId,
        doNotAutoPlayAI: Boolean = gameState.value.doNotAutoPlayAI
    ) {
        _gameState.value = gameState.value.copy(
            players = players,
            deck = deck,
            discardPile = discardPile,
            startingPlayerIndex = startingPlayerIndex,
            currentPlayerIndex = currentPlayerIndex,
            currentPlayerId = currentPlayerId,
            multiplayerState = multiplayerState,
            currentCombinationOnMat = currentCombinationOnMat,
            skipCount = skipCount,
            turnInfo = turnInfo,
            pointLimit = pointLimit,
            aiTimerDuration = aiTimerDuration,
            appDialogEvent = appDialogEvent,
            soundEffectVolume = soundEffectVolume,
            soundMusicVolume = soundMusicVolume,
            pendingSounds = pendingSounds,
            pendingMusic = pendingMusic,
            gameDialogEvent = gameDialogEvent,
            pendingNotices = pendingNotices,
            turnId = turnId,
            turnHintMsg = gameState.value.turnHintMsg,
            bannerMsg = gameState.value.bannerMsg,
            doNotAutoPlayAI = doNotAutoPlayAI
        )
    }

    // -------------------------------------------------------------------------
    // Event dispatcher wiring
    // -------------------------------------------------------------------------

    private val dispatcher = GameEventDispatcher(
        getState = { gameState.value },
        updateState = { updateGameState(it) },
        handleSideEffect = ::handleSideEffect,
        reducer = ::gameReducer
    )

    private fun handleSideEffect(effect: GameSideEffect) {
        when (effect) {
            is GameSideEffect.StartAITimer -> launchAITimer(effect.turnId, this.gameState.value.aiTimerDuration)
            is GameSideEffect.ShowDialog -> setGameDialogEvent(effect.dialog)
            is GameSideEffect.GetAIMove -> getAIMove()
            is GameSideEffect.PlaySound -> enqueueSound(effect.effect)
            is GameSideEffect.PlayMusic -> enqueueMusic(MusicCommand.Play(effect.track, effect.loop))
            is GameSideEffect.StopMusic -> enqueueMusic(MusicCommand.Stop)
        }
    }
}

// Internal helper to expose GameManager as IGameManager.
internal fun getGameManagerInstance(): IGameManager = GameManager
