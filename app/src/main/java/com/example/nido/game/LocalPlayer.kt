package com.example.nido.game.players

import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.game.GameContext

class LocalPlayer(id: String, name: String, avatar: String) : Player(id, name, avatar, PlayerType.LOCAL) {
    override fun play(gameContext: GameContext): Combination? {
        // 🛑 This should never be called directly, as local players choose moves via UI.
        throw UnsupportedOperationException("Local player must play via UI interaction.")
    }
}
