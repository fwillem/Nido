package com.example.nido.game

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import com.example.nido.data.model.*
import com.example.nido.game.rules.GameRules
import com.example.nido.events.AppEvent
import com.example.nido.game.ai.AIPlayer

/**
 * A fake implementation of IGameManager for UI previews and automated tests.
 */
class FakeGameManager : IGameManager {


    // ✅ Create static fake players
    private val dummyLocalPlayer = LocalPlayer(
        id = "1",
        name = "Alice",
        avatar = "",
        score = 80,
        hand = Hand(mutableStateListOf(
            Card(2, "RED"),
            Card(3, "RED"),
            Card(4, "GREEN")
        ))
    )

    private val dummyAIPlayer1 = AIPlayer(
        id = "2",
        name = "Bob",
        avatar = "",
        score = 60,
        hand = Hand(mutableStateListOf(
            Card(2, "BLUE"),
            Card(5, "MOCHA")
        ))
    )

    private val dummyAIPlayer2 = AIPlayer(
        id = "3",
        name = "Carol",
        avatar = "",
        score = 70,
        hand = Hand(mutableStateListOf(
            Card(3, "MOCHA"),
            Card(3, "GREEN")
        ))
    )

    // ✅ Create a fake game state for previews/tests
    private val _gameState = mutableStateOf(
        GameState(
            screen = GameScreens.PLAYING,
            pointLimit = 15,
            players = listOf(dummyLocalPlayer, dummyAIPlayer1, dummyAIPlayer2),
            startingPlayerIndex = 0,
            currentPlayerIndex = 0,
            currentCombinationOnMat = Combination(mutableStateListOf(Card(3, "MOCHA"), Card(3, "GREEN"))),
            discardPile = mutableStateListOf(Card(2, "ORANGE")),
            selectedCards = mutableStateListOf(),
            deck = mutableStateListOf(Card(7, "PINK")),
            skipCount = 0,
            soundOn = true,
            turnId = 1
        )
    )

    override val gameState: State<GameState>
        get() = _gameState

    override fun initialize(viewModel: GameViewModel) {
        // No-op: Fake implementation doesn't need a ViewModel.
    }

    override fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int) {
        // Simulate starting a new game by resetting _gameState
    }

    override fun startNewRound() {
        // Simulate round reset
    }

    override fun skipTurn() {
        // Simulate skipping a turn
    }

    override fun playCombination(selectedCards: List<Card>, cardToKeep: Card?): gameManagerMoveResult {
        return gameManagerMoveResult.NEXT_PLAYER  // Fake result
    }

    override fun processAIMove() {
        // Simulate AI making a move
    }

    override fun processSkip() {
        // Simulate a skip action
    }

    override fun withdrawCardsFromMat(cardsToWithdraw: List<Card>) {
        // Simulate withdrawing cards
    }

    override fun setDialogEvent(event: AppEvent) {
        // Simulate setting an event
    }

    override fun clearDialogEvent() {
        // Simulate clearing an event
    }

    override fun isValidMove(selectedCards: List<Card>): Boolean {
        return true // Fake validation logic
    }

    override fun isGameOver(): Boolean = false

    override fun getGameWinners(): List<Player> = listOf(dummyLocalPlayer)

    override fun getPlayerRankings(): List<Pair<Player, Int>> = listOf(
        dummyLocalPlayer to 1,
        dummyAIPlayer1 to 2,
        dummyAIPlayer2 to 3
    )

    override fun getPlayerHandScores(): List<Pair<Player, Int>> = listOf(
        dummyLocalPlayer to 80,
        dummyAIPlayer1 to 60,
        dummyAIPlayer2 to 70
    )

    override fun getCurrentPlayerHandSize(): Int = 3

    override fun isCurrentPlayerLocal(): Boolean = true

    override fun currentPlayerHasValidCombination(): Boolean = true

    override fun updatePlayerHand(playerIndex: Int, newHand: Hand) {
        _gameState.value = _gameState.value.copy(
            players = _gameState.value.players.mapIndexed { index, player ->
                if (index == playerIndex) player.copy(hand = newHand) else player
            }
        )

    }

}
