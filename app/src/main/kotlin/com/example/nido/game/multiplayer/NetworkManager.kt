package com.example.nido.game.multiplayer

import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_BANNER_MSG
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_CHAT
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_PING
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_STATE_SYNC
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_TURN_HINT
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_TURN_PLAY
import com.example.nido.game.multiplayer.MessageTypes.MSG_TYPE_TURN_SKIP
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldPath


/**
 * 🔗 NetworkManager — Firestore-based, symmetric message bus
 *
 * - Path: games/{gameId}/messages
 * - Listens only to messages addressed to me (toId == myPlayerId)
 * - Single active connection at a time to avoid cross-room leaks
 * - Stable ordering: createdAt ASC, documentId ASC
 * - No dev hard-codes; callers control room & IDs (use Firebase UID for players)
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
     * 🎧 Connect to a game room and start receiving messages addressed to me.
     *
     * @param gameId Firestore document id of the room (games/{gameId})
     * @param myPlayerId The local player's id (👉 use Firebase.auth.currentUser!!.uid)
     * @param onMessage Callback for newly added messages to me
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

        val app = runCatching { FirebaseApp.getInstance() }.getOrNull()
        val opts = app?.options
        TRACE(WARNING) { "🟡 T0 Firebase: projectId=${opts?.projectId ?: "-"} appId=${opts?.applicationId ?: "-"} gameID=$gameId (dbUrl=${opts?.databaseUrl ?: "-"})" }

        val db = FirebaseFirestore.getInstance()
        val colRef = db.collection("games")
            .document(gameId)
            .collection("messages")

        TRACE(WARNING) { "🟡 T1 Listening at path=games/$gameId/messages filter: toId == $myPlayerId" }

        // One-shot audit (handy for room alignment)
        colRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { snap ->
                TRACE(WARNING) { "🟡 T2 AUDIT: ${snap.size()} docs currently in games/$gameId/messages" }
            }
            .addOnFailureListener { e ->
                TRACE(WARNING) { "🟡 T2 AUDIT failed: ${e.message}" }
            }

        // Live listener: messages addressed to me
        registration = colRef
            .whereEqualTo("toId", myPlayerId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .orderBy(FieldPath.documentId()) // tie-breaker for equal timestamps
            .addSnapshotListener { snapshot, error ->

                // Ignore if we switched rooms meanwhile
                val stillActive = activeGameId
                if (stillActive != gameId) {
                    TRACE(WARNING) { "⚠️ Stale event: eventGameId=$gameId activeGameId=$stillActive (ignored)" }
                    return@addSnapshotListener
                }

                if (error != null) {
                    TRACE(ERROR) { "❌ Listener error: ${error.message}" }
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    TRACE(WARNING) { "⚠️ Listener snapshot is null" }
                    return@addSnapshotListener
                }

                TRACE(WARNING) {
                    val kinds = snapshot.documentChanges.joinToString(prefix = "[", postfix = "]") { it.type.name }
                    "🟡 T4 snapshot changes=${snapshot.documentChanges.size} $kinds (fromCache=${snapshot.metadata.isFromCache} pendingWrites=${snapshot.metadata.hasPendingWrites()})"
                }

                if (snapshot.documentChanges.isEmpty()) {
                    TRACE(WARNING) { "… tick (no new messages)" }
                }

                for (change in snapshot.documentChanges) {
                    if (change.type != DocumentChange.Type.ADDED) {
                        TRACE(WARNING) { "Ignoring change type=${change.type} id=${change.document.id}" }
                        continue
                    }
                    val data = change.document.data
                    val type = data["type"] as? String ?: run {
                        TRACE(WARNING) { "Missing 'type' in ${change.document.id}" }
                        continue
                    }
                    val fromId = data["fromId"] as? String ?: run {
                        TRACE(WARNING) { "Missing 'fromId' in ${change.document.id}" }
                        continue
                    }
                    val text = data["text"] as? String
                    TRACE(WARNING) { "📥 Incoming id=${change.document.id} type=$type from=$fromId text=${text ?: ""}" }
                    onMessage(InboundMessage(type = type, fromId = fromId, text = text))
                }
            }
    }

    /** 📴 Disconnect and clear active context. */
    fun disconnect() {
        if (registration != null) TRACE(WARNING) { "🔌 Detaching listener" }
        registration?.remove()
        registration = null
        activeGameId = null
        activePlayerId = null
    }


    // ---- internals ---------------------------------------------------------

    private fun send(
        type: String,
        gameId: String,
        fromId: String,
        toId: String,
        text: String?
    ) {
        val targetGameId = activeGameId
        val me = activePlayerId
        if (targetGameId == null || me == null) {
            TRACE(ERROR) { "❌ send($type) while not connected (fromId=$fromId toId=$toId). Ignored." }
            return
        }
        if (gameId != targetGameId) {
            TRACE(WARNING) { "⚠️ send($type) gameId mismatch: provided=$gameId connected=$targetGameId. Using connected room." }
        }
        if (fromId != me) {
            // Not fatal, but helps catch accidental misuse
            TRACE(WARNING) { "⚠️ fromId ($fromId) != activePlayerId ($me). Proceeding." }
        }

        val db = FirebaseFirestore.getInstance()
        val colRef = db.collection("games").document(targetGameId).collection("messages")

        val payload = hashMapOf(
            "type" to type,
            "fromId" to fromId,
            "toId" to toId,
            "createdAt" to FieldValue.serverTimestamp()
        ).apply {
            if (text != null) put("text", text)
        }

        val app = runCatching { FirebaseApp.getInstance() }.getOrNull()
        val opts = app?.options
        TRACE(WARNING) { "🟡 T3 WRITE $type -> games/$targetGameId/messages  {fromId=$fromId toId=$toId} projectId=${opts?.projectId ?: "-"} appId=${opts?.applicationId ?: "-"}" }

        colRef.add(payload)
            .addOnSuccessListener { ref ->
                TRACE(WARNING) { "✅ WRITE ok id=${ref.id} path=games/$targetGameId/messages/${ref.id}" }
            }
            .addOnFailureListener { e ->
                TRACE(ERROR) { "❌ send($type) failed to=$toId error=${e.message}" }
            }
    }

    // --- Add in NetworkManager.kt (near sendPing/sendChat) ---


    /**
     * 💬 Send a chat message to another player.
     * Always targets the currently connected room to prevent cross-room writes.
     */
    fun sendChat(
        gameId: String,   // kept for source-compat; ignored if it mismatches the active room
        fromId: String,
        toId: String,
        text: String
    ) {
        send(type = MSG_TYPE_CHAT, gameId = gameId, fromId = fromId, toId = toId, text = text)
    }

    /**
     * 📡 Send a ping (no text).
     * Always targets the currently connected room to prevent cross-room writes.
     */
    fun sendPing(
        gameId: String,   // kept for source-compat; ignored if it mismatches the active room
        fromId: String,
        toId: String
    ) {
        send(type = MSG_TYPE_PING, gameId = gameId, fromId = fromId, toId = toId, text = null)
    }

    /** Host receives guest's play intent. Payload lives in `text` as JSON. */
    fun sendTurnPlay(
        gameId: String,
        fromId: String,
        toId: String,
        text: String
    ) {
        send(type = MSG_TYPE_TURN_PLAY, gameId = gameId, fromId = fromId, toId = toId, text = text)
    }

    /** Host receives guest's skip intent. No body needed. */
    fun sendTurnSkip(
        gameId: String,
        fromId: String,
        toId: String
    ) {
        send(type = MSG_TYPE_TURN_SKIP, gameId = gameId, fromId = fromId, toId = toId, text = null)
    }

    /** Host → guest authoritative snapshot after the play resolves. */
    fun sendStateSync(
        gameId: String,
        fromId: String,
        toId: String,
        text: String
    ) {
        send(type = MSG_TYPE_STATE_SYNC, gameId = gameId, fromId = fromId, toId = toId, text = text)
    }

    // UI-only, light payloads
    fun sendTurnHint(gameId: String, fromId: String, toId: String, text: String) =
        send(type = MSG_TYPE_TURN_HINT, gameId = gameId, fromId = fromId, toId = toId, text = text)

    fun sendBannerMsg(gameId: String, fromId: String, toId: String, text: String) =
        send(type = MSG_TYPE_BANNER_MSG, gameId = gameId, fromId = fromId, toId = toId, text = text)

}
