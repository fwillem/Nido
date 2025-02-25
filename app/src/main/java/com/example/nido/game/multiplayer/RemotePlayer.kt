package com.example.nido.game.multiplayer

import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.game.GameContext
import com.example.nido.data.model.Combination


class RemotePlayer(id: String, name: String, avatar: String) : Player(id, name, avatar, PlayerType.REMOTE) {
    override fun play(gameContext: GameContext): Combination? {
        return NetworkManager.receiveMove() // Waits for the move from the network
    }
}
