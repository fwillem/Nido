package com.example.nido.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.nido.data.model.PlayerType
import com.example.nido.data.SavedPlayer
import com.example.nido.utils.Constants.DATASTORE_NAME
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Extension property to get the DataStore instance
val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

object NidoPreferences {
    private val PLAYERS_KEY = stringPreferencesKey("players_json")
    private val POINT_LIMIT_KEY = intPreferencesKey("point_limit")

    // ----- Flow for Saved Players (nullable!) -----
    fun playersFlow(context: Context): Flow<List<SavedPlayer>?> =
        context.dataStore.data.map { prefs ->
            val json = prefs[PLAYERS_KEY]
            if (json.isNullOrBlank()) {
                TRACE(INFO) { "No players found in DataStore (key missing or blank)" }
                null
            } else {
                try {
                    val players = Json.decodeFromString<List<SavedPlayer>>(json)
                    TRACE(DEBUG) { "Decoded players from DataStore: $players" }
                    players
                } catch (e: Exception) {
                    TRACE(ERROR) { "Failed to decode players from DataStore: $e" }
                    null
                }
            }
        }

    // ----- Save Players -----
    suspend fun setPlayers(context: Context, players: List<SavedPlayer>) {
        val json = Json.encodeToString(players)
        context.dataStore.edit { prefs ->
            prefs[PLAYERS_KEY] = json
        }
        TRACE(DEBUG) { "Saved players to DataStore: $players" }
    }

    // ----- Flow for Point Limit (nullable!) -----
    fun pointLimitFlow(context: Context): Flow<Int?> =
        context.dataStore.data.map { prefs ->
            val value = prefs[POINT_LIMIT_KEY]
            if (value == null) {
                TRACE(INFO) { "No point limit found in DataStore (key missing)" }
            } else {
                TRACE(DEBUG) { "Loaded point limit from DataStore: $value" }
            }
            value
        }


    // ----- Save Point Limit -----
    suspend fun setPointLimit(context: Context, limit: Int) {
        context.dataStore.edit { prefs ->
            prefs[POINT_LIMIT_KEY] = limit
        }
        TRACE(DEBUG) { "Saved point limit to DataStore: $limit" }
    }
}
