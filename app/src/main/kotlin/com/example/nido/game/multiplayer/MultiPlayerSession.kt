package com.example.nido.multiplayer

import com.example.nido.game.multiplayer.RoomCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
/*
sealed class SessionState {
    data object Disconnected : SessionState()
    data class Connecting(val myUid: String) : SessionState()
    data class Connected(
        val myUid: String,
        val gameId: String,
        val remoteUid: String? = null
    ) : SessionState()
}

interface MultiplayerSession {
    val state: StateFlow<SessionState>
    val myUidOrNull: String?
    val gameIdOrNull: String?
    val remoteUidOrNull: String?

    fun hostQuickRoom(myUid: String)
    fun joinRoom(gameId: String, myUid: String)
    fun setRemoteUid(uid: String)
    fun disconnect()
}

object NetworkSession : MultiplayerSession {
    private val _state = MutableStateFlow<SessionState>(SessionState.Disconnected)
    override val state: StateFlow<SessionState> = _state

    override val myUidOrNull get() =
        when (val s = _state.value) {
            is SessionState.Connected -> s.myUid
            is SessionState.Connecting -> s.myUid
            else -> null
        }
    override val gameIdOrNull get() = (state.value as? SessionState.Connected)?.gameId
    override val remoteUidOrNull get() = (state.value as? SessionState.Connected)?.remoteUid

    override fun hostQuickRoom(myUid: String) {
        _state.value = SessionState.Connecting(myUid)
        RoomCoordinator.hostWaitingGame(
            ownerUid = myUid,
            onInboundMessage = ::handleInbound,
            onConnected = { res ->
                _state.value = SessionState.Connected(myUid = myUid, gameId = res.gameId, remoteUid = null)
            },
            onError = { _ ->
                _state.value = SessionState.Disconnected
            }
        )
    }

    override fun joinRoom(gameId: String, myUid: String) {
        _state.value = SessionState.Connecting(myUid)
        RoomCoordinator.joinGame(
            myUid = myUid,
            gameId = gameId,
            onInboundMessage = ::handleInbound,
            onConnected = { _ ->
                _state.value = SessionState.Connected(myUid = myUid, gameId = gameId, remoteUid = null)
            },
            onError = { _ ->
                _state.value = SessionState.Disconnected
            }
        )
    }

    override fun setRemoteUid(uid: String) {
        val cur = _state.value
        if (cur is SessionState.Connected && cur.remoteUid != uid) {
            _state.value = cur.copy(remoteUid = uid)
        }
    }

    override fun disconnect() { _state.value = SessionState.Disconnected }

    private fun handleInbound(msg: NetworkMessage) {
        // Route vers ton GameManager / VM (comme avant)
    }
}
*/