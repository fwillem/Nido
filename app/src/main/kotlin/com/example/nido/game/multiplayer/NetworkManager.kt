

package com.example.nido.game.multiplayer

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldPath // ✨
import com.example.nido.utils.Constants // ✨

/**
 * Lightweight Firestore-based messaging hub used by GameManager.
 * - Keeps UI ignorant of networking details.
 * - All messages go to games/{gameId}/messages.
 * - Listener receives messages addressed to the local player (toId = myPlayerId).
 *
 * Hardening in this version:
 *  - Single active connection (game) at a time.
 *  - All sends go to the currently connected gameId (callers cannot accidentally send to a different room).
 *  - Aggressive traces (T0..T4) that prove path, filters, change counts, and write targets.
 */
object NetworkManager {

    // Single live listener
    private var registration: ListenerRegistration? = null

    // Active connection context
    @Volatile private var activeGameId: String? = null
    @Volatile private var activePlayerId: String? = null

    data class InboundMessage(
        val type: String,
        val fromId: String,
        val text: String? = null
    )

    /**
     * Start listening for messages addressed to 'myPlayerId' in the given game room.
     *
     * Firestore path: games/{gameId}/messages
     * Message schema:
     *  - type: String ("chat" | "ping" | ...)
     *  - fromId: String
     *  - toId: String
     *  - text: String? (optional)
     *  - createdAt: FieldValue.serverTimestamp()
     */
    fun connectToGame(
        gameId: String,
        myPlayerId: String,
        onMessage: (InboundMessage) -> Unit
    ) {
        // Replace any previous listener
        registration?.remove()
        registration = null

        val forcedGameId = Constants.DEV_FORCE_GAME_ID // ✨
        val forcedMyId = Constants.ME_UID // ✨

        activeGameId = forcedGameId // ✨
        activePlayerId = forcedMyId // ✨

        TRACE(WARNING) { "DEV HARD-CODE: ignoring provided gameId=$gameId, myPlayerId=$myPlayerId -> using gameId=$forcedGameId, myId=$forcedMyId" } // ✨

        val app = try { FirebaseApp.getInstance() } catch (_: Exception) { null }
        val options = app?.options
        TRACE(INFO) {
            "T0 Firebase: projectId=${options?.projectId ?: "-"} appId=${options?.applicationId ?: "-"} (dbUrl=${options?.databaseUrl ?: "-"})"
        }

        val db = FirebaseFirestore.getInstance()
        val colRef = db.collection("games")
            .document(forcedGameId) // ✨
            .collection("messages")

        TRACE(INFO) {
            "T1 Listening at path=games/$forcedGameId/messages filter: toId == $forcedMyId" // ✨
        }

        // One-shot audit: show how many docs are currently in the collection (useful to prove room alignment)
        colRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { snap ->
                val count = snap.size()
                TRACE(INFO) { "T2 AUDIT: $count docs currently in games/$forcedGameId/messages" } // ✨
            }
            .addOnFailureListener { e ->
                TRACE(WARNING) { "T2 AUDIT failed: ${e.message}" }
            }

        // Live listener for messages addressed to me in this room
        registration = colRef
            .whereEqualTo("toId", forcedMyId) // ✨
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .orderBy(FieldPath.documentId()) // ✨
            .addSnapshotListener { snapshot, error ->

                // Drop events if we got disconnected/reconnected to another room meanwhile
                val stillGameId = activeGameId
                if (stillGameId != forcedGameId) { // ✨
                    TRACE(WARNING) {
                        "Listener event for stale room: eventGameId=$forcedGameId activeGameId=$stillGameId (ignored)" // ✨
                    }
                    return@addSnapshotListener
                }

                if (error != null) {
                    TRACE(ERROR) { "Listener error: ${error.message}" }
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    TRACE(WARNING) { "Listener snapshot is null" }
                    return@addSnapshotListener
                }

                TRACE(DEBUG) {
                    "T4 snapshot changes=${snapshot.documentChanges.size} ${
                        snapshot.documentChanges.joinToString(prefix = "[", postfix = "]") { it.type.name }
                    } (fromCache=${snapshot.metadata.isFromCache} pendingWrites=${snapshot.metadata.hasPendingWrites()})"
                }

                if (snapshot.documentChanges.isEmpty()) {
                    TRACE(VERBOSE) { "Listener tick: no changes" }
                }

                for (change in snapshot.documentChanges) {
                    if (change.type != DocumentChange.Type.ADDED) {
                        TRACE(VERBOSE) { "Ignoring change type=${change.type} id=${change.document.id}" }
                        continue
                    }
                    val data = change.document.data
                    val type = data["type"] as? String ?: run {
                        TRACE(WARNING) { "Missing 'type' field in ${change.document.id}" }
                        continue
                    }
                    val fromId = data["fromId"] as? String ?: run {
                        TRACE(WARNING) { "Missing 'fromId' in ${change.document.id}" }
                        continue
                    }
                    val text = data["text"] as? String
                    TRACE(INFO) {
                        "Incoming message id=${change.document.id} type=$type from=$fromId text=${text ?: ""}"
                    }
                    onMessage(InboundMessage(type = type, fromId = fromId, text = text))
                }
            }
    }

    /** Stop listening and clear active room context. */
    fun disconnect() {
        if (registration != null) {
            TRACE(INFO) { "Detaching listener" }
        }
        registration?.remove()
        registration = null
        activeGameId = null
        activePlayerId = null
    }

    /**
     * Send a chat message to another player (DEV hard-coded IDs).
     * NOTE: Always targets the forced room/IDs in dev mode.
     */
    fun sendChat(
        gameId: String,   // kept for source-compat; ignored
        fromId: String,   // ignored in dev hard-code
        toId: String,     // ignored in dev hard-code
        text: String
    ) {
        val forcedGameId = activeGameId ?: Constants.DEV_FORCE_GAME_ID // ✨
        val fromIdFixed = Constants.ME_UID // ✨
        val toIdFixed = Constants.OTHER_UID // ✨
        if (activeGameId == null) TRACE(WARNING) { "DEV HARD-CODE: no active connection; forcing gameId=$forcedGameId" } // ✨
        TRACE(WARNING) { "DEV HARD-CODE: ignoring provided ids (gameId=$gameId fromId=$fromId toId=$toId) -> using gameId=$forcedGameId fromId=$fromIdFixed toId=$toIdFixed" } // ✨

        val db = FirebaseFirestore.getInstance()
        val colRef = db.collection("games")
            .document(forcedGameId) // ✨
            .collection("messages")

        val payload = hashMapOf(
            "type" to "chat",
            "fromId" to fromIdFixed, // ✨
            "toId" to toIdFixed,     // ✨
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )

        val app = try { FirebaseApp.getInstance() } catch (_: Exception) { null }
        val options = app?.options
        TRACE(INFO) {
            "T3 WRITE chat -> path=games/$forcedGameId/messages  projectId=${options?.projectId ?: "-"} appId=${options?.applicationId ?: "-"} (dbUrl=${options?.databaseUrl ?: "-"})  {fromId=$fromIdFixed toId=$toIdFixed}" // ✨
        }

        colRef.add(payload)
            .addOnSuccessListener { ref ->
                TRACE(INFO) { "WRITE ok id=${ref.id} path=games/$forcedGameId/messages/${ref.id}" } // ✨
            }
            .addOnFailureListener { e ->
                TRACE(ERROR) { "sendChat failed to=$toIdFixed error=${e.message}" } // ✨
            }
    }

    /**
     * Send a ping message (no text) (DEV hard-coded IDs).
     * NOTE: Always targets the forced room/IDs in dev mode.
     */
    fun sendPing(
        gameId: String,   // kept for source-compat; ignored
        fromId: String,   // ignored in dev hard-code
        toId: String      // ignored in dev hard-code
    ) {
        val forcedGameId = activeGameId ?: Constants.DEV_FORCE_GAME_ID // ✨
        val fromIdFixed = Constants.ME_UID // ✨
        val toIdFixed = Constants.OTHER_UID // ✨
        if (activeGameId == null) TRACE(WARNING) { "DEV HARD-CODE: no active connection; forcing gameId=$forcedGameId" } // ✨
        TRACE(WARNING) { "DEV HARD-CODE: ignoring provided ids (gameId=$gameId fromId=$fromId toId=$toId) -> using gameId=$forcedGameId fromId=$fromIdFixed toId=$toIdFixed" } // ✨

        val db = FirebaseFirestore.getInstance()
        val colRef = db.collection("games")
            .document(forcedGameId) // ✨
            .collection("messages")

        val payload = hashMapOf(
            "type" to "ping",
            "fromId" to fromIdFixed, // ✨
            "toId" to toIdFixed,     // ✨
            "createdAt" to FieldValue.serverTimestamp()
        )

        val app = try { FirebaseApp.getInstance() } catch (_: Exception) { null }
        val options = app?.options
        TRACE(INFO) {
            "T3 WRITE ping -> path=games/$forcedGameId/messages  projectId=${options?.projectId ?: "-"} appId=${options?.applicationId ?: "-"} (dbUrl=${options?.databaseUrl ?: "-"})  {fromId=$fromIdFixed toId=$toIdFixed}" // ✨
        }

        colRef.add(payload)
            .addOnSuccessListener { ref ->
                TRACE(INFO) { "WRITE ok id=${ref.id} path=games/$forcedGameId/messages/${ref.id}" } // ✨
            }
            .addOnFailureListener { e ->
                TRACE(ERROR) { "sendPing failed to=$toIdFixed error=${e.message}" } // ✨
            }
    }
}


/**
 * Lightweight Firestore-based messaging hub used by GameManager.
 * - Keeps UI ignorant of networking details.
 * - All messages go to games/{gameId}/messages.
 * - Listener receives messages addressed to the local player (toId = myPlayerId).
 *
 * Hardening in this version:
 *  - Single active connection (game) at a time.
 *  - All sends go to the currently connected gameId (callers cannot accidentally send to a different room).
 *  - Aggressive traces (T0..T4) that prove path, filters, change counts, and write targets.
 */


/*****
 * NORMAL VERSION START
 */
/*
object NetworkManager {

    // Single live listener
    private var registration: ListenerRegistration? = null

    // Active connection context
    @Volatile private var activeGameId: String? = null
    @Volatile private var activePlayerId: String? = null

    data class InboundMessage(
        val type: String,
        val fromId: String,
        val text: String? = null
    )

    /**
     * Start listening for messages addressed to 'myPlayerId' in the given game room.
     *
     * Firestore path: games/{gameId}/messages
     * Message schema:
     *  - type: String ("chat" | "ping" | ...)
     *  - fromId: String
     *  - toId: String
     *  - text: String? (optional)
     *  - createdAt: FieldValue.serverTimestamp()
     */
    fun connectToGame(
        gameId: String,
        myPlayerId: String,
        onMessage: (InboundMessage) -> Unit
    ) {


        // Replace any previous listener
        registration?.remove()
        registration = null

        activeGameId = gameId
        activePlayerId = myPlayerId

        val app = try { FirebaseApp.getInstance() } catch (_: Exception) { null }
        val options = app?.options
        TRACE(INFO) {
            "T0 Firebase: projectId=${options?.projectId ?: "-"} appId=${options?.applicationId ?: "-"} (dbUrl=${options?.databaseUrl ?: "-"})"
        }

        val db = FirebaseFirestore.getInstance()
        val colRef = db.collection("games")
            .document(gameId)
            .collection("messages")

        TRACE(INFO) {
            "T1 Listening at path=games/$gameId/messages filter: toId == $myPlayerId"
        }

        // One-shot audit: show how many docs are currently in the collection (useful to prove room alignment)
        colRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { snap ->
                val count = snap.size()
                TRACE(INFO) { "T2 AUDIT: $count docs currently in games/$gameId/messages" }
            }
            .addOnFailureListener { e ->
                TRACE(WARNING) { "T2 AUDIT failed: ${e.message}" }
            }

        // Live listener for messages addressed to me in this room
        registration = colRef
            .whereEqualTo("toId", myPlayerId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->

                // Drop events if we got disconnected/reconnected to another room meanwhile
                val stillGameId = activeGameId
                if (stillGameId != gameId) {
                    TRACE(WARNING) {
                        "Listener event for stale room: eventGameId=$gameId activeGameId=$stillGameId (ignored)"
                    }
                    return@addSnapshotListener
                }

                if (error != null) {
                    TRACE(ERROR) { "Listener error: ${error.message}" }
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    TRACE(WARNING) { "Listener snapshot is null" }
                    return@addSnapshotListener
                }

                TRACE(DEBUG) {
                    "T4 snapshot changes=${snapshot.documentChanges.size} ${
                        snapshot.documentChanges.joinToString(prefix = "[", postfix = "]") { it.type.name }
                    } (fromCache=${snapshot.metadata.isFromCache} pendingWrites=${snapshot.metadata.hasPendingWrites()})"
                }

                if (snapshot.documentChanges.isEmpty()) {
                    TRACE(VERBOSE) { "Listener tick: no changes" }
                }

                for (change in snapshot.documentChanges) {
                    if (change.type != DocumentChange.Type.ADDED) {
                        TRACE(VERBOSE) { "Ignoring change type=${change.type} id=${change.document.id}" }
                        continue
                    }
                    val data = change.document.data
                    val type = data["type"] as? String ?: run {
                        TRACE(WARNING) { "Missing 'type' field in ${change.document.id}" }
                        continue
                    }
                    val fromId = data["fromId"] as? String ?: run {
                        TRACE(WARNING) { "Missing 'fromId' in ${change.document.id}" }
                        continue
                    }
                    val text = data["text"] as? String
                    TRACE(INFO) {
                        "Incoming message id=${change.document.id} type=$type from=$fromId text=${text ?: ""}"
                    }
                    onMessage(InboundMessage(type = type, fromId = fromId, text = text))
                }
            }
    }

    /** Stop listening and clear active room context. */
    fun disconnect() {
        if (registration != null) {
            TRACE(INFO) { "Detaching listener" }
        }
        registration?.remove()
        registration = null
        activeGameId = null
        activePlayerId = null
    }

    /**
     * Send a chat message to another player.
     * NOTE: The write always targets the currently connected room to prevent accidental cross-room sends.
     */
    fun sendChat(
        gameId: String,   // kept for source-compat; ignored if it mismatches the active room
        fromId: String,
        toId: String,
        text: String
    ) {
        val targetGameId = activeGameId
        if (targetGameId == null) {
            TRACE(ERROR) { "sendChat called while not connected (fromId=$fromId toId=$toId). Ignored." }
            return
        }
        if (gameId != targetGameId) {
            TRACE(ERROR) { "sendChat gameId mismatch: provided=$gameId connected=$targetGameId. Using connected room." }
        }

        val db = FirebaseFirestore.getInstance()
        val colRef = db.collection("games")
            .document(targetGameId)
            .collection("messages")

        val payload = hashMapOf(
            "type" to "chat",
            "fromId" to fromId,
            "toId" to toId,
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )

        val app = try { FirebaseApp.getInstance() } catch (_: Exception) { null }
        val options = app?.options
        TRACE(INFO) {
            "T3 WRITE chat -> path=games/$targetGameId/messages  projectId=${options?.projectId ?: "-"} appId=${options?.applicationId ?: "-"} (dbUrl=${options?.databaseUrl ?: "-"})  {fromId=$fromId toId=$toId}"
        }

        colRef.add(payload)
            .addOnSuccessListener { ref ->
                TRACE(INFO) { "WRITE ok id=${ref.id} path=games/$targetGameId/messages/${ref.id}" }
            }
            .addOnFailureListener { e ->
                TRACE(ERROR) { "sendChat failed to=$toId error=${e.message}" }
            }
    }

    /**
     * Send a ping message (no text).
     * NOTE: The write always targets the currently connected room to prevent accidental cross-room sends.
     */
    fun sendPing(
        gameId: String,   // kept for source-compat; ignored if it mismatches the active room
        fromId: String,
        toId: String
    ) {
        val targetGameId = activeGameId
        if (targetGameId == null) {
            TRACE(ERROR) { "sendPing called while not connected (fromId=$fromId toId=$toId). Ignored." }
            return
        }
        if (gameId != targetGameId) {
            TRACE(ERROR) { "sendPing gameId mismatch: provided=$gameId connected=$targetGameId. Using connected room." }
        }

        val db = FirebaseFirestore.getInstance()
        val colRef = db.collection("games")
            .document(targetGameId)
            .collection("messages")

        val payload = hashMapOf(
            "type" to "ping",
            "fromId" to fromId,
            "toId" to toId,
            "createdAt" to FieldValue.serverTimestamp()
        )

        val app = try { FirebaseApp.getInstance() } catch (_: Exception) { null }
        val options = app?.options
        TRACE(INFO) {
            "T3 WRITE ping -> path=games/$targetGameId/messages  projectId=${options?.projectId ?: "-"} appId=${options?.applicationId ?: "-"} (dbUrl=${options?.databaseUrl ?: "-"})  {fromId=$fromId toId=$toId}"
        }

        colRef.add(payload)
            .addOnSuccessListener { ref ->
                TRACE(INFO) { "WRITE ok id=${ref.id} path=games/$targetGameId/messages/${ref.id}" }
            }
            .addOnFailureListener { e ->
                TRACE(ERROR) { "sendPing failed to=$toId error=${e.message}" }
            }
    }
}
*/ /* END OF NORMAL VERSION */