package com.example.nido.data

import androidx.compose.runtime.mutableStateListOf
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.game.LocalPlayer
import com.example.nido.game.ai.AIPlayer
import com.example.nido.game.multiplayer.RemotePlayer

import kotlinx.serialization.Serializable

@Serializable
data class SavedPlayer(
    val name: String,
    val avatar: String,
    val playerType: PlayerType
) {
    // Convert SavedPlayer back to a real Player instance
    fun toPlayer(id: String): Player =
        when (playerType) {
            PlayerType.LOCAL -> LocalPlayer(
                id = id,
                name = name,
                avatar = avatar,
                score = 0, // or your desired default
                hand = com.example.nido.data.model.Hand(mutableStateListOf()),
                isLocallyManaged = true
            )
            PlayerType.AI -> AIPlayer(
                id = id,
                name = name,
                avatar = avatar,
                score = 0, // or your desired default
                hand = com.example.nido.data.model.Hand(mutableStateListOf()),
                isLocallyManaged = true
            )
            PlayerType.REMOTE -> RemotePlayer(
                id = id,
                name = name,
                avatar = avatar,
                score = 0, // or your desired default
                hand = com.example.nido.data.model.Hand(mutableStateListOf()),
                isLocallyManaged = false
            )
        }

    companion object {
        // Convert from a Player to a SavedPlayer
        fun fromPlayer(player: Player): SavedPlayer = SavedPlayer(
            name = player.name,
            avatar = player.avatar,
            playerType = player.playerType
        )
    }
}
