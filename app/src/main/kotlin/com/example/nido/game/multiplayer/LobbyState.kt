package com.example.nido.game.multiplayer

enum class LobbyRole {
    NONE,
    HOST,
    GUEST
}

enum class ConnectionStatus {
    Idle,       // Not doing anything
    Scanning,   // Searching for a game (Guest usually)
    Creating,   // Creating a room (Host)
    Connected,  // In a room, waiting for peer or ready
    GameStarted // Handshake complete, game logic taken over by GameReducer
}

data class LobbyState(
    val role: LobbyRole = LobbyRole.NONE,
    val status: ConnectionStatus = ConnectionStatus.Idle,
    val myUid: String = "",
    val gameId: String? = null,
    val remoteUid: String? = null,
    val localReady: Boolean = false,
    val remoteReady: Boolean = false,
    val error: String? = null
)

sealed class LobbyEvent {
    data class Initialize(val myUid: String) : LobbyEvent()
    object StartScan : LobbyEvent()         // User clicked "Play Online"
    object HostGame : LobbyEvent()          // Fallback or explicit Host
    
    // Internal events driven by NetworkManager/RoomCoordinator callbacks
    data class RoomFound(val gameId: String, val ownerUid: String) : LobbyEvent()
    data class RoomCreated(val gameId: String) : LobbyEvent()
    data class PeerJoined(val remoteUid: String) : LobbyEvent()
    data class ErrorOccurred(val message: String) : LobbyEvent()
    
    // Handshake
    object SendReady : LobbyEvent()
    object RemoteReady : LobbyEvent()
    
    // Game Launch
    object GameLaunched : LobbyEvent()
    
    object Reset : LobbyEvent()
}
