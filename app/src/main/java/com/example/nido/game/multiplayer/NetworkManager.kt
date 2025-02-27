package com.example.nido.game.multiplayer
import com.example.nido.data.model.Combination
import com.example.nido.utils.TestData

object NetworkManager {
    fun sendMove(playerId: String, move: Combination) {
        // TODO: Implement network sending
    }

    fun receiveMove(): Combination {
        // TODO: Implement receiving logic
        return(TestData.generateDummyCombination())
    }
}
