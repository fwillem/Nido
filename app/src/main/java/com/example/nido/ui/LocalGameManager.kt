package com.example.nido.ui


import android.app.GameManager
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.nido.game.GameViewModel
import com.example.nido.game.IGameManager

val LocalGameManager = staticCompositionLocalOf<IGameManager> {
    object : IGameManager {
        override fun initialize(viewModel: GameViewModel) {
            GameManager.initialize(viewModel)
        }

        override val gameState get() = GameManager.gameState
        override fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int) = GameManager.startNewGame(selectedPlayers, selectedPointLimit)
        override fun startNewRound() = GameManager.startNewRound()
        override fun skipTurn() = GameManager.skipTurn()
        override fun playCombination(selectedCards: List<Card>, cardToKeep: Card?) = GameManager.playCombination(selectedCards, cardToKeep)
        override fun processAIMove() = GameManager.processAIMove()
        override fun processSkip() = GameManager.processSkip()
        override fun withdrawCardsFromMat(cardsToWithdraw: List<Card>) = GameManager.withdrawCardsFromMat(cardsToWithdraw)
        override fun setDialogEvent(event: AppEvent) = GameManager.setDialogEvent(event)
        override fun clearDialogEvent() = GameManager.clearDialogEvent()
        override fun isGameOver() = GameManager.isGameOver()
        override fun getGameWinners() = GameManager.getGameWinners()
        override fun getPlayerRankings() = GameManager.getPlayerRankings()
        override fun getPlayerHandScores() = GameManager.getPlayerHandScores()
        override fun getCurrentPlayerHandSize() = GameManager.getCurrentPlayerHandSize()
        override fun isCurrentPlayerLocal() = GameManager.isCurrentPlayerLocal()
        override fun currentPlayerHasValidCombination() = GameManager.currentPlayerHasValidCombination()
    }
}
