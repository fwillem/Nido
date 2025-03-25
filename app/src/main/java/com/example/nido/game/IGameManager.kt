package com.example.nido.game

import com.example.nido.data.model.Hand

interface IGameManager : IGameStateProvider, IGameActions {
    fun updatePlayerHand(playerIndex: Int, hand: Hand)
}
