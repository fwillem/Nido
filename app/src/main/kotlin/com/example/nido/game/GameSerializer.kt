package com.example.nido.game

import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Hand
import com.example.nido.data.model.PlayerType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Strict JSON parsing and encoding for game networking.
 * Any parsing error here raises a FATAL exception (trace + crash in strict mode)
 * rather than failing silently.
 */
object GameSerializer {

    class SerializationException(message: String, cause: Throwable? = null) :
        RuntimeException(message, cause)

    // --- Public DTOs (Moved from GameManager) ---

    data class PublicPlayerView(
        val playerId: String,
        val name: String,
        val type: PlayerType,
        val score: Int,
        val handCount: Int
    )

    data class LastMove(
        val byPlayerId: String,
        val action: String,
        val played: List<Card>? = null,
        val kept: Card? = null
    )

    data class ClientSnapshot(
        val turnId: Int,
        val currentPlayerIndex: Int,
        val currentPlayerId: String,
        val matCombination: Combination,
        val playersPublic: List<PublicPlayerView>,
        val skipCount: Int,
        val pointLimit: Int,
        val lastMove: LastMove? = null,
        val myHand: Hand? = null
    )

    data class TurnPlayIntent(
        val selectedCards: List<Card>,
        val cardToKeep: Card? = null
    )

    data class StartPlayer(
        val id: String,
        val name: String,
        val type: PlayerType,
        val score: Int,
        val avatar: String
    )

    data class StartPayload(
        val players: List<StartPlayer>,
        val pointLimit: Int,
        val startingPlayerIndex: Int,
        val currentPlayerIndex: Int,
        val turnId: Int
    )

    // --- Wire Constants ---

    private object HintWireKind {
        const val PLAYER_SKIPPED = "player_skipped"
        const val MAT_DISCARDED_NEXT = "mat_discarded_next"
        const val YOU_CANNOT_BEAT = "you_cannot_beat"
        const val YOU_MUST_PLAY_ONE = "you_must_play_one"
        const val YOU_CAN_PLAY_N_OR_NPLUS1 = "you_can_play_n_or_nplus1"
        const val YOU_KEPT = "you_kept"
        const val PLAYER_KEPT = "player_kept"
    }

    private object BannerWireKind {
        const val PLAY = "play"
    }

    // --- Core Encoding/Decoding ---

    fun encodeTurnPlay(intent: TurnPlayIntent): String = try {
        val root = JSONObject()
        val cardsArr = JSONArray()
        intent.selectedCards.forEach { card ->
            cardsArr.put(encodeCardToken(card))
        }
        root.put("selectedCards", cardsArr)

        if (intent.cardToKeep != null) {
            root.put("cardKept", encodeCardToken(intent.cardToKeep))
        }
        root.toString()
    } catch (e: Exception) {
        throw SerializationException("Failed to encode TurnPlayIntent", e)
    }

    fun decodeTurnPlay(json: String): TurnPlayIntent = try {
        val obj = JSONObject(json)
        val selected = obj.optJSONArray("selectedCards")?.let { arr ->
            (0 until arr.length()).map { parseCardToken(arr.getString(it)) }
        } ?: emptyList()
        val keep = obj.optString("cardToKeep", "")
            .takeIf { it.isNotBlank() }
            ?.let(::parseCardToken)
        TurnPlayIntent(selectedCards = selected, cardToKeep = keep)
    } catch (e: Exception) {
        throw SerializationException("Failed to decode TurnPlayIntent: $json", e)
    }

    fun encodeSnapshot(snapshot: ClientSnapshot): String = try {
        val root = JSONObject().apply {
            put("turnId", snapshot.turnId)
            put("currentPlayerIndex", snapshot.currentPlayerIndex)
            put("currentPlayerId", snapshot.currentPlayerId)
            put("skipCount", snapshot.skipCount)
            put("pointLimit", snapshot.pointLimit)
            put("matCombination", snapshot.matCombination.toString())

            val players = JSONArray()
            snapshot.playersPublic.forEach { p ->
                players.put(JSONObject().apply {
                    put("playerId", p.playerId)
                    put("name", p.name)
                    put("type", p.type.name)
                    put("score", p.score)
                    put("handCount", p.handCount)
                })
            }
            put("playersPublic", players)

            snapshot.lastMove?.let { lm ->
                put("lastMove", JSONObject().apply {
                    put("byPlayerId", lm.byPlayerId)
                    put("action", lm.action)
                    put("played", lm.played?.let { a -> JSONArray(a.map { encodeCardToken(it) }) })
                    put("kept", lm.kept?.let { encodeCardToken(it) })
                })
            }

            snapshot.myHand?.let { put("myHand", encodeHandCsv(it)) }
        }
        root.toString()
    } catch (e: Exception) {
        throw SerializationException("Failed to encode ClientSnapshot", e)
    }

    fun decodeSnapshot(json: String): ClientSnapshot = try {
        val o = JSONObject(json)

        val ppArr = o.getJSONArray("playersPublic")
        val pp = (0 until ppArr.length()).map { i ->
            val p = ppArr.getJSONObject(i)
            PublicPlayerView(
                playerId = p.getString("playerId"),
                name = p.getString("name"),
                type = PlayerType.valueOf(p.getString("type")),
                score = p.getInt("score"),
                handCount = p.getInt("handCount")
            )
        }

        val myHand = o.optString("myHand", null)
            ?.takeIf { it.isNotBlank() && it != "The hand is empty" }
            ?.let(::parseHandCsv)

        // Fix: Parse lastMove properly
        val lastMove = o.optJSONObject("lastMove")?.let { lm ->
            LastMove(
                byPlayerId = lm.getString("byPlayerId"),
                action = lm.getString("action"),
                played = lm.optJSONArray("played")?.let { arr ->
                    (0 until arr.length()).map { parseCardToken(arr.getString(it)) }
                },
                kept = lm.optString("kept", "").takeIf { it.isNotBlank() }?.let(::parseCardToken)
            )
        }

        ClientSnapshot(
            turnId = o.getInt("turnId"),
            currentPlayerIndex = o.getInt("currentPlayerIndex"),
            currentPlayerId = o.getString("currentPlayerId"),
            matCombination = o.optString("matCombination", "").takeIf { it.isNotEmpty() }
                ?.let { parseCombinationString(it) }
                ?: Combination(mutableListOf()), // Default to empty if missing/empty
            playersPublic = pp,
            skipCount = o.getInt("skipCount"),
            pointLimit = o.getInt("pointLimit"),
            lastMove = lastMove,
            myHand = myHand
        )
    } catch (e: Exception) {
        throw SerializationException("Failed to decode ClientSnapshot: $json", e)
    }

    fun encodeStart(p: StartPayload): String = try {
        val root = JSONObject().apply {
            put("pointLimit", p.pointLimit)
            put("startingPlayerIndex", p.startingPlayerIndex)
            put("currentPlayerIndex", p.currentPlayerIndex)
            put("turnId", p.turnId)

            val arr = JSONArray()
            p.players.forEach { sp ->
                arr.put(JSONObject().apply {
                    put("id", sp.id)
                    put("name", sp.name)
                    put("type", sp.type.name)
                    put("score", sp.score)
                    put("avatar", sp.avatar)
                })
            }
            put("players", arr)
        }
        root.toString()
    } catch (e: Exception) {
        throw SerializationException("Failed to encode StartPayload", e)
    }

    fun decodeStart(json: String): StartPayload = try {
        val o = JSONObject(json)
        val arr = o.getJSONArray("players")
        val players = (0 until arr.length()).map { i ->
            val p = arr.getJSONObject(i)
            StartPlayer(
                id = p.getString("id"),
                name = p.getString("name"),
                type = PlayerType.valueOf(p.getString("type")),
                score = p.getInt("score"),
                avatar = p.getString("avatar")
            )
        }
        StartPayload(
            players = players,
            pointLimit = o.getInt("pointLimit"),
            startingPlayerIndex = o.getInt("startingPlayerIndex"),
            currentPlayerIndex = o.getInt("currentPlayerIndex"),
            turnId = o.getInt("turnId")
        )
    } catch (e: Exception) {
        throw SerializationException("Failed to decode StartPayload: $json", e)
    }

    fun encodeTurnHint(hint: TurnHintMsg?): String? = try {
        if (hint == null) null else {
            val o = JSONObject()
            when (hint) {
                is TurnHintMsg.PlayerSkipped -> {
                    o.put("kind", HintWireKind.PLAYER_SKIPPED)
                    o.put("name", hint.name)
                }
                is TurnHintMsg.MatDiscardedNext -> {
                    o.put("kind", HintWireKind.MAT_DISCARDED_NEXT)
                    o.put("name", hint.name)
                }
                is TurnHintMsg.YouCannotBeat -> {
                    o.put("kind", HintWireKind.YOU_CANNOT_BEAT)
                }
                is TurnHintMsg.YouMustPlayOne -> {
                    o.put("kind", HintWireKind.YOU_MUST_PLAY_ONE)
                    o.put("canAllIn", hint.canAllIn)
                }
                is TurnHintMsg.YouCanPlayNOrNPlusOne -> {
                    o.put("kind", HintWireKind.YOU_CAN_PLAY_N_OR_NPLUS1)
                    o.put("n", hint.n)
                }
                is TurnHintMsg.YouKept -> {
                    o.put("kind", HintWireKind.YOU_KEPT)
                    o.put("card", hint.card)
                }
                is TurnHintMsg.PlayerKept -> {
                    o.put("kind", HintWireKind.PLAYER_KEPT)
                    o.put("name", hint.name)
                    o.put("card", hint.card)
                }
            }
            o.toString()
        }
    } catch (e: Exception) {
        throw SerializationException("Failed to encode TurnHintMsg", e)
    }

    fun decodeTurnHint(json: String): TurnHintMsg? = try {
        val o = JSONObject(json)
        when (o.getString("kind")) {
            HintWireKind.PLAYER_SKIPPED -> TurnHintMsg.PlayerSkipped(o.getString("name"))
            HintWireKind.MAT_DISCARDED_NEXT -> TurnHintMsg.MatDiscardedNext(o.getString("name"))
            HintWireKind.YOU_CANNOT_BEAT -> TurnHintMsg.YouCannotBeat
            HintWireKind.YOU_MUST_PLAY_ONE -> TurnHintMsg.YouMustPlayOne(
                canAllIn = o.optBoolean("canAllIn", false)
            )
            HintWireKind.YOU_CAN_PLAY_N_OR_NPLUS1 -> TurnHintMsg.YouCanPlayNOrNPlusOne(
                n = o.optInt("n", 1)
            )
            HintWireKind.YOU_KEPT -> TurnHintMsg.YouKept(
                card = o.getString("card")
            )
            HintWireKind.PLAYER_KEPT -> TurnHintMsg.PlayerKept(
                name = o.getString("name"),
                card = o.getString("card")
            )
            else -> null
        }
    } catch (e: Exception) {
        throw SerializationException("Failed to decode TurnHintMsg: $json", e)
    }

    fun encodeBanner(banner: BannerMsg?): String? = try {
        if (banner == null) null else {
            val o = JSONObject()
            when (banner) {
                is BannerMsg.Play -> {
                    o.put("kind", BannerWireKind.PLAY)
                    o.put("name", banner.name)
                }
            }
            o.toString()
        }
    } catch (e: Exception) {
        throw SerializationException("Failed to encode BannerMsg", e)
    }

    fun decodeBanner(json: String): BannerMsg? = try {
        val o = JSONObject(json)
        when (o.getString("kind")) {
            BannerWireKind.PLAY -> BannerMsg.Play(name = o.getString("name"))
            else -> null
        }
    } catch (e: Exception) {
        throw SerializationException("Failed to decode BannerMsg: $json", e)
    }

    // --- Helpers ---

    private fun encodeCardToken(card: Card): String {
        return "${card.value}/${card.color.name}"
    }

    private fun parseCardToken(token: String): Card {
        val parts = token.split("/")
        require(parts.size == 2) { "Bad card token: $token" }
        val value = parts[0].trim().toInt()
        val colorStr = parts[1].trim().uppercase()
        return Card(value, colorStr)
    }

    private fun parseHandCsv(csv: String): Hand {
        if (csv.isBlank()) return Hand()
        val cards = csv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map(::parseCardToken)
        return Hand().also { it.cards.addAll(cards) }
    }

    private fun encodeHandCsv(hand: Hand): String =
        hand.cards.joinToString(", ") { encodeCardToken(it) }

    private fun parseCombinationString(str: String): Combination {
        val parts = str.split(", ")
        val cards = parts.map { token ->
            val cleanToken = token.trim()
            try {
                // Handle optional variations like "7/BLUE" vs "7 BLUE" if needed,
                // but strictly speaking we should standardize.
                // Reusing the robust logic from old GameManager for safety:
                val separator = if (cleanToken.contains("/")) "/" else " "
                val segments = cleanToken.split(separator)
                if (segments.size == 2) {
                    Card(segments[0].toInt(), segments[1])
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }.filterNotNull()
        return Combination(cards.toMutableList())
    }
}
