package com.example.nido.game.multiplayer

import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*

data class LobbyReducerResult(
    val newState: LobbyState,
    val sideEffects: List<LobbySideEffect> = emptyList()
)

sealed class LobbySideEffect {
    data class ScanForRooms(val myUid: String) : LobbySideEffect()
    data class CreateRoom(val myUid: String) : LobbySideEffect()
    data class JoinInGame(val gameId: String, val myUid: String) : LobbySideEffect()
    data class SendReadyPacket(val gameId: String, val myUid: String, val toUid: String) : LobbySideEffect()
    data class NotifyGameStart(val gameId: String, val isHost: Boolean, val remoteUid: String) : LobbySideEffect()
}

fun lobbyReducer(state: LobbyState, event: LobbyEvent): LobbyReducerResult {
    TRACE(DEBUG) { "LobbyEvent: $event | Current State: ${state.status} Role: ${state.role}" }

    return when (event) {
        is LobbyEvent.Initialize -> {
            LobbyReducerResult(state.copy(myUid = event.myUid, status = ConnectionStatus.Idle))
        }

        is LobbyEvent.StartScan -> {
            if (state.myUid.isBlank()) return LobbyReducerResult(state) // Safety check
            LobbyReducerResult(
                newState = state.copy(
                    status = ConnectionStatus.Scanning,
                    role = LobbyRole.GUEST, // Assume guest first when scanning
                    error = null
                ),
                sideEffects = listOf(LobbySideEffect.ScanForRooms(state.myUid))
            )
        }

        is LobbyEvent.HostGame -> {
             LobbyReducerResult(
                newState = state.copy(
                    status = ConnectionStatus.Creating,
                    role = LobbyRole.HOST,
                    error = null
                ),
                sideEffects = listOf(LobbySideEffect.CreateRoom(state.myUid))
            )
        }

        is LobbyEvent.RoomFound -> {
            // We found a room to join
            LobbyReducerResult(
                newState = state.copy(
                    status = ConnectionStatus.Connected,
                    role = LobbyRole.GUEST,
                    gameId = event.gameId,
                    remoteUid = event.ownerUid,
                    localReady = true // Auto-ready when joining via "Quick Play"
                ),
                sideEffects = listOf(
                    LobbySideEffect.JoinInGame(event.gameId, state.myUid),
                    // We also want to signal readiness immediately if we joined successfully
                    LobbySideEffect.SendReadyPacket(event.gameId, state.myUid, event.ownerUid)
                )
            )
        }

        is LobbyEvent.RoomCreated -> {
            LobbyReducerResult(
                newState = state.copy(
                    status = ConnectionStatus.Connected,
                    role = LobbyRole.HOST,
                    gameId = event.gameId,
                    localReady = true // Host is always ready waiting for connection
                )
            )
        }

        is LobbyEvent.PeerJoined -> {
            // A remote player has connected (relevant for Host)
            LobbyReducerResult(
                newState = state.copy(remoteUid = event.remoteUid)
            )
        }
        
        is LobbyEvent.SendReady -> {
            // User manually clicked ready (or triggered automatically)
             if (state.gameId != null && state.remoteUid != null) {
                LobbyReducerResult(
                    newState = state.copy(localReady = true),
                     sideEffects = listOf(LobbySideEffect.SendReadyPacket(state.gameId, state.myUid, state.remoteUid))
                )
             } else {
                 LobbyReducerResult(state)
             }
        }

        is LobbyEvent.RemoteReady -> {
            val newState = state.copy(remoteReady = true)
            // If both are ready, launch game!
            if (newState.localReady) {
                 LobbyReducerResult(
                    newState = newState.copy(status = ConnectionStatus.GameStarted),
                    sideEffects = listOf(
                        LobbySideEffect.NotifyGameStart(
                            gameId = state.gameId!!, 
                            isHost = state.role == LobbyRole.HOST, 
                            remoteUid = state.remoteUid!!
                        )
                    )
                )
            } else {
                // If we are not ready yet, we should probably auto-respond ready if this was a quick match
                 LobbyReducerResult(
                    newState = newState.copy(localReady = true), // Auto-ready echo
                    sideEffects = listOf(
                         LobbySideEffect.SendReadyPacket(state.gameId!!, state.myUid, state.remoteUid!!)
                    )
                )
            }
        }
        
        is LobbyEvent.GameLaunched -> {
             LobbyReducerResult(state.copy(status = ConnectionStatus.GameStarted))
        }

        is LobbyEvent.ErrorOccurred -> {
            LobbyReducerResult(
                state.copy(
                    status = ConnectionStatus.Idle, 
                    error = event.message,
                    role = LobbyRole.NONE
                )
            )
        }

        is LobbyEvent.Reset -> {
            LobbyReducerResult(LobbyState(myUid = state.myUid))
        }
    }
}
