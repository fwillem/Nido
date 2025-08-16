package com.example.nido.game

import com.example.nido.data.model.Card
import com.example.nido.data.model.Player
import com.example.nido.utils.Debug


interface IGameActions {
        fun initialize(viewModel: GameViewModel)
        fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int, doNotAutoPlayAI: Boolean)
        fun startNewRound()
        fun playCombination(selectedCards: List<Card>, cardToKeep: Card?)
        fun skipTurn()
        fun getAIMove()
        fun processSkip()
        fun updateComment(comment : String)
    }
