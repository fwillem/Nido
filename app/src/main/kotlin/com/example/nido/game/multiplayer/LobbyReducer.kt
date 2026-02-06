package com.example.nido.game.multiplayer

import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceDomain
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
    
    // New side effects for timeout management
    object StartConnectionTimer : LobbySideEffect()
    object CancelConnectionTimer : LobbySideEffect()
}

fun lobbyReducer(state: LobbyState, event: LobbyEvent): LobbyReducerResult {
    TRACE(INFO,domain = TraceDomain.LOBBY) { event.debugString() }

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
                    LobbySideEffect.StartConnectionTimer // Start timeout guard
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
                // Determine if we should start
                val newState = state.copy(localReady = true)
                val sideEffects = mutableListOf<LobbySideEffect>(
                     LobbySideEffect.SendReadyPacket(state.gameId, state.myUid, state.remoteUid)
                )

                if (state.remoteReady) {
                    // Both ready -> GO!
                    sideEffects += LobbySideEffect.NotifyGameStart(
                        gameId = state.gameId, 
                        isHost = state.role == LobbyRole.HOST, 
                        remoteUid = state.remoteUid
                    )
                    sideEffects += LobbySideEffect.CancelConnectionTimer // Stop timer

                     LobbyReducerResult(
                        newState = newState.copy(status = ConnectionStatus.GameStarted),
                        sideEffects = sideEffects
                    )
                } else {
                     LobbyReducerResult(
                        newState = newState,
                        sideEffects = sideEffects
                    )
                }
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
                        ),
                        LobbySideEffect.CancelConnectionTimer // Stop timer
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
             LobbyReducerResult(
                 state.copy(
                     status = ConnectionStatus.GameStarted,
                     remoteReady = true // Implicit trust: if game launches, peer is ready
                 ),
                 sideEffects = listOf(LobbySideEffect.CancelConnectionTimer) // Stop timer
             )
        }

        is LobbyEvent.ErrorOccurred -> {
            TRACE(WARNING, domain = TraceDomain.LOBBY) { "Error: ${event.message} -> Falling back to HOST" }
            LobbyReducerResult(
                newState = state.copy(
                    status = ConnectionStatus.Creating,
                    role = LobbyRole.HOST,
                    error = event.message // Keep the error visible briefly or log it?
                ),
                sideEffects = listOf(LobbySideEffect.CreateRoom(state.myUid))
            )
        }

        is LobbyEvent.ConnectionTimeout -> {
            val failedId = state.gameId
            val newBlacklist = if (failedId != null) {
                state.blacklistedRoomIds + failedId
            } else {
                state.blacklistedRoomIds
            }
            
            TRACE(WARNING, domain = TraceDomain.LOBBY) { "Timeout! Blacklisting $failedId. Retrying..." }

            // Reset state to scanning and try again, preserving blacklist
            LobbyReducerResult(
                newState = state.copy(
                    status = ConnectionStatus.Scanning,
                    role = LobbyRole.GUEST,
                    gameId = null,
                    remoteUid = null,
                    error = "Connection timed out",
                    blacklistedRoomIds = newBlacklist
                ),
                sideEffects = listOf(LobbySideEffect.ScanForRooms(state.myUid))
            )
        }

        is LobbyEvent.Reset -> {
            LobbyReducerResult(LobbyState(myUid = state.myUid))
        }
    }
}
