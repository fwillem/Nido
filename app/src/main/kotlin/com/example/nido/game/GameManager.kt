
package com.example.nido.game

import com.example.nido.data.model.Card
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.nido.replay.GameSession
import com.example.nido.game.BannerMsg
import com.example.nido.game.TurnHintMsg
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.data.model.PlayerActionType
import com.example.nido.data.model.Hand
import com.example.nido.events.AppDialogEvent
import com.example.nido.events.GameDialogEvent
import com.example.nido.game.ai.AIPlayer
import com.example.nido.game.engine.GameEventDispatcher
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_BANNER_MSG
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_CHAT
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_PING
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_STATE_SYNC
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_TURN_HINT
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_TURN_PLAY
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_TURN_SKIP
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_READY
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_START
import com.example.nido.game.multiplayer.MultiplayerMode
import com.example.nido.game.multiplayer.MultiplayerState
import com.example.nido.game.multiplayer.NetworkManager
import com.example.nido.game.multiplayer.RemotePlayer
import com.example.nido.game.multiplayer.RoomCoordinator
import com.example.nido.game.multiplayer.LobbyState
import com.example.nido.game.multiplayer.LobbyRole
import com.example.nido.game.multiplayer.LobbyEvent
import com.example.nido.game.multiplayer.LobbySideEffect
import com.example.nido.game.multiplayer.lobbyReducer
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


import org.json.JSONArray
import org.json.JSONObject


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

    private val dispatcher = GameEventDispatcher(
        getState = { _gameState.value },
        updateState = { 
            _gameState.value = it
            if (isHost()) broadcastAfterHostCommit() 
        },
        handleSideEffect = ::handleSideEffect,
        reducer = ::gameReducer
    )

    fun updateGameState(
        turnInfo: TurnInfo = gameState.value.turnInfo,
        doNotAutoPlayAI: Boolean = gameState.value.doNotAutoPlayAI,
        pointLimit: Int = gameState.value.pointLimit,
        aiTimerDuration: Int = gameState.value.aiTimerDuration,
        players: List<Player> = gameState.value.players,
        startingPlayerIndex: Int = gameState.value.startingPlayerIndex,
        currentPlayerIndex: Int = gameState.value.currentPlayerIndex,
        currentPlayerId: String = gameState.value.currentPlayerId,
        multiplayerState: MultiplayerState? = gameState.value.multiplayerState,
        currentCombinationOnMat: Combination = gameState.value.currentCombinationOnMat,
        discardPile: SnapshotStateList<Card> = gameState.value.discardPile,
        deck: SnapshotStateList<Card> = gameState.value.deck,
        skipCount: Int = gameState.value.skipCount,
        appDialogEvent: AppDialogEvent? = gameState.value.appDialogEvent,
        gameDialogEvent: GameDialogEvent? = gameState.value.gameDialogEvent,
        turnId: Int = gameState.value.turnId,
        turnHintMsg: TurnHintMsg? = gameState.value.turnHintMsg,
        bannerMsg: BannerMsg? = gameState.value.bannerMsg,
        pendingNotices: List<UiNotice> = gameState.value.pendingNotices,
        lastPlayerWhoPlayed: Player? = gameState.value.lastPlayerWhoPlayed,
        lastPlayerWhoSkipped: Player? = gameState.value.lastPlayerWhoSkipped,
        lastKeptCard: Card? = gameState.value.lastKeptCard,
        sessions: MutableList<GameSession> = gameState.value.sessions,
        currentSession: GameSession? = gameState.value.currentSession,
        soundEffectVolume: SoundVolume = gameState.value.soundEffectVolume,
        soundMusicVolume: SoundVolume = gameState.value.soundMusicVolume,
        pendingSounds: List<SoundEffect> = gameState.value.pendingSounds,
        pendingMusic: List<MusicCommand> = gameState.value.pendingMusic
    ) {
        val newState = GameState(
            turnInfo = turnInfo,
            doNotAutoPlayAI = doNotAutoPlayAI,
            pointLimit = pointLimit,
            aiTimerDuration = aiTimerDuration,
            players = players,
            startingPlayerIndex = startingPlayerIndex,
            currentPlayerIndex = currentPlayerIndex,
            currentPlayerId = currentPlayerId,
            multiplayerState = multiplayerState,
            currentCombinationOnMat = currentCombinationOnMat,
            discardPile = discardPile,
            deck = deck,
            skipCount = skipCount,
            appDialogEvent = appDialogEvent,
            gameDialogEvent = gameDialogEvent,
            turnId = turnId,
            turnHintMsg = turnHintMsg,
            bannerMsg = bannerMsg,
            pendingNotices = pendingNotices,
            lastPlayerWhoPlayed = lastPlayerWhoPlayed,
            lastPlayerWhoSkipped = lastPlayerWhoSkipped,
            lastKeptCard = lastKeptCard,
            sessions = sessions,
            currentSession = currentSession,
            soundEffectVolume = soundEffectVolume,
            soundMusicVolume = soundMusicVolume,
            pendingSounds = pendingSounds,
            pendingMusic = pendingMusic
        )
        _gameState.value = newState
    }

    // --- Lobby Reducer State ---
    private val _lobbyState = MutableStateFlow(LobbyState())
    override val lobbyState: StateFlow<LobbyState> = _lobbyState.asStateFlow()

    private fun dispatchLobbyEvent(event: LobbyEvent) {
        // Reducer is synchronous
        val result = lobbyReducer(_lobbyState.value, event)
        _lobbyState.value = result.newState
        result.sideEffects.forEach(::handleLobbySideEffect)
    }

    private fun handleLobbySideEffect(effect: LobbySideEffect) {
        when (effect) {
            is LobbySideEffect.ScanForRooms -> {
                RoomCoordinator.queryRecentRooms(
                    maxAgeMillis = 5 * 60 * 1000L,
                    maxRooms = 8,
                    onResult = { rooms ->
                        val candidate = rooms.firstOrNull { it.ownerId != effect.myUid }
                        if (candidate != null) {
                            dispatchLobbyEvent(LobbyEvent.RoomFound(candidate.id, candidate.ownerId ?: ""))
                        } else {
                            dispatchLobbyEvent(LobbyEvent.HostGame) // Fallback to host
                        }
                    },
                    onError = {
                        dispatchLobbyEvent(LobbyEvent.HostGame) // Fallback to host
                    }
                )
            }
            is LobbySideEffect.CreateRoom -> {
                RoomCoordinator.hostWaitingGame(
                    ownerUid = effect.myUid,
                    onInboundMessage = ::handleInbound,
                    onConnected = { res ->
                        // Sync legacy MultiplayerState so isHost() works
                        setMultiplayerState(
                            MultiplayerState(
                                mode = MultiplayerMode.HOST,
                                myUid = effect.myUid,
                                currentGameId = res.gameId,
                                knownRemoteUid = null,
                                localReady = true
                            )
                        )
                        dispatchLobbyEvent(LobbyEvent.RoomCreated(res.gameId))
                        cleanupMyRoomsAfterConnect(effect.myUid, res.gameId)
                    },
                    onError = { dispatchLobbyEvent(LobbyEvent.ErrorOccurred("Host failed: $it")) }
                )
            }
            is LobbySideEffect.JoinInGame -> {
                RoomCoordinator.joinSpecificGame(
                    gameId = effect.gameId,
                    myUid = effect.myUid,
                    onInboundMessage = ::handleInbound,
                    onConnected = { res ->
                         // Sync legacy MultiplayerState so isGuest() works
                         setMultiplayerState(
                            MultiplayerState(
                                mode = MultiplayerMode.JOINER,
                                myUid = effect.myUid,
                                currentGameId = res.gameId,
                                knownRemoteUid = res.ownerId,
                                localReady = true
                            )
                        )
                         // Joined successfully
                         cleanupMyRoomsAfterConnect(effect.myUid, res.gameId)
                         // NOW it is safe to send the Ready packet
                         dispatchLobbyEvent(LobbyEvent.SendReady)
                    },
                    onError = { dispatchLobbyEvent(LobbyEvent.ErrorOccurred("Join failed: $it")) }
                )
            }
            is LobbySideEffect.SendReadyPacket -> {
                NetworkManager.sendReady(effect.gameId, effect.myUid, effect.toUid)
            }
            is LobbySideEffect.NotifyGameStart -> {
                // Bridge to Game Logic
                maybeStartMultiplayerGame(effect.gameId, effect.isHost, effect.remoteUid)
            }
        }
    }

    override fun initialize(viewModel: GameViewModel) { /* no-op for now */
    }

    // -------------------------------------------------------------------------
    // Multiplayer Game Setup
    // -------------------------------------------------------------------------
// --- Handshake “start pressed” ---


    private data class MpSetup(
        val localName: String,
        val aiCount: Int,
        val pointLimit: Int,
        val doNotAutoPlayAI: Boolean,
        val aiTimerMs: Int
    )

    private var localSetup: MpSetup? = null

    private fun ensureRemoteSeatAdded(remoteUid: String, remoteName: String = "Guest"): Boolean {
        val s = gameState.value
        if (s.players.any { it.playerType == PlayerType.REMOTE }) return false
        val remote = RemotePlayer(id = remoteUid, name = remoteName, avatar = "🌍")
        updateGameState(players = s.players + remote)
        TRACE(INFO) { "Remote seat added: $remoteUid" }
        return true
    }

    private fun maybeSendReady() {
        val ms = gameState.value.multiplayerState ?: run {
            TRACE(WARNING) { "READY -> not sending: multiplayerState=null" } // 🟠
            return
        }
        if (ms.currentGameId == null || ms.knownRemoteUid == null) {
            TRACE(WARNING) { "READY -> not sending: missing gameId/peer; state=$ms" } // 🟠
            return
        }
        if (!ms.localReady) {
            TRACE(INFO) { "READY -> not sending: localReady=false" } // 🟡
            return
        }
        sendReadyNow("localReady=true") // 🟢
    }



    // Real start of the game when te two sides are ready
    private fun maybeStartMultiplayerGame(gameId: String, isHost: Boolean, remoteUid: String) {
        if (!isHost) {
            // Guest just waits for START
            return 
        }

        ensureRemoteSeatAdded(remoteUid, "Guest")
        
        // ... (Keep existing setup logic) ...
        val setup = localSetup ?: MpSetup(
            localName = "You",
            aiCount = 0,
            pointLimit = gameState.value.pointLimit.takeIf { it > 0 } ?: 15,
            doNotAutoPlayAI = gameState.value.doNotAutoPlayAI,
            aiTimerMs = gameState.value.aiTimerDuration.takeIf { it > 0 } ?: 1800
        )
        
        // Build players list
        // We recreate MultiplayerState on the fly here to satisfy buildPlayersForHost or refactor it
        val tempMs = MultiplayerState(
            myUid = _lobbyState.value.myUid,
            currentGameId = gameId,
            mode = MultiplayerMode.HOST,
            knownRemoteUid = remoteUid
        )
        
        val players = buildPlayersForHost(tempMs, setup)

        updateGameState(
            players = players,
            pointLimit = setup.pointLimit,
            doNotAutoPlayAI = setup.doNotAutoPlayAI,
            aiTimerDuration = setup.aiTimerMs,
            multiplayerState = tempMs // Legacy sync
        )

        startNewGame(
            selectedPlayers = players,
            selectedPointLimit = setup.pointLimit,
            doNotAutoPlayAI = setup.doNotAutoPlayAI,
            AITimerDuration = setup.aiTimerMs
        )

         // Prepare and send START + initial STATE_SYNC (order important: SYNC first!)
        val startPayload = GameSerializer.StartPayload(
            players = gameState.value.players.map { p ->
                GameSerializer.StartPlayer(
                    id = p.id,
                    name = p.name,
                    type = p.playerType,
                    score = p.score,
                    avatar = p.avatar
                )
            },
            pointLimit = gameState.value.pointLimit,
            startingPlayerIndex = gameState.value.startingPlayerIndex,
            currentPlayerIndex = gameState.value.currentPlayerIndex,
            turnId = gameState.value.turnId
        )

        TRACE(INFO) { "Multiplayer game starting... Sending Initial SNAPSHOT first." }

        // 1) Authoritative snapshot (MUST be first so Guest has data before Navigate)
        val snapshot = buildClientSnapshotFor(remoteUid)
        NetworkManager.sendStateSync(gameId, _lobbyState.value.myUid, remoteUid, encodeSnapshot(snapshot))

        TRACE(INFO) { "Snapshot sent. Now sending START command." }

        // 2) Start command (Triggers navigation on Guest)
        NetworkManager.sendStart(
            gameId = gameId,
            fromId = _lobbyState.value.myUid,
            toId = remoteUid,
            text = encodeStart(startPayload)
        )
        
        // Critical: Update UI state so NidoApp navigates to Game Screen
        gameState.value.multiplayerState?.let { ms ->
            setMultiplayerState(ms.copy(gameLaunched = true))
        }

        dispatchLobbyEvent(LobbyEvent.GameLaunched)
    }



    fun onStartMultiplayerPressed(
        localName: String,
        aiCount: Int,
        pointLimit: Int,
        doNotAutoPlayAI: Boolean,
        aiTimerMs: Int,
        myUid: String
    ) {
        localSetup = MpSetup(localName, aiCount, pointLimit, doNotAutoPlayAI, aiTimerMs)
        
        // Initialize Lobby State
        dispatchLobbyEvent(LobbyEvent.Initialize(myUid))
        
        // Start Scan (Guest first logic)
        dispatchLobbyEvent(LobbyEvent.StartScan)
    }

    private fun sendReadyNow(reason: String) {
        val ms = gameState.value.multiplayerState ?: run {
            TRACE(WARNING) { "READY -> skip ($reason): multiplayerState=null" } // 🟠
            return
        }
        val gameId = ms.currentGameId ?: run {
            TRACE(WARNING) { "READY -> skip ($reason): gameId=null" } // 🟠
            return
        }
        val toId = ms.knownRemoteUid ?: run {
            TRACE(WARNING) { "READY -> skip ($reason): knownRemoteUid=null" } // 🟠
            return
        }

        TRACE(WARNING) {
            "READY -> send ($reason) gameId=$gameId to=$toId myUid=${ms.myUid} localReady=${ms.localReady} remoteReady=${ms.remoteReady}"
        } // 🟠

        NetworkManager.sendReady(gameId, ms.myUid, toId)
    }


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
        val initializedPlayers =
            GameRules.initializePlayerScores(selectedPlayers, selectedPointLimit)

        // Keep existing multiplayer state if any
        val initial = GameState(
            players = initializedPlayers,
            doNotAutoPlayAI = doNotAutoPlayAI,
            pointLimit = selectedPointLimit,
            startingPlayerIndex = startingPlayerIndex,
            currentPlayerIndex = startingPlayerIndex,
            aiTimerDuration = AITimerDuration,
            currentSession = session,
            multiplayerState = gameState.value.multiplayerState
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

    // --- add inside GameManager object ---
    private fun cleanupMyRoomsAfterConnect(myUid: String, keepGameId: String) {
        RoomCoordinator.deleteOwnedRooms(
            ownerUid = myUid,
            excludeGameId = keepGameId,
            onCompleted = { count ->
                TRACE(WARNING) { "ROOM_CLEAN: done owner=$myUid kept=$keepGameId deleted=$count" }
            }
        )
    }


// -------------------------------------------------------------------------
// Turn actions
// -------------------------------------------------------------------------

    override fun skipTurn() {
        TRACE(DEBUG) { "skipTurn()" }

        if (authorityMode() == AuthorityMode.MIRROR) {
            val multiplayerState = gameState.value.multiplayerState ?: return
            val gameId = multiplayerState.currentGameId ?: return
            val hostUid = multiplayerState.knownRemoteUid ?: return

            NetworkManager.sendTurnSkip(gameId, multiplayerState.myUid, hostUid)
            return
        }

        // Host path: unchanged
        dispatcher.enqueueEvent(GameEvent.PlayerSkipped)
    }


    override fun playCombination(selectedCards: List<Card>, cardToKeep: Card?) {
        if (selectedCards.isEmpty()) {
            TRACE(FATAL) { "No cards selected" }
            return
        }

        if (authorityMode() == AuthorityMode.MIRROR) {
            val multiplayerState = gameState.value.multiplayerState ?: return
            val gameId = multiplayerState.currentGameId ?: return
            val hostUid = multiplayerState.knownRemoteUid ?: return

            val json = encodeTurnPlay(
                GameSerializer.TurnPlayIntent(
                    selectedCards = selectedCards,
                    cardToKeep = cardToKeep
                )
            )
            NetworkManager.sendTurnPlay(gameId, multiplayerState.myUid, hostUid, json)
            return
        }

        // Host path: keep your existing reducer flow
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

        var notice: UiNotice? = null
        
        // 1. Peer Discovery always notified to Reducer
         dispatchLobbyEvent(LobbyEvent.PeerJoined(msg.fromId))

        when (msg.type) {
            MSG_TYPE_TURN_PLAY -> if (isHost()) {
                val text = msg.text ?: return
                try {
                    // Use GameSerializer to decode
                    val intent = decodeTurnPlay(text)
                    
                    // Dispatch to Game Engine
                    dispatcher.enqueueEvent(
                        GameEvent.CardPlayed(
                            playerId = msg.fromId,
                            playedCards = intent.selectedCards,
                            cardKept = intent.cardToKeep
                        )
                    )
                } catch (e: Exception) {
                    TRACE(ERROR) { "Failed to decode turn_play: ${e.message}" }
                }
            }

            MSG_TYPE_TURN_SKIP -> if (isHost()) {
                 // dispatch SKIP event to GameReducer
                dispatcher.enqueueEvent(GameEvent.PlayerSkipped)
            }

            MSG_TYPE_STATE_SYNC -> if (isGuest() || _lobbyState.value.role == LobbyRole.GUEST) {
                // dispatch SYNC event to GameReducer (Fix for invisible hand)
                 val text = msg.text ?: return
                 // Deserialize with strict GameSerializer
                 try {
                     val snapshot = decodeSnapshot(text)
                     dispatcher.enqueueEvent(GameEvent.NetworkStateSync(snapshot))
                 } catch (e: Exception) {
                     TRACE(ERROR) { "SNAPSHOT decode failed: ${e.message}" }
                 }
            }

            MSG_TYPE_TURN_HINT -> {
                val text = msg.text ?: return
                decodeTurnHint(text)?.let { hint ->
                    dispatcher.enqueueEvent(GameEvent.TurnHintReceived(hint))
                }
            }

            MSG_TYPE_BANNER_MSG -> {
                 val text = msg.text ?: return
                 decodeBanner(text)?.let { banner ->
                     dispatcher.enqueueEvent(GameEvent.BannerReceived(banner))
                 }
            }
            // ... (rest of messages)
            
            MSG_TYPE_READY -> {
                dispatchLobbyEvent(LobbyEvent.RemoteReady)
            }
            
            // ... (Other Lobby Messages)
             MSG_TYPE_START -> if (isGuest() || _lobbyState.value.role == LobbyRole.GUEST) {
                val text = msg.text ?: return
                try {
                    val payload = decodeStart(text)
                    applyStartFromHost(payload, msg.fromId)
                    dispatchLobbyEvent(LobbyEvent.GameLaunched)
                } catch (e: Exception) {
                    TRACE(ERROR) { "START decode/apply failed: ${e.message}" }
                }
            }
        }
    }


// -------------------------------------------------------------------------
// Multiplayer lobby state
// -------------------------------------------------------------------------

    override fun setMultiplayerState(state: MultiplayerState?) {
        updateGameState(multiplayerState = state)
    }

    override fun getMultiplayerState(): MultiplayerState? =
        gameState.value.multiplayerState

    // Host a new waiting room using the new Lobby System
    override fun hostQuickRoom(myUid: String) {
        dispatchLobbyEvent(LobbyEvent.Initialize(myUid))
        dispatchLobbyEvent(LobbyEvent.HostGame)
    }

    // Join the first waiting room using the new Lobby System
    override fun joinQuickRoom(myUid: String) {
        dispatchLobbyEvent(LobbyEvent.Initialize(myUid))
        dispatchLobbyEvent(LobbyEvent.StartScan)
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
// --- replace the existing autoQuickConnect(myUid: String) ---
    override fun autoQuickConnect(myUid: String) {
        val windowMillis = 5 * 60 * 1000L  // 5 minutes
        TRACE(WARNING) { "ROOM_BOOT: start uid=$myUid window=${windowMillis / 1000}s" }

        // 1) Scan recent rooms (≤ 5 min), then pick the first not owned by me.
        RoomCoordinator.queryRecentRooms(
            maxAgeMillis = windowMillis,
            maxRooms = 8,
            onResult = { rooms ->
                val candidate = rooms.firstOrNull { it.ownerId != myUid }
                if (candidate != null) {
                    TRACE(WARNING) { "ROOM_PICK: join room=${candidate.id} owner=${candidate.ownerId ?: "?"}" }
                    // 2a) Join that specific room
                    RoomCoordinator.joinSpecificGame(
                        gameId = candidate.id,
                        myUid = myUid,
                        onInboundMessage = ::handleInbound,
                        onConnected = { res ->
                            // Update multiplayer state (JOINER)
                            setMultiplayerState(
                                MultiplayerState(
                                    mode = MultiplayerMode.JOINER,
                                    myUid = myUid,
                                    currentGameId = res.gameId,
                                    knownRemoteUid = res.ownerId,
                                    localReady = true // We came from 'Play Online' -> implying Ready
                                )
                            )
                            // après setMultiplayerState(...) + notice :
                            res.ownerId?.let { owner ->
                                NetworkManager.sendPing(
                                    gameId = res.gameId,
                                    fromId = myUid,
                                    toId = owner
                                )
                                TRACE(WARNING) { "Ping sent to $owner" }
                            }
                            updateGameState(
                                pendingNotices = gameState.value.pendingNotices +
                                        UiNotice(
                                            message = "Joined game ${res.gameId}",
                                            kind = NoticeKind.Success
                                        )
                            )
                            // 3) Cleanup: delete all my other rooms (simple v1)
                            cleanupMyRoomsAfterConnect(myUid, keepGameId = res.gameId)
                            maybeSendReady() // <--- CRITICAL: Tell host we are ready!
                        },
                        onError = { _ ->
                            // Fallback: host if join failed
                            TRACE(WARNING) { "ROOM_PICK: join failed → hosting new" }
                            hostAfterScan(myUid)
                        }
                    )
                } else {
                    // 2b) No candidate → host
                    TRACE(WARNING) { "ROOM_PICK: no candidate → hosting new" }
                    hostAfterScan(myUid)
                }
            },
            onError = { _ ->
                // On scan error, host new room (keep it moving)
                TRACE(WARNING) { "ROOM_SCAN: error → hosting new" }
                hostAfterScan(myUid)
            }
        )
    }

    // Small helper to host then cleanup (kept local for readability).
    private fun hostAfterScan(myUid: String) {
        RoomCoordinator.hostWaitingGame(
            ownerUid = myUid,
            onInboundMessage = ::handleInbound,
            onConnected = { host ->
                setMultiplayerState(
                    MultiplayerState(
                        mode = MultiplayerMode.HOST,
                        myUid = myUid,
                        currentGameId = host.gameId,
                        knownRemoteUid = null,
                        localReady = true // We came from 'Play Online' -> implying Ready
                    )
                )
                updateGameState(
                    pendingNotices = gameState.value.pendingNotices +
                            UiNotice(
                                message = "Hosting game ${host.gameId}",
                                kind = NoticeKind.Info
                            )
                )
                // Delete all my other rooms (duplicates/old) except the current one
                cleanupMyRoomsAfterConnect(myUid, keepGameId = host.gameId)
            },
            onError = { msg ->
                updateGameState(
                    pendingNotices = gameState.value.pendingNotices +
                            UiNotice(message = "Auto-connect failed: $msg", kind = NoticeKind.Error)
                )
            }
        )
    }

    // --- GameSerializer Delegation ------------------------------------------------

    // Delegates to strict GameSerializer
    private fun encodeTurnPlay(intent: GameSerializer.TurnPlayIntent): String =
        GameSerializer.encodeTurnPlay(intent)

    private fun decodeTurnPlay(json: String): GameSerializer.TurnPlayIntent =
        GameSerializer.decodeTurnPlay(json)

    private fun encodeSnapshot(snapshot: GameSerializer.ClientSnapshot): String =
        GameSerializer.encodeSnapshot(snapshot)

    private fun decodeSnapshot(json: String): GameSerializer.ClientSnapshot =
        GameSerializer.decodeSnapshot(json)

    private fun encodeStart(p: GameSerializer.StartPayload): String =
        GameSerializer.encodeStart(p)

    private fun decodeStart(json: String): GameSerializer.StartPayload =
        GameSerializer.decodeStart(json)

    private fun encodeTurnHint(hint: TurnHintMsg?): String? =
        GameSerializer.encodeTurnHint(hint)

    private fun encodeBanner(banner: BannerMsg?): String? =
        GameSerializer.encodeBanner(banner)

    private fun decodeBanner(json: String): BannerMsg? =
        GameSerializer.decodeBanner(json)

    private fun decodeTurnHint(json: String): TurnHintMsg? =
        GameSerializer.decodeTurnHint(json)


    // --- Restored Logic Methods (using GameSerializer DTOs) ---

    private fun applyStartFromHost(payload: GameSerializer.StartPayload, hostUid: String) {
        val currentPlayers = gameState.value.players
        val currentTurnId = gameState.value.turnId

        val players: List<Player> = payload.players.map { sp ->
            startPlayerToConcrete(sp, hostUid, currentPlayers, payload.turnId, currentTurnId)
        }

        val ms0 = gameState.value.multiplayerState
        val ms = (ms0 ?: MultiplayerState(myUid = myNetworkingUidOrNull() ?: ""))
            .copy(knownRemoteUid = hostUid, gameLaunched = true)
        setMultiplayerState(ms)

        updateGameState(
            players = players,
            startingPlayerIndex = payload.startingPlayerIndex,
            currentPlayerIndex  = payload.currentPlayerIndex,
            currentPlayerId     = players.getOrNull(payload.currentPlayerIndex)?.id ?: "",
            pointLimit          = payload.pointLimit,
            turnId              = payload.turnId
        )
    }

    private fun startPlayerToConcrete(
        sp: GameSerializer.StartPlayer,
        hostUid: String,
        existingPlayers: List<Player>,
        newTurnId: Int,
        currentTurnId: Int
    ): Player {
        val myUid = myNetworkingUidOrNull()
        val mappedType = when {
            sp.type == PlayerType.AI -> PlayerType.AI
            sp.id == myUid           -> PlayerType.LOCAL     // me (guest)
            else                     -> PlayerType.REMOTE    // everyone else (host/humans)
        }
        TRACE(ERROR) { "Mapping player ${sp.name} (id=${sp.id}) vs myUid=$myUid -> $mappedType" }
        val base = when (mappedType) {
            PlayerType.LOCAL  -> LocalPlayer(id = sp.id, name = sp.name, avatar = sp.avatar)
            PlayerType.AI     -> AIPlayer(id = sp.id, name = sp.name, avatar = sp.avatar)
            PlayerType.REMOTE -> RemotePlayer(id = sp.id, name = sp.name, avatar = sp.avatar)
        }

        // Race-fix: if we already received a STATE_SYNC for this same turn (or newer)
        // that populated our hand, preserve it. START payload has no hands.
        if (newTurnId == currentTurnId) {
            val existing = existingPlayers.find { it.id == sp.id }
            if (existing != null && existing.hand.cards.isNotEmpty()) {
                TRACE(INFO) { "Preserving hand for ${sp.name} despite START (race fix)" }
                return base.copy(hand = existing.hand)
            }
        }
        return base
    }

    private fun buildPlayersForHost(ms: MultiplayerState, setup: MpSetup): List<Player> {
        val host = LocalPlayer(
            id = ms.myUid,
            name = setup.localName.ifBlank { "You" },
            avatar = "🙂"
        )
        val remoteUid = ms.knownRemoteUid ?: error("No remote uid")
        val remote = RemotePlayer(id = remoteUid, name = "Guest", avatar = "🌍")
        val ais = (1..setup.aiCount).map { i ->
            AIPlayer(
                id = "${ms.myUid}-ai-$i",
                name = "AI $i",
                avatar = "🤖"
            )
        }
        return listOf(host, remote) + ais
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

    private enum class AuthorityMode { FULL, MIRROR }

    private fun authorityMode(): AuthorityMode =
        when (gameState.value.multiplayerState?.mode) {
            MultiplayerMode.HOST -> AuthorityMode.FULL
            MultiplayerMode.JOINER -> AuthorityMode.MIRROR
            null -> AuthorityMode.FULL // single-player
        }

    private fun isHost() = gameState.value.multiplayerState?.mode == MultiplayerMode.HOST
    private fun isGuest() = gameState.value.multiplayerState?.mode == MultiplayerMode.JOINER


    private fun buildClientSnapshotFor(recipientUid: String?): GameSerializer.ClientSnapshot {
        val state = gameState.value

        val playersPublic = state.players.map { player ->
            GameSerializer.PublicPlayerView(
                playerId = player.id,
                name = player.name,
                type = player.playerType,
                score = player.score,
                handCount = player.hand.cards.size
            )
        }

        // v1: single remote seat -> we expose that hand to the remote client
    val remoteIndex = state.players.indexOfFirst { it.playerType == PlayerType.REMOTE }
    val isFullAuth = authorityMode() == AuthorityMode.FULL
    val handSize = if (remoteIndex >= 0) state.players[remoteIndex].hand.cards.size else -1

    TRACE(WARNING) { "SNAPSHOT_BUILD: recipient=$recipientUid remoteIdx=$remoteIndex auth=$isFullAuth handSize=$handSize players=${state.players.map { "${it.name}:${it.playerType}" }}" }

    val myHand: Hand? =
        if (isFullAuth && recipientUid != null && remoteIndex >= 0) {
             // Basic ID check: Does recipientUid match the remote player? 
             // Currently v1 assumes ANY recipient gets the FIRST remote player's hand.
             // This is risky if IDs mismatch.
             val remotePlayer = state.players[remoteIndex]
             if (remotePlayer.id != recipientUid) {
                 TRACE(ERROR) { "SNAPSHOT_MISMATCH: Sending hand of ${remotePlayer.name}(${remotePlayer.id}) to $recipientUid!" }
             }
            state.players[remoteIndex].hand
        } else null

        return GameSerializer.ClientSnapshot(
            turnId = state.turnId,
            currentPlayerIndex = state.currentPlayerIndex,
            currentPlayerId = state.currentPlayerId,
            matCombination = state.currentCombinationOnMat,
            playersPublic = playersPublic,
            skipCount = state.skipCount,
            pointLimit = state.pointLimit,
            lastMove = state.lastPlayerWhoPlayed?.let { player ->
                GameSerializer.LastMove(
                    byPlayerId = player.id,
                    action = "PLAY",
                    played = state.currentCombinationOnMat.cards,
                    kept = state.lastKeptCard
                )
            },
            myHand = myHand
        )
    }

    private fun applyAuthoritativeSnapshot(snapshot: GameSerializer.ClientSnapshot) {
        TRACE(WARNING) { "APPLY SNAPSHOT: turn=${snapshot.turnId} myHand=${snapshot.myHand?.cards?.size}" }
        dispatcher.enqueueEvent(GameEvent.NetworkStateSync(snapshot))
    }

    private fun broadcastAfterHostCommit() {
        if (!isHost()) return
        TRACE(WARNING) { "BROADCAST_COMMIT: Triggering state sync to guest." }
        val state = gameState.value
        val multiplayerState = state.multiplayerState ?: return
        val gameId = multiplayerState.currentGameId ?: return
        val remoteUid = multiplayerState.knownRemoteUid ?: return // v1: single remote

        // 1) Authoritative snapshot
        val snapshot = buildClientSnapshotFor(remoteUid)
        NetworkManager.sendStateSync(
            gameId,
            multiplayerState.myUid,
            remoteUid,
            encodeSnapshot(snapshot)
        )

        // 2) TurnHint (tiny, UI-only)
        encodeTurnHint(state.turnHintMsg)?.let { json ->
            NetworkManager.sendTurnHint(gameId, multiplayerState.myUid, remoteUid, json)

        }

        // 3) Banner (tiny, UI-only)
        encodeBanner(state.bannerMsg)?.let { json ->
            NetworkManager.sendBannerMsg(gameId, multiplayerState.myUid, remoteUid, json)
        }
    }

    private fun handleSideEffect(effect: GameSideEffect) {
        when (effect) {
            is GameSideEffect.StartAITimer -> launchAITimer(
                effect.turnId,
                this.gameState.value.aiTimerDuration
            )

            is GameSideEffect.ShowDialog -> setGameDialogEvent(effect.dialog)
            is GameSideEffect.GetAIMove -> getAIMove()
            is GameSideEffect.PlaySound -> enqueueSound(effect.effect)
            is GameSideEffect.PlayMusic -> enqueueMusic(
                MusicCommand.Play(
                    effect.track,
                    effect.loop
                )
            )

            is GameSideEffect.StopMusic -> enqueueMusic(MusicCommand.Stop)
        }
    }
}

// Internal helper to expose GameManager as IGameManager.
internal fun getGameManagerInstance(): IGameManager = GameManager
