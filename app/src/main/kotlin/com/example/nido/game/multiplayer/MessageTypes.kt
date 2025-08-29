package com.example.nido.game.multiplayer


/** Public, shared message type names used on the wire. */
object MessageTypes {
    const val MSG_TYPE_TURN_PLAY   = "turn_play"
    const val MSG_TYPE_TURN_SKIP   = "turn_skip"
    const val MSG_TYPE_STATE_SYNC  = "state_sync"
    const val MSG_TYPE_TURN_HINT   = "turn_hint"
    const val MSG_TYPE_BANNER_MSG  = "banner_msg"
    const val MSG_TYPE_CHAT        = "chat"
    const val MSG_TYPE_PING        = "ping"
    const val MSG_TYPE_READY       = "ready"
    const val MSG_TYPE_START       = "start"


    val VALID: Set<String> = setOf(
        MSG_TYPE_TURN_PLAY, MSG_TYPE_TURN_SKIP, MSG_TYPE_STATE_SYNC, MSG_TYPE_TURN_HINT, MSG_TYPE_BANNER_MSG, MSG_TYPE_CHAT, MSG_TYPE_PING, MSG_TYPE_READY, MSG_TYPE_START
    )
}
