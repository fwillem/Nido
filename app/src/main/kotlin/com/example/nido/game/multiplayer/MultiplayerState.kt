// file: game/multiplayer/MultiplayerState.kt
package com.example.nido.game.multiplayer

/** Role of this device in the current room. */
enum class MultiplayerMode { HOST, JOINER }

/** Lightweight runtime networking/session state (not part of core domain). */
data class MultiplayerState(
    val myUid: String,
    val currentGameId: String? = null,
    val mode: MultiplayerMode? = null,
    val knownRemoteUid: String? = null, // last known peer (host for a joiner, or last sender for a host)
    val localReady: Boolean = false,
    val remoteReady: Boolean = false,
    val gameLaunched: Boolean = false,
)
