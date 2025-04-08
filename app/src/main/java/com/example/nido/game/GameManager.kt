package com.example.nido.game

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.data.repository.DeckRepository
import com.example.nido.game.rules.GameRules
import com.example.nido.utils.Constants
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.data.model.PlayerActionType
import com.example.nido.data.model.Hand
import com.example.nido.events.AppEvent
import kotlin.Int
import com.example.nido.game.events.GameEvent


enum class gameManagerMoveResult {
    ROUND_OVER,
    GAME_OVER,
    NEXT_PLAYER,
    INVALID_MOVE
}

private object GameManager : IGameManager {
    private var gameViewModel: GameViewModel? = null
    override val gameState: State<GameState>
        get() = getViewModel().gameState


    override fun initialize(viewModel: GameViewModel) {
        if (gameViewModel != null) {
            // Already initialized; the user may have changed the orientation of the phone or moved the app in background
            TRACE(DEBUG) { "GameManager already initialized; reattaching." }
            return
        }
        gameViewModel = viewModel
    }

    private fun getViewModel(): GameViewModel {
        return gameViewModel
            ?: throw IllegalStateException("GameManager has not been initialized!") // 🚨 Prevent usage before initialization
    }


    /**
     * Overall game initialization.
     * This function now only sets up the base state (players, point limit, deck, startingIndex) without dealing cards.
     * Then it calls startNewRound() to handle round-specific initialization.
     */
    override fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int) {
        TRACE(DEBUG) { "selectedPlayers: $selectedPlayers, selectedPointLimit: $selectedPointLimit" }

        //  Generate the deck
        val deck = DeckRepository.generateDeck(shuffle = true, nbOfPlayers = selectedPlayers.size)
        TRACE(VERBOSE) { "Brand new deck generated!: $deck" }

        // Choose a random starting player.
        // TODO For debug we will simplify, the right value is :  val startingPlayerIndex = (0 until selectedPlayers.size).random()
        val startingPlayerIndex = -1

        val initializedPlayers =
            GameRules.initializePlayerScores(selectedPlayers, selectedPointLimit)


        // Set up initial state
        val initialState = GameState(

            players = initializedPlayers,

            pointLimit = selectedPointLimit,
            deck = mutableStateListOf<Card>().apply { addAll(deck) },
            startingPlayerIndex = startingPlayerIndex,
            currentPlayerIndex = startingPlayerIndex,
            currentCombinationOnMat = Combination(mutableListOf()),
            discardPile = mutableStateListOf(),
            skipCount = 0,
            gamePhase = GamePhase.Idle
        )


        // Update the state without dealing.
        getViewModel().updateGameState(initialState)
        TRACE(INFO) { "Initial gameState set: ${getViewModel().gameState}" }

        // Start the first round (this will shuffle the deck and deal cards).
        startNewRound()
    }

    /**
     * Starts a new round.
     * 🔄 Reshuffles the existing deck (no need to re-create it) and deals cards to each player.
     * 🏁 Updates startingIndex to be the next player after the one who started the previous round.
     */
    override fun startNewRound() {
        TRACE(DEBUG) { "startNewRound()" }
        dispatchEvent(GameEvent.NewRoundStarted)
    }


    private fun getCurrentPlayer(): Player =
        gameState.value.players[gameState.value.currentPlayerIndex]

    override fun skipTurn() {
        TRACE(DEBUG) { "skipTurn()" }
        dispatchEvent(GameEvent.PlayerSkipped)



    }

    /**
     * Play a combination of cards.Returns true if the player won.
     */
    override fun playCombination(
        selectedCards: List<Card>,
        cardToKeep: Card?
    ): gameManagerMoveResult {
        val currentGameState = gameState.value
        var moveResult: gameManagerMoveResult = gameManagerMoveResult.NEXT_PLAYER

        if (selectedCards.isEmpty()) {
            TRACE(FATAL) { " No cards selected" }
            return gameManagerMoveResult.INVALID_MOVE
        }

        // Create the new combination based on selected cards.
        val currentCombination = currentGameState.currentCombinationOnMat
        val newCombination = Combination(selectedCards.toMutableList())

        // Validate the move.
        if (!GameRules.isValidMove(
                currentCombination,
                newCombination,
                getCurrentPlayer().hand.cards.size
            )
        ) {
            TRACE(FATAL) { "Invalid combination! Move rejected." } // THis shall not happen here since it has been checked before in MatView
            return gameManagerMoveResult.INVALID_MOVE
        }


        /**
         * Update the current player's hand by removing the played cards (note that for human players, card has already been removed by HandView)
         */

        val updatedHand = getCurrentPlayer().hand.copy().apply { removeCombination(newCombination) }
        val updatedPlayers = currentGameState.players.toMutableList().apply {
            this[currentGameState.currentPlayerIndex] = getCurrentPlayer().copy(hand = updatedHand)
        }

        // We need to figure out here if the player won
        if (GameRules.hasPlayerWonTheRound(updatedHand)) {
            TRACE(INFO) { "${getCurrentPlayer().name} is playing: $newCombination " }
            TRACE(INFO) { "😍 ${getCurrentPlayer().name}  😎 won! " }

            /**
             * The player won the round !
             * Update the scores
             * Understand if the game is over
             */
            GameRules.updatePlayersScores(updatedPlayers)
            val gameOver = GameRules.isGameOver(updatedPlayers, currentGameState.pointLimit)

            if (gameOver) {

                TRACE(INFO) { "Game is over! 🍾" }
                TRACE(INFO) { "SetDialogEvent GameOver" }

                GameManager.setDialogEvent(
                    AppEvent.GameEvent.GameOver(
                        playerRankings = GameRules.getPlayerRankings(updatedPlayers),
                    )
                )

                // The game is over..
                moveResult = gameManagerMoveResult.GAME_OVER

            } else {
                TRACE(INFO) { "SetDialogEvent RoundOver" }

                GameManager.setDialogEvent(
                    AppEvent.GameEvent.RoundOver(
                        winner = getCurrentPlayer(),
                        playersHandScore = GameRules.getPlayerHandScores(updatedPlayers),
                    )
                )

                // TRACE(INFO) { "We need to Start a new round" }
                // startNewRound() start new round is now called in the handling of the GameEvent

                // Round is over
                moveResult = gameManagerMoveResult.ROUND_OVER
            }

        } else {
            // Build a new discard pile:
            // It consists of the existing discard pile plus the cards from the current combination
            // excluding the card chosen by the player to keep.
            val discardedCards = currentCombination.cards.filter { it != cardToKeep }

            val newDiscardPile = mutableStateListOf<Card>().apply {
                addAll(currentGameState.discardPile)
                addAll(discardedCards)
            }

            TRACE(INFO) { "${getCurrentPlayer().name} is playing: $newCombination and is keeping: $cardToKeep, $discardedCards moves to discard pile" }


            // If a card was chosen to keep, add it back to the player's hand.
            cardToKeep?.let { updatedHand.addCard(it) }

            // Update the game state.
            val updatedState = currentGameState.copy(
                players = updatedPlayers,
                currentCombinationOnMat = newCombination,
                discardPile = newDiscardPile,
                skipCount = 0
            )

            getViewModel().updateGameState(updatedState)

            nextTurn()

            moveResult = gameManagerMoveResult.NEXT_PLAYER
        }

        return moveResult
    }


    private fun nextTurn() {
        val currentGameState = gameState.value
        val nextIndex = (currentGameState.currentPlayerIndex + 1) % currentGameState.players.size

        val updatedState = currentGameState.copy(
            currentPlayerIndex = nextIndex,
            turnId = currentGameState.turnId + 1
        )


        getViewModel().updateGameState(updatedState) // ✅ Safe access to ViewModel

        val nextPlayer = updatedState.players[nextIndex]
        TRACE(DEBUG) { "Player is now ${nextPlayer.name}($nextIndex)" }

    }

    override fun processAIMove() {
        val currentPlayer = getCurrentPlayer()
        if (currentPlayer.playerType == PlayerType.AI) {
            TRACE(DEBUG) { "AI is playing (${currentPlayer.name})" }
            handleAIMove(currentPlayer)
        } else {
            TRACE(ERROR) { "Not AI's turn!" }
        }
    }

    override fun processSkip() {
        val currentPlayer = getCurrentPlayer()
        if (currentPlayer.playerType == PlayerType.AI) {
            TRACE(WARNING) { " AI not supposed to skip via this function" }
        } else {
            TRACE(INFO) { "Local player ${currentPlayer.name} skips" }
            skipTurn()
        }
    }

    private fun handleAIMove(aiPlayer: Player) {
        val playerAction = aiPlayer.play(this)

        if (playerAction.actionType == PlayerActionType.PLAY) {
            // Check if combination is null; if so, log a fatal error.
            if (playerAction.combination == null) {
                TRACE(FATAL) { "Combination cannot be null when actionType is PLAY for ${aiPlayer.name}" }
            } else {
                TRACE(DEBUG) { "${aiPlayer.name} is playing: ${playerAction.combination} and is keeping: ${playerAction.cardToKeep}" }
                // The non-null assertion (!!) is now safe because TRACE(FATAL) will throw if combination is null.
                val playMoveResult =
                    playCombination(playerAction.combination!!.cards, playerAction.cardToKeep)

                // Here we need to test is game is ended
                if (playMoveResult == gameManagerMoveResult.GAME_OVER) {
                    TRACE(DEBUG, tag = "handleAIMove") { "Game Over" }
                    // onEndGame() not done here, will be done upon reception of the GAME_OVER event
                }
            }

        } else {
            TRACE(INFO) { "${aiPlayer.name} has no move !" }
            skipTurn()
        }
    }

    override fun isValidMove(selectedCards: List<Card>): Boolean {
        val currentCombination = gameState.value.currentCombinationOnMat
        val selectedCombination = Combination(selectedCards.toMutableList())
        return GameRules.isValidMove(
            currentCombination,
            selectedCombination,
            getCurrentPlayer().hand.cards.size
        )
    }

    override fun isGameOver(): Boolean {
        return GameRules.isGameOver(gameState.value.players, gameState.value.pointLimit)
    }

    override fun getGameWinners(): List<Player> {
        return GameRules.getGameWinners(gameState.value.players)
    }

    override fun getPlayerRankings(): List<Pair<Player, Int>> {
        return GameRules.getPlayerRankings(gameState.value.players)
    }

    override fun getPlayerHandScores(): List<Pair<Player, Int>> {
        return GameRules.getPlayerHandScores(gameState.value.players)
    }

    override fun getCurrentPlayerHandSize(): Int {
        return getCurrentPlayer().hand.cards.size
    }

    override fun isCurrentPlayerLocal(): Boolean {
        return getCurrentPlayer().playerType == PlayerType.LOCAL
    }

    // This function checks if the player is able to make a valid move
    // In this current implementation, the function needs to check the union of cards in the selectedcard and the hand
    override fun currentPlayerHasValidCombination(): Boolean {
        // Consider both the remaining hand and the selected cards.
        val fullHand = getCurrentPlayer().hand.cards.toMutableList().apply {
            addAll(gameState.value.selectedCards)
        }

        // Find all possible valid combinations from the full hand.
        val possibleMoves: List<Combination> = GameRules.findValidCombinations(fullHand)

        // Get the current combination on the playmat.
        val playmatCombination = gameState.value.currentCombinationOnMat

        // Look for a valid move that beats the current playmat combination.
        return (possibleMoves.find {
            GameRules.isValidMove(
                playmatCombination,
                it,
                fullHand.size
            )
        } != null)
    }


    override fun withdrawCardsFromMat(cardsToWithdraw: List<Card>) {
        val currentGameState = gameState.value
        val currentPlayer = getCurrentPlayer()

        TRACE(DEBUG) { "Withdrawing cards $cardsToWithdraw from ${currentPlayer.name}'s hand." }

        // Create a copy of the current player's hand and add back the withdrawn cards.
        val updatedHand = currentPlayer.hand.copy()
        cardsToWithdraw.forEach { card ->
            updatedHand.addCard(card)
        }
        val updatedPlayer = currentPlayer.copy(hand = updatedHand)

        // Update the players list with the updated player.
        val updatedPlayers = currentGameState.players.toMutableList().apply {
            this[currentGameState.currentPlayerIndex] = updatedPlayer
        }

        // Withdraw the cards from selectedCards.
        // We create a new mutable list and remove the withdrawn cards.
        val updatedSelectedCards = mutableStateListOf<Card>().apply {
            addAll(currentGameState.selectedCards)
        }
        updatedSelectedCards.removeAll(cardsToWithdraw)

        // Create the new game state with the updated players and selectedCards.
        val updatedState = currentGameState.copy(
            players = updatedPlayers,
            selectedCards = updatedSelectedCards
        )

        getViewModel().updateGameState(updatedState)
        TRACE(DEBUG) { "Withdrawn cards ${cardsToWithdraw.joinToString()} returned to ${currentPlayer.name}'s hand." }
    }

    override fun setDialogEvent(event: AppEvent) {
        val currentState = gameState.value
        getViewModel().updateGameState(currentState.copy(gameEvent = event))
    }

    override fun clearDialogEvent() {
        val currentState = gameState.value
        getViewModel().updateGameState(currentState.copy(gameEvent = null))
    }

    override fun updatePlayerHand(playerIndex: Int, hand: Hand) {
        val currentGameState = gameState.value

        // Ensure the playerIndex is valid
        if (playerIndex in currentGameState.players.indices) {
            // Update the player's hand
            val updatedPlayers = currentGameState.players.mapIndexed { index, player ->
                if (index == playerIndex) player.copy(hand = hand) else player
            }

            // Apply the new state
            getViewModel().updateGameState(currentGameState.copy(players = updatedPlayers))

            TRACE(DEBUG) { "✅ Updated Player($playerIndex) hand: ${hand.cards}" }
        } else {
            TRACE(ERROR) { "⚠️ Invalid playerIndex: $playerIndex" }
        }
    }

    private fun dispatchEvent(event: GameEvent) {
        val currentState = gameState.value
        val result = gameReducer(currentState, event)
        getViewModel().updateGameState(result.newState)
        // Optionally: Process any follow-up events
        result.followUpEvents.forEach { followUp ->
            dispatchEvent(followUp)
        }
    }
}

// Internal helper function to expose GameManager as IGameManager within the module.
internal fun getGameManagerInstance(): IGameManager = GameManager



