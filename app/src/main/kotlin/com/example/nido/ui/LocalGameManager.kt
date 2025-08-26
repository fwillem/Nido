package com.example.nido.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.nido.game.IGameManager
import com.example.nido.game.getGameManagerInstance
import com.example.nido.game.GameViewModel
import com.example.nido.data.model.Card
import com.example.nido.data.model.Player
import com.example.nido.data.model.Hand
import com.example.nido.events.AppDialogEvent
import com.example.nido.events.GameDialogEvent
import com.example.nido.game.MusicCommand
import com.example.nido.game.SoundEffect
import com.example.nido.game.UiNotice
import com.example.nido.utils.Debug

val LocalGameManager = staticCompositionLocalOf<IGameManager> {
    object : IGameManager {
        override fun initialize(viewModel: GameViewModel) {
            getGameManagerInstance().initialize(viewModel)
        }
        override val gameState get() = getGameManagerInstance().gameState
        override fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int, doNotAutoPlayAI: Boolean, aiTimerDuration: Int) =
            getGameManagerInstance().startNewGame(selectedPlayers, selectedPointLimit, doNotAutoPlayAI, aiTimerDuration)
        override fun startNewRound() = getGameManagerInstance().startNewRound()
        override fun skipTurn() = getGameManagerInstance().skipTurn()
        override fun playCombination(selectedCards: List<Card>, cardToKeep: Card?) =
            getGameManagerInstance().playCombination(selectedCards, cardToKeep)
        override fun getAIMove() = getGameManagerInstance().getAIMove()
        override fun processSkip() = getGameManagerInstance().processSkip()

        override fun isGameOver() = getGameManagerInstance().isGameOver()
        override fun getGameWinners() = getGameManagerInstance().getGameWinners()
        override fun getPlayerRankings() = getGameManagerInstance().getPlayerRankings()
        override fun getPlayerHandScores() = getGameManagerInstance().getPlayerHandScores()
        override fun getCurrentPlayerHandSize() = getGameManagerInstance().getCurrentPlayerHandSize()
        override fun isCurrentPlayerLocal() = getGameManagerInstance().isCurrentPlayerLocal()
        override fun currentPlayerHasValidCombination() = getGameManagerInstance().currentPlayerHasValidCombination()
        override fun isValidMove(selectedCards: List<Card>) = getGameManagerInstance().isValidMove(selectedCards)
        override fun hasPlayedAllRemainingCards() = getGameManagerInstance().hasPlayedAllRemainingCards()


        override fun updatePlayerHand(playerIndex: Int, hand: Hand) = getGameManagerInstance().updatePlayerHand(playerIndex, hand)
        override fun setAppDialogEvent(event: AppDialogEvent) = getGameManagerInstance().setAppDialogEvent(event)
        override fun clearAppDialogEvent() = getGameManagerInstance().clearAppDialogEvent()

        override fun setGameDialogEvent(event: GameDialogEvent) = getGameManagerInstance().setGameDialogEvent(event)
        override fun clearGameDialogEvent() = getGameManagerInstance().clearGameDialogEvent()

        override fun consumeSound(effect: SoundEffect) = getGameManagerInstance().consumeSound(effect)
        override fun consumeMusic(cmd: MusicCommand) = getGameManagerInstance().consumeMusic(cmd)

        override fun consumeNotice(notice: UiNotice) = getGameManagerInstance().consumeNotice(notice)

        override fun chatWithRemotePlayer(remotePlayerId: String, text: String) = getGameManagerInstance().chatWithRemotePlayer(remotePlayerId, text)
        override fun pingTestPeerIfPossible() = getGameManagerInstance().pingTestPeerIfPossible()
    }
}
