package com.example.nido.replay

import java.time.Instant

/**
 * A GameSession represents one full playthrough of a game,
 * from start to finish. It aggregates all GameActions
 * (played cards, skips, deals, etc.).
 */
data class GameSession(
    val sessionId: String,              // unique ID, e.g. UUID
    val startTimestamp: Long = Instant.now().toEpochMilli(),
    var endTimestamp: Long? = null,     // changed to var
    val actions: MutableList<GameAction> = mutableListOf()
)
