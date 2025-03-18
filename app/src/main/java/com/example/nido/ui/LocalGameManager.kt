package com.example.nido.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.nido.game.IGameManager
import com.example.nido.game.getGameManagerInstance
import com.example.nido.game.GameViewModel
import com.example.nido.data.model.Card
import com.example.nido.data.model.Player
import com.example.nido.events.AppEvent

val LocalGameManager = staticCompositionLocalOf<IGameManager> {
    object : IGameManager {
        override fun initialize(viewModel: GameViewModel) {
            getGameManagerInstance().initialize(viewModel)
        }
        override val gameState get() = getGameManagerInstance().gameState
        override fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int) =
            getGameManagerInstance().startNewGame(selectedPlayers, selectedPointLimit)
        override fun startNewRound() = getGameManagerInstance().startNewRound()
        override fun skipTurn() = getGameManagerInstance().skipTurn()
        override fun playCombination(selectedCards: List<Card>, cardToKeep: Card?) =
            getGameManagerInstance().playCombination(selectedCards, cardToKeep)
        override fun processAIMove() = getGameManagerInstance().processAIMove()
        override fun processSkip() = getGameManagerInstance().processSkip()
        override fun withdrawCardsFromMat(cardsToWithdraw: List<Card>) =
            getGameManagerInstance().withdrawCardsFromMat(cardsToWithdraw)
        override fun setDialogEvent(event: AppEvent) = getGameManagerInstance().setDialogEvent(event)
        override fun clearDialogEvent() = getGameManagerInstance().clearDialogEvent()
        override fun isGameOver() = getGameManagerInstance().isGameOver()
        override fun getGameWinners() = getGameManagerInstance().getGameWinners()
        override fun getPlayerRankings() = getGameManagerInstance().getPlayerRankings()
        override fun getPlayerHandScores() = getGameManagerInstance().getPlayerHandScores()
        override fun getCurrentPlayerHandSize() = getGameManagerInstance().getCurrentPlayerHandSize()
        override fun isCurrentPlayerLocal() = getGameManagerInstance().isCurrentPlayerLocal()
        override fun currentPlayerHasValidCombination() = getGameManagerInstance().currentPlayerHasValidCombination()
        override fun isValidMove(selectedCards: List<Card>) = getGameManagerInstance().isValidMove(selectedCards)

    }
}
