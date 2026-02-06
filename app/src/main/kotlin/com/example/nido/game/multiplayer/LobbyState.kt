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
    val error: String? = null,
    val blacklistedRoomIds: Set<String> = emptySet()
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
    
    // Timeout
    object ConnectionTimeout : LobbyEvent()

    object Reset : LobbyEvent()
}


/**
 * FULL debug string for LobbyEvent:
 * - explicit event name
 * - explicit fields
 * - NO truncation of IDs
 * - one line (Logcat-friendly)
 */
fun LobbyEvent.debugString(): String = when (this) {
    is LobbyEvent.Initialize ->
        "LobbyEvent.Initialize(myUid=$myUid)"

    LobbyEvent.StartScan ->
        "LobbyEvent.StartScan"

    LobbyEvent.HostGame ->
        "LobbyEvent.HostGame"

    is LobbyEvent.RoomFound ->
        "LobbyEvent.RoomFound(gameId=$gameId, ownerUid=$ownerUid)"

    is LobbyEvent.RoomCreated ->
        "LobbyEvent.RoomCreated(gameId=$gameId)"

    is LobbyEvent.PeerJoined ->
        "LobbyEvent.PeerJoined(remoteUid=$remoteUid)"

    is LobbyEvent.ErrorOccurred ->
        "LobbyEvent.ErrorOccurred(message=${message.quoteForLog()})"

    LobbyEvent.SendReady ->
        "LobbyEvent.SendReady"

    LobbyEvent.RemoteReady ->
        "LobbyEvent.RemoteReady"

    LobbyEvent.GameLaunched ->
        "LobbyEvent.GameLaunched"
        
    LobbyEvent.ConnectionTimeout ->
        "LobbyEvent.ConnectionTimeout"

    LobbyEvent.Reset ->
        "LobbyEvent.Reset"
}

/**
 * FULL debug string for LobbyState (optional but recommended).
 * Still one line, but includes all critical identifiers without truncation.
 */
fun LobbyState.debugString(): String =
    "LobbyState(role=$role, status=$status, myUid=$myUid, gameId=${gameId ?: "-"}, " +
            "remoteUid=${remoteUid ?: "-"}, localReady=$localReady, remoteReady=$remoteReady, " +
            "error=${error?.quoteForLog() ?: "-"}, blacklistSize=${blacklistedRoomIds.size})"

/**
 * Keep logs one-line safe: escape newlines/tabs/quotes.
 * (No truncation: you asked for full decoding.)
 */
private fun String.quoteForLog(): String =
    buildString(length + 2) {
        append('"')
        for (ch in this@quoteForLog) {
            when (ch) {
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '"'  -> append("\\\"")
                else -> append(ch)
            }
        }
        append('"')
    }