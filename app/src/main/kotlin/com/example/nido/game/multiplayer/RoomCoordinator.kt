package com.example.nido.game.multiplayer

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import java.util.UUID
import com.google.firebase.Timestamp


/**
 * Thin lobby orchestrator on top of Firestore.
 *
 * Responsibilities:
 * - Host creates a "waiting" room and connects NetworkManager to its messages.
 * - Guest auto-joins the first "waiting" room (by createdAt) and connects.
 * - No UI lists here; this is purely orchestration to obtain a shared gameId.
 *
 * Firestore layout (minimal):
 * games/{gameId} {
 *   ownerId: String,
 *   status: "waiting" | "playing" | "closed",
 *   players: [String],         // array of uids
 *   maxPlayers: Int,
 *   pointLimit: Int,
 *   createdAt: server timestamp,
 *   updatedAt: server timestamp
 * }
 */
object RoomCoordinator {

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    data class HostResult(val gameId: String)
    data class JoinResult(val gameId: String, val ownerId: String?)

    /** Small, human-friendly code (kept for potential sharing/debug). */
    fun newRoomCode(length: Int = 8): String =
        UUID.randomUUID().toString().replace("-", "").take(length).uppercase()

    /**
     * Host a new "waiting" game and immediately connect locally to its messages.
     *
     * @param ownerUid         uid of the hosting device (will be the first player)
     * @param maxPlayers       capacity hint (used to block extra joins in transaction)
     * @param pointLimit       game rule hint (metadata only, not enforced here)
     * @param onInboundMessage callback passed to NetworkManager.connectToGame
     * @param onConnected      invoked when room is created & connected
     * @param onError          invoked if Firestore write fails
     * @param gameId           optional pre-generated id (lets callers get the id synchronously)
     */
    fun hostWaitingGame(
        ownerUid: String,
        maxPlayers: Int = 2,
        pointLimit: Int = 15,
        onInboundMessage: (NetworkManager.InboundMessage) -> Unit,
        onConnected: (HostResult) -> Unit = {},
        onError: (String) -> Unit = {},
        gameId: String = newRoomCode()
    ) {
        val ref = db.collection("games").document(gameId)

        val payload = mapOf(
            "ownerId" to ownerUid,
            "status" to "waiting",
            "players" to listOf(ownerUid),
            "maxPlayers" to maxPlayers,
            "pointLimit" to pointLimit,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        TRACE(INFO) { "ROOM hostWaitingGame: creating games/$gameId owner=$ownerUid" }
        ref.set(payload)
            .addOnSuccessListener {
                NetworkManager.connectToGame(gameId, ownerUid, onInboundMessage)
                TRACE(WARNING) { "ROOM hostWaitingGame: CONNECTED to games/$gameId as $ownerUid" }
                onConnected(HostResult(gameId))
            }
            .addOnFailureListener { e ->
                TRACE(ERROR) { "ROOM hostWaitingGame: failed: ${e.message}" }
                onError(e.message ?: "host failed")
            }
    }

    /**
     * Join the first available "waiting" game (ordered by createdAt) and connect.
     * Uses a transaction to avoid races when multiple guests join at the same time.
     *
     * Note: the query may require a composite index on (status, createdAt).
     */
    fun joinFirstWaitingGame(
        myUid: String,
        onInboundMessage: (NetworkManager.InboundMessage) -> Unit,
        onConnected: (JoinResult) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        db.collection("games")
            .whereEqualTo("status", "waiting")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    onError("No waiting games.")
                    return@addOnSuccessListener
                }

                fun tryIdx(i: Int) {
                    if (i >= snap.size()) {
                        onError("All rooms full or changed.")
                        return
                    }
                    val ref = snap.documents[i].reference

                    db.runTransaction { tx ->
                        val s = tx.get(ref)

                        val status = s.getString("status") ?: "waiting"
                        if (status != "waiting") error("not waiting")

                        val players = (s.get("players") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        val maxPlayers = (s.getLong("maxPlayers") ?: 2L).toInt()

                        if (myUid in players) error("already joined")
                        if (players.size >= maxPlayers) error("full")

                        val updatedPlayers = players + myUid
                        tx.update(
                            ref,
                            mapOf(
                                "players" to updatedPlayers,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )

                        // Transaction result: (gameId, ownerId)
                        Pair(ref.id, s.getString("ownerId"))
                    }.addOnSuccessListener { (gameId, ownerId) ->
                        TRACE(WARNING) { "ROOM joinFirstWaitingGame: joined $gameId" }
                        NetworkManager.connectToGame(gameId, myUid, onInboundMessage)
                        TRACE(WARNING) { "ROOM joinFirstWaitingGame: CONNECTED to games/$gameId as $myUid" }
                        onConnected(JoinResult(gameId, ownerId))
                    }.addOnFailureListener {
                        // Try the next candidate room on conflict/full/not-waiting
                        tryIdx(i + 1)
                    }
                }

                tryIdx(0)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "query failed")
            }
    }

    // -----------------------------------------------------------------------
    // Back-compat wrappers to match earlier GameManager calls
    // -----------------------------------------------------------------------

    /**
     * Synchronous-style helper: returns the new gameId immediately so the caller
     * can store it in state, while the Firestore write/connection happen async.
     */
    fun hostNewGame(
        hostUid: String,
        onInboundMessage: (NetworkManager.InboundMessage) -> Unit,
        onConnected: (HostResult) -> Unit = {},
        onError: (String) -> Unit = {}
    ): HostResult {
        val gameId = newRoomCode()
        hostWaitingGame(
            ownerUid = hostUid,
            onInboundMessage = onInboundMessage,
            onConnected = onConnected,
            onError = onError,
            gameId = gameId
        )
        return HostResult(gameId)
    }

    /**
     * Wrapper that returns null via callback if no room could be joined.
     */
    fun joinFirstOpenGame(
        myUid: String,
        onInboundMessage: (NetworkManager.InboundMessage) -> Unit,
        onResult: (JoinResult?) -> Unit
    ) {
        joinFirstWaitingGame(
            myUid = myUid,
            onInboundMessage = onInboundMessage,
            onConnected = { jr -> onResult(jr) },
            onError = { _ -> onResult(null) }
        )
    }

    /**
     * Return at most [maxRooms] most recent rooms (by createdAt DESC), filtered by [maxAgeMillis].
     * NOTE: We keep it simple: client-side age filtering, no status check for v1.
     */
    fun queryRecentRooms(
        maxAgeMillis: Long,
        maxRooms: Int = 10,
        onResult: (List<RoomSummary>) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        db.collection("games")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(maxRooms.toLong())
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.map { d ->
                    RoomSummary(
                        id = d.id,
                        ownerId = d.getString("ownerId"),
                        createdAt = d.getTimestamp("createdAt"),
                        updatedAt = d.getTimestamp("updatedAt")
                    )
                }.filter { rs ->
                    val t = (rs.updatedAt ?: rs.createdAt)?.toDate()?.time ?: 0L
                    t > 0L && (now - t) <= maxAgeMillis
                }
                TRACE(WARNING) { "ROOM_SCAN: fetched=${snap.size()} recent=${list.size}" }
                onResult(list)
            }
            .addOnFailureListener { e ->
                TRACE(ERROR) { "ROOM_SCAN error: ${e.message}" }
                onError(e.message ?: "scan failed")
            }
    }

    /**
     * Join a specific room (append myUid to players if not already present).
     * v1 naive: no status checks; keeps it minimal.
     */
    fun joinSpecificGame(
        gameId: String,
        myUid: String,
        onInboundMessage: (NetworkManager.InboundMessage) -> Unit,
        onConnected: (JoinResult) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val ref = db.collection("games").document(gameId)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            if (!snap.exists()) error("room not found")
            val players = (snap.get("players") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val ownerId = snap.getString("ownerId")
            val maxPlayers = (snap.getLong("maxPlayers") ?: 2L).toInt()

            val newPlayers = if (myUid in players) players else players + myUid
            if (newPlayers.size > maxPlayers) error("room full")

            tx.update(ref, mapOf(
                "players" to newPlayers,
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            Pair(ownerId, newPlayers)
        }.addOnSuccessListener { (ownerId, _) ->
            NetworkManager.connectToGame(gameId, myUid, onInboundMessage)
            TRACE(WARNING) { "ROOM_JOIN: CONNECTED to games/$gameId as $myUid (owner=$ownerId)" }
            onConnected(JoinResult(gameId, ownerId))
        }.addOnFailureListener { e ->
            TRACE(ERROR) { "ROOM_JOIN error: ${e.message}" }
            onError(e.message ?: "join failed")
        }
    }

    /**
     * Delete every room owned by [ownerUid] except [excludeGameId].
     * v1 simple: no status checks; we assume caller knows what to keep.
     */
    fun deleteOwnedRooms(
        ownerUid: String,
        excludeGameId: String?,
        onCompleted: (Int) -> Unit = {}
    ) {
        db.collection("games")
            .whereEqualTo("ownerId", ownerUid)
            .limit(50)
            .get()
            .addOnSuccessListener { snap ->
                val targets = snap.documents.filter { it.id != excludeGameId }
                TRACE(WARNING) { "ROOM_CLEAN: owner=$ownerUid candidates=${targets.size}" }
                if (targets.isEmpty()) {
                    onCompleted(0); return@addOnSuccessListener
                }
                var deleted = 0
                targets.forEach { d ->
                    d.reference.delete()
                        .addOnSuccessListener {
                            deleted++
                            TRACE(INFO) { "ROOM_CLEAN: deleted room=${d.id}" }
                            if (deleted == targets.size) onCompleted(deleted)
                        }
                        .addOnFailureListener { e ->
                            TRACE(ERROR) { "ROOM_CLEAN delete fail id=${d.id} err=${e.message}" }
                            // continue; we still call onCompleted when last callback returns (best effort)
                            if (++deleted == targets.size) onCompleted(targets.size)
                        }
                }
            }
            .addOnFailureListener { e ->
                TRACE(ERROR) { "ROOM_CLEAN query fail: ${e.message}" }
                onCompleted(0)
            }
    }



}

/** Minimal snapshot used for simple picking/cleanup (no heavy status for now). */
data class RoomSummary(
    val id: String,
    val ownerId: String?,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?
)