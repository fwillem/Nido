package com.example.nido.game
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.game.events.GameEvent
import com.example.nido.utils.Constants
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.DEBUG
import com.example.nido.utils.TraceLogLevel.VERBOSE
import com.example.nido.utils.TraceLogLevel.FATAL
import com.example.nido.utils.TraceLogLevel.INFO
import com.example.nido.data.model.Hand
import com.example.nido.data.repository.DeckRepository
import com.example.nido.events.AppEvent
import com.example.nido.game.rules.GameRules
import com.example.nido.data.model.PlayerType



data class ReducerResult(
    val newState: GameState,
    val followUpEvents: List<GameEvent> = emptyList()
)

fun gameReducer(state: GameState, event: GameEvent): ReducerResult {
    return when (event) {
        is GameEvent.NewRoundStarted -> handleNewRoundStarted(state)
        is GameEvent.CardPlayed -> handleCardPlayed(state, event.playedCards, event.cardKept)
        is GameEvent.NextTurn -> handleNextTurn(state)
        is GameEvent.PlayerSkipped -> handlePlayerSkipped(state)
        is GameEvent.RoundOver -> {
            // No-op for now; reserved for future use.
            ReducerResult(state)
        }
        is GameEvent.GameOver -> {
            // No-op for now; reserved for future use.
            ReducerResult(state)
        }
        is GameEvent.ShowDialog -> {
            // No state change for UI events.
            ReducerResult(state)
        }
    }
}
private fun handleNewRoundStarted(state: GameState) : ReducerResult {

    //  Generate the deck
    val deck = DeckRepository.generateDeck(shuffle = true, nbOfPlayers = state.players.size)
    TRACE(VERBOSE) { "Brand new deck generated!(${deck.size}): $deck" }

    val mutableDeck = mutableStateListOf<Card>().apply { addAll(deck) }

    // Determine new starting player index.
    val newStartingPlayerIndex = (state.startingPlayerIndex + 1) % state.players.size
    // Clear players' hands.
    val clearedPlayers = state.players.map { it.copy(hand = Hand()) }

    // Identify the starting player.
    val currentPlayer = clearedPlayers[newStartingPlayerIndex]

    val turnInfo = TurnInfo()

    // Rebuild the state with updated values.
    var newState = state.copy(
        players = clearedPlayers,
        deck = mutableDeck,
        currentCombinationOnMat = Combination(mutableStateListOf()),
        discardPile = mutableStateListOf(),
        skipCount = 0,
        startingPlayerIndex = newStartingPlayerIndex,
        currentPlayerIndex = newStartingPlayerIndex,
        turnId = state.turnId + 1,
        playerId = currentPlayer.id,
        turnInfo = turnInfo
    )
    // Deal cards to each player.
    newState = dealCards(newState)
    TRACE(VERBOSE) { "New round started: $newState" }

    return ReducerResult(newState = newState)
}

private fun handleCardPlayed(state: GameState, selectedCards: List<Card>, cardToKeep: Card?) : ReducerResult
{


    // Create the new combination based on selected cards.
            val currentCombination = state.currentCombinationOnMat
            val newCombination = Combination(selectedCards.toMutableList())
            val player = state.players[state.currentPlayerIndex]


            // Validate the move.
            if (!GameRules.isValidMove(
                    currentCombination,
                    newCombination,
                    player.hand.cards
                )
            ) {
                TRACE(FATAL) { "Invalid combination! Move rejected." } // THis shall not happen here since it has been checked before in MatView
                return ReducerResult(state)
            }


            /**
             * Update the current player's hand by removing the played cards (note that for human players, card has already been removed by HandView)
             */

            val updatedHand = player.hand.copy().apply { removeCombination(newCombination) }
            val updatedPlayers = state.players.toMutableList().apply {
                this[state.currentPlayerIndex] = player.copy(hand = updatedHand)
            }

            val followUpEvents = mutableListOf<GameEvent>()
            var newState = state

            // We need to figure out here if the player won
            if (GameRules.hasPlayerWonTheRound(updatedHand)) {
                TRACE(INFO) { "${player.name} is playing: $newCombination " }
                TRACE(INFO) { "😍 ${player.name}  😎 won! " }

                /**
                 * The player won the round !
                 * Update the scores
                 * Understand if the game is over
                 */
                GameRules.updatePlayersScores(updatedPlayers)
                val gameOver = GameRules.isGameOver(updatedPlayers, state.pointLimit)

                if (gameOver) {

                    TRACE(INFO) { "Game is over! 🍾" }
                    TRACE(INFO) { "SetDialogEvent GameOver" }

                    // Add a ShowDialog follow-up event for game over
                    followUpEvents += GameEvent.ShowDialog(
                        AppEvent.GameEvent.GameOver(
                            playerRankings = GameRules.getPlayerRankings(updatedPlayers)
                        )
                    )
                    followUpEvents += GameEvent.GameOver

                } else {
                    TRACE(INFO) { "SetDialogEvent RoundOver" }

                    followUpEvents += GameEvent.ShowDialog(
                        AppEvent.GameEvent.RoundOver(
                            winner = player,
                            playersHandScore = GameRules.getPlayerHandScores(updatedPlayers)
                        )
                    )

                }
                // Update newState with updated players (and any round/game state if needed)
                newState = state.copy(players = updatedPlayers)

            } else {
                // Build a new discard pile:
                // It consists of the existing discard pile plus the cards from the current combination
                // excluding the card chosen by the player to keep.
                val discardedCards = currentCombination.cards.filter { it != cardToKeep }

                val newDiscardPile = mutableStateListOf<Card>().apply {
                    addAll(state.discardPile)
                    addAll(discardedCards)
                }

                TRACE(INFO) { "${player.name} is playing: $newCombination and is keeping: $cardToKeep, $discardedCards moves to discard pile" }


                // If a card was chosen to keep, add it back to the player's hand.
                cardToKeep?.let { updatedHand.addCard(it) }

                // Update the game state.
                newState = state.copy(
                    players = updatedPlayers,
                    currentCombinationOnMat = newCombination,
                    discardPile = newDiscardPile,
                    skipCount = 0
                )

                // 🟢 Add a NextTurn follow-up event
                followUpEvents += GameEvent.NextTurn

            }

    // 🟢 Return the resul. I cannot return updat with new state and follow-up events
    return ReducerResult(newState, followUpEvents)
}


private fun handlePlayerSkipped(gameState: GameState ) : ReducerResult
{

        val player = gameState.players[gameState.currentPlayerIndex]

        TRACE(DEBUG) { "${player.name} is skipping turn" }
        val newSkipCount = gameState.skipCount + 1
        var updatedState = gameState

        //
        if (newSkipCount >= (gameState.players.size - 1)) {
            // All players have skipped: discard the current playmat
            TRACE(INFO) { "All players but one skipped! Discarding current playmat , ${player.name} will restart." }

            val discardedCards = gameState.currentCombinationOnMat.cards
            val newDiscardPile = mutableStateListOf<Card>().apply {
                addAll(gameState.discardPile)
                addAll(discardedCards)
            }
            // Reset currentCombinationOnMat and skipCount, but keep currentPlayerIndex unchanged.
            updatedState = gameState.copy(
                currentCombinationOnMat = Combination(mutableListOf()),
                discardPile = newDiscardPile,
                skipCount = 0,
            )
        } else {

            // We just update the new skipcount
            updatedState = gameState.copy(skipCount = newSkipCount)

        }

    return ReducerResult(newState = updatedState, followUpEvents = listOf(GameEvent.NextTurn))

}

private fun dealCards(gameState: GameState): GameState {
    val mutableDeck = gameState.deck.toMutableList()
    val mutablePlayers = gameState.players.map { player ->
        val updatedHand = player.hand.copy()
        var copyCount = 0;
        repeat(Constants.HAND_SIZE) {
            if (mutableDeck.isNotEmpty()) {
                val card = mutableDeck.removeAt(0)
                copyCount++

                updatedHand.addCard(card)
            } else {
                TRACE(FATAL) { "Deck is empty before dealing all cards!" }
            }
        }
        player.copy(hand = updatedHand)
    }

    /*
    // Trace each player's name and their hand
    mutablePlayers.forEach { player ->
        TRACE(VERBOSE) { "$player.name's hand:" + player.hand.cards.joinToString(", ") { card -> "${card.value} ${card.color}" } }
    }
     */

    return gameState.copy(
        players = mutablePlayers,
        deck = mutableStateListOf<Card>().apply { addAll(mutableDeck) }
    )
}

private fun handleNextTurn(gameState: GameState): ReducerResult {
    val nextIndex = (gameState.currentPlayerIndex + 1) % gameState.players.size

    // Compute new player and phase
    val newState = gameState.copy(
        currentPlayerIndex = nextIndex,
        turnId = gameState.turnId + 1
    )

    val nextPlayer = newState.players[nextIndex]

    // 🟢 Set the correct TurnPhase based on player type and doNotAutoPlayAI
    val newTurnPhase = when (nextPlayer.playerType) {
        PlayerType.LOCAL -> TurnPhase.WaitingForLocal(nextPlayer.name)
        PlayerType.AI -> TurnPhase.WaitingForAI(
            name = nextPlayer.name,
            isAutomatic = !gameState.doNotAutoPlayAI // read flag from GameState!
        )
        PlayerType.REMOTE -> TurnPhase.WaitingForRemote(nextPlayer.name)
    }

    TRACE(DEBUG) { "Player is now ${nextPlayer.name}($nextIndex), turnPhase set to $newTurnPhase" }

    // 🟢 Copy turnPhase into the new state
    return ReducerResult(newState.copy(turnPhase = newTurnPhase))
}