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
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_BANNER_MSG
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_CHAT
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_PING
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_STATE_SYNC
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_TURN_HINT
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_TURN_PLAY
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_TURN_SKIP
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

            val json = encodeTurnPlay(TurnPlayIntent(selectedCards = selectedCards, cardToKeep = cardToKeep))
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

        // Learn peer uid if unknown (single block)
        gameState.value.multiplayerState?.let { ms ->
            if (ms.knownRemoteUid == null) {
                setMultiplayerState(ms.copy(knownRemoteUid = msg.fromId))
            }
        }

        when (msg.type) {
            MSG_TYPE_TURN_PLAY -> if (isHost()) {
                // TODO: decode + enqueue CardPlayed, then broadcastAfterHostCommit()
            }

            MSG_TYPE_TURN_SKIP -> if (isHost()) {
                dispatcher.enqueueEvent(GameEvent.PlayerSkipped)
                // TODO: after reducer commit → broadcastAfterHostCommit()
            }

            MSG_TYPE_STATE_SYNC -> if (isGuest()) {
                // TODO: decode JSON → ClientSnapshot, then applyAuthoritativeSnapshot(snapshot)
            }

            MSG_TYPE_TURN_HINT -> if (isGuest()) {
                val text = msg.text ?: return
                val hint = decodeTurnHint(text)
                updateGameState(gameState.value.copy(turnHintMsg = hint))
            }

            MSG_TYPE_BANNER_MSG -> if (isGuest()) {
                val text = msg.text ?: return
                val banner = decodeBanner(text)
                updateGameState(gameState.value.copy(bannerMsg = banner))
            }


            MSG_TYPE_PING -> {
                notice = UiNotice(message = "Ping received from ${msg.fromId}", kind = NoticeKind.Success)
            }

            MSG_TYPE_CHAT -> {
                notice = UiNotice(message = "Chat from ${msg.fromId}: ${msg.text ?: ""}", kind = NoticeKind.Success)
            }
        }

        notice?.let {
            updateGameState(pendingNotices = gameState.value.pendingNotices + it)
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
// --- replace the existing autoQuickConnect(myUid: String) ---
    override fun autoQuickConnect(myUid: String) {
        val windowMillis = 5 * 60 * 1000L  // 5 minutes
        TRACE(WARNING) { "ROOM_BOOT: start uid=$myUid window=${windowMillis/1000}s" }

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
                                    knownRemoteUid = res.ownerId
                                ))
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
                                        UiNotice(message = "Joined game ${res.gameId}", kind = NoticeKind.Success)
                            )
                            // 3) Cleanup: delete all my other rooms (simple v1)
                            cleanupMyRoomsAfterConnect(myUid, keepGameId = res.gameId)
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
                        knownRemoteUid = null
                    )
                )
                updateGameState(
                    pendingNotices = gameState.value.pendingNotices +
                            UiNotice(message = "Hosting game ${host.gameId}", kind = NoticeKind.Info)
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

    // ---- Neutral wire format for TurnHintMsg (no localization on the wire) ----
    private object HintWireKind {
        const val PLAYER_SKIPPED = "player_skipped"
        const val MAT_DISCARDED_NEXT = "mat_discarded_next"
        const val YOU_CANNOT_BEAT = "you_cannot_beat"
        const val YOU_MUST_PLAY_ONE = "you_must_play_one"
        const val YOU_CAN_PLAY_N_OR_NPLUS1 = "you_can_play_n_or_nplus1"
        const val YOU_KEPT = "you_kept"
        const val PLAYER_KEPT = "player_kept"
    }

    /** Encode a TurnHintMsg into a small JSON string. Returns null if no hint. */
    private fun encodeTurnHint(hint: TurnHintMsg?): String? {
        if (hint == null) return null
        val o = JSONObject()
        when (hint) {
            is TurnHintMsg.PlayerSkipped -> {
                o.put("kind", HintWireKind.PLAYER_SKIPPED)
                o.put("name", hint.name)
            }
            is TurnHintMsg.MatDiscardedNext -> {
                o.put("kind", HintWireKind.MAT_DISCARDED_NEXT)
                o.put("name", hint.name)
            }
            is TurnHintMsg.YouCannotBeat -> {
                o.put("kind", HintWireKind.YOU_CANNOT_BEAT)
            }
            is TurnHintMsg.YouMustPlayOne -> {
                o.put("kind", HintWireKind.YOU_MUST_PLAY_ONE)
                o.put("canAllIn", hint.canAllIn)
            }
            is TurnHintMsg.YouCanPlayNOrNPlusOne -> {
                o.put("kind", HintWireKind.YOU_CAN_PLAY_N_OR_NPLUS1)
                o.put("n", hint.n)
            }
            is TurnHintMsg.YouKept -> {
                o.put("kind", HintWireKind.YOU_KEPT)
                o.put("card", hint.card) // string form you already display in UI
            }
            is TurnHintMsg.PlayerKept -> {
                o.put("kind", HintWireKind.PLAYER_KEPT)
                o.put("name", hint.name)
                o.put("card", hint.card)
            }
        }
        return o.toString()
    }

    /** Decode JSON string back to a TurnHintMsg (or null). */
    private fun decodeTurnHint(json: String): TurnHintMsg? {
        val o = JSONObject(json)
        return when (o.getString("kind")) {
            HintWireKind.PLAYER_SKIPPED -> TurnHintMsg.PlayerSkipped(o.getString("name"))
            HintWireKind.MAT_DISCARDED_NEXT -> TurnHintMsg.MatDiscardedNext(o.getString("name"))
            HintWireKind.YOU_CANNOT_BEAT -> TurnHintMsg.YouCannotBeat
            HintWireKind.YOU_MUST_PLAY_ONE -> TurnHintMsg.YouMustPlayOne(
                canAllIn = o.optBoolean("canAllIn", false)
            )
            HintWireKind.YOU_CAN_PLAY_N_OR_NPLUS1 -> TurnHintMsg.YouCanPlayNOrNPlusOne(
                n = o.optInt("n", 1)
            )
            HintWireKind.YOU_KEPT -> TurnHintMsg.YouKept(
                card = o.getString("card")
            )
            HintWireKind.PLAYER_KEPT -> TurnHintMsg.PlayerKept(
                name = o.getString("name"),
                card = o.getString("card")
            )
            else -> null
        }
    }

    // ---- Neutral wire format for BannerMsg ----
    private object BannerWireKind {
        const val PLAY = "play"
    }

    private fun encodeBanner(banner: BannerMsg?): String? {
        if (banner == null) return null
        val o = JSONObject()
        when (banner) {
            is BannerMsg.Play -> {
                o.put("kind", BannerWireKind.PLAY)
                o.put("name", banner.name)
            }
        }
        return o.toString()
    }

    private fun decodeBanner(json: String): BannerMsg? {
        val o = JSONObject(json)
        return when (o.getString("kind")) {
            BannerWireKind.PLAY -> BannerMsg.Play(name = o.getString("name"))
            else -> null
        }
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

    // Multiplayer part
    // Guest → Host: human intent to play
    private data class TurnPlayIntent(
        val selectedCards: List<Card>,
        val cardToKeep: Card? = null
    )

    // Host → Guests: safe public view
    private data class PublicPlayerView(
        val playerId: String,
        val name: String,
        val type: PlayerType,
        val score: Int,
        val handCount: Int
    )

    // Optional: last move summary for UX
    private data class LastMove(
        val byPlayerId: String,
        val action: String,              // "PLAY" | "SKIP"
        val played: List<Card>? = null,
        val kept: Card? = null
    )

    // Host → Guest authoritative snapshot (personalized: only recipient’s hand)
    private data class ClientSnapshot(
        val turnId: Int,
        val currentPlayerIndex: Int,
        val currentPlayerId: String,
        val matCombination: Combination,
        val playersPublic: List<PublicPlayerView>,
        val skipCount: Int,
        val pointLimit: Int,
        val lastMove: LastMove? = null,
        val myHand: Hand? = null
    )

    private enum class AuthorityMode { FULL, MIRROR } // FULL=host, MIRROR=guest

    private fun authorityMode(): AuthorityMode =
        when (gameState.value.multiplayerState?.mode) {
            MultiplayerMode.HOST   -> AuthorityMode.FULL
            MultiplayerMode.JOINER -> AuthorityMode.MIRROR
            null                   -> AuthorityMode.FULL // single-player
        }

    private fun isHost()  = gameState.value.multiplayerState?.mode == MultiplayerMode.HOST
    private fun isGuest() = gameState.value.multiplayerState?.mode == MultiplayerMode.JOINER


    // --- TURN_PLAY encode/decode ------------------------------------------------
    private fun encodeTurnPlay(intent: TurnPlayIntent): String {
        val root = JSONObject()
        root.put("selectedCards", JSONArray(intent.selectedCards.map { it.toString() })) // TODO: switch to structured JSON
        root.put("cardToKeep", intent.cardToKeep?.toString())
        return root.toString()
    }

    private fun decodeTurnPlay(json: String): TurnPlayIntent {
        val obj = JSONObject(json)
        val selected = obj.optJSONArray("selectedCards")?.let { arr ->
            (0 until arr.length()).map { /* TODO parseCard(arr.getString(it)) */ throw IllegalStateException("parseCard not implemented") }
        } ?: emptyList()
        val keep = obj.optString("cardToKeep", null)?.let { /* TODO parseCard(it) */ null }
        return TurnPlayIntent(selectedCards = selected, cardToKeep = keep)
    }

    // --- STATE_SYNC encode (decode optional now; we apply via structured method) ---
    private fun encodeSnapshot(snapshot: ClientSnapshot): String {
        val root = JSONObject().apply {
            put("turnId", snapshot.turnId)
            put("currentPlayerIndex", snapshot.currentPlayerIndex)
            put("currentPlayerId", snapshot.currentPlayerId)
            put("skipCount", snapshot.skipCount)
            put("pointLimit", snapshot.pointLimit)

            // v1 string forms; replace with structured JSON when ready
            put("matCombination", snapshot.matCombination.toString())

            val players = JSONArray()
            snapshot.playersPublic.forEach { p ->
                players.put(JSONObject().apply {
                    put("playerId", p.playerId)
                    put("name", p.name)
                    put("type", p.type.name)
                    put("score", p.score)
                    put("handCount", p.handCount)
                })
            }
            put("playersPublic", players)

            snapshot.lastMove?.let { lm ->
                put("lastMove", JSONObject().apply {
                    put("byPlayerId", lm.byPlayerId)
                    put("action", lm.action)
                    put("played", lm.played?.let { a -> JSONArray(a.map { it.toString() }) })
                    put("kept", lm.kept?.toString())
                })
            }

            snapshot.myHand?.let { put("myHand", it.toString()) } // TODO structured
        }
        return root.toString()
    }

    private fun buildClientSnapshotFor(recipientUid: String?): ClientSnapshot {
        val state = gameState.value

        val playersPublic = state.players.map { player ->
            PublicPlayerView(
                playerId = player.id,
                name = player.name,
                type = player.playerType,
                score = player.score,
                handCount = player.hand.cards.size
            )
        }

        // v1: single remote seat → we expose that hand to the remote client
        val remoteIndex = state.players.indexOfFirst { it.playerType == PlayerType.REMOTE }
        val myHand: Hand? =
            if (authorityMode() == AuthorityMode.FULL && recipientUid != null && remoteIndex >= 0) {
                state.players[remoteIndex].hand
            } else null

        return ClientSnapshot(
            turnId = state.turnId,
            currentPlayerIndex = state.currentPlayerIndex,
            currentPlayerId = state.currentPlayerId,
            matCombination = state.currentCombinationOnMat,
            playersPublic = playersPublic,
            skipCount = state.skipCount,
            pointLimit = state.pointLimit,
            lastMove = null, // populate when you track it in state
            myHand = myHand
        )
    }

    private fun applyAuthoritativeSnapshot(snapshot: ClientSnapshot) {
        val state = gameState.value
        if (snapshot.turnId <= state.turnId) return // de-duplication (at-least-once)

        val remappedPlayers = state.players.map { player ->
            val pub = snapshot.playersPublic.firstOrNull { it.playerId == player.id } ?: return@map player
            val newHand = if (snapshot.myHand != null && player.playerType == PlayerType.REMOTE) snapshot.myHand else player.hand
            player.copy(score = pub.score, hand = newHand)
        }

        updateGameState(
            players = remappedPlayers,
            currentPlayerIndex = snapshot.currentPlayerIndex,
            currentPlayerId = snapshot.currentPlayerId,
            currentCombinationOnMat = snapshot.matCombination,
            skipCount = snapshot.skipCount,
            pointLimit = snapshot.pointLimit,
            turnId = snapshot.turnId
        )
    }

    private fun broadcastAfterHostCommit() {
        if (!isHost()) return
        val state = gameState.value
        val multiplayerState = state.multiplayerState ?: return
        val gameId = multiplayerState.currentGameId ?: return
        val remoteUid = multiplayerState.knownRemoteUid ?: return // v1: single remote

        // 1) Authoritative snapshot
        val snapshot = buildClientSnapshotFor(remoteUid)
        NetworkManager.sendStateSync(gameId, multiplayerState.myUid, remoteUid, encodeSnapshot(snapshot))

        // 2) TurnHint (tiny, UI-only)
        encodeTurnHint(state.turnHintMsg)?.let { json ->
            NetworkManager.sendTurnHint(gameId, multiplayerState.myUid, remoteUid, json)

        }

        // 3) Banner (tiny, UI-only)
        encodeBanner(state.bannerMsg)?.let { json ->
            NetworkManager.sendBannerMsg(gameId, multiplayerState.myUid, remoteUid, json)
        }



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
        updateState = { next ->
            val prev = gameState.value
            updateGameState(next)
            if (isHost()) {
                broadcastAfterHostCommitIfChanged(prev, gameState.value)
            }
        },
        handleSideEffect = ::handleSideEffect,
        reducer = ::gameReducer
    )

    private fun broadcastAfterHostCommitIfChanged(prev: GameState, next: GameState) {
        val ms = next.multiplayerState ?: return
        val gameId = ms.currentGameId ?: return
        val remote = ms.knownRemoteUid ?: return

        // 1) Send authoritative snapshot only if core public state changed
        val publicChanged =
            prev.turnId != next.turnId ||
                    prev.currentPlayerIndex != next.currentPlayerIndex ||
                    prev.currentCombinationOnMat != next.currentCombinationOnMat ||
                    prev.skipCount != next.skipCount ||
                    // keep it cheap: compare {score, handCount} only
                    prev.players.map { it.score to it.hand.cards.size } !=
                    next.players.map { it.score to it.hand.cards.size }

        if (publicChanged) {
            val snapshotJson = encodeSnapshot(buildClientSnapshotFor(remote))
            NetworkManager.sendStateSync(gameId, ms.myUid, remote, snapshotJson)
        }

        // 2) Send hint only if it changed
        if (prev.turnHintMsg != next.turnHintMsg) {
            encodeTurnHint(next.turnHintMsg)?.let { json ->
                NetworkManager.sendTurnHint(gameId, ms.myUid, remote, json)
            }
        }

        // 3) Send banner only if it changed
        if (prev.bannerMsg != next.bannerMsg) {
            encodeBanner(next.bannerMsg)?.let { json ->
                NetworkManager.sendBannerMsg(gameId, ms.myUid, remote, json)
            }
        }
    }

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
