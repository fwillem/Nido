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



data class ReducerResult(
    val newState: GameState,
    val followUpEvents: List<GameEvent> = emptyList()
)

fun gameReducer(state: GameState,  event: GameEvent): ReducerResult {

    var reducerResult = ReducerResult(state)

    when (event) {

        is GameEvent.NewRoundStarted -> {
            return handleNewRoundStarted(state)
        }
        is GameEvent.CardPlayed -> {
            // In the reducer, inside CardPlayed branch
            TRACE(DEBUG) { "AI DONT PLAY Reducer: CardPlayed by ${event.playerId}, setting playmat to ${event.playedCards}" }
            return handleCardPlayed(state, event.playedCards, event.cardKept)
         }
        is GameEvent.NextTurn -> {
            return handleNextTurn(state)
          }
        is GameEvent.PlayerSkipped -> {
            return handlePlayerSkipped(state)
        }
        is GameEvent.RoundEnded -> {

        }
        is GameEvent.GameEnded -> {
        }

        is GameEvent.ShowDialog -> {
            // Usually, you don't want to change state in reducer for UI dialogs.
            // Just return the current state.
        }
    }
    return reducerResult
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

    // Determine the initial turn state based on player type.
    val initialTurnState = when {
        currentPlayer.playerType == com.example.nido.data.model.PlayerType.AI ->
            TurnState.AIProcessing
        currentPlayer.playerType == com.example.nido.data.model.PlayerType.REMOTE ->
            TurnState.RemoteProcessing
        else -> TurnState.WaitingForSelection
    }
    val turnInfo = TurnInfo(
        state = initialTurnState,
        canSkip = false,      // First player must play.
        canGoAllIn = false    // At the first turn, user cannot go all in.
    )

    // Rebuild the state with updated values.
    var newState = state.copy(
        players = clearedPlayers,
        deck = mutableDeck,
        currentCombinationOnMat = Combination(mutableStateListOf()),
        discardPile = mutableStateListOf(),
        selectedCards = mutableStateListOf(), // Clear any selected cards.
        skipCount = 0,
        startingPlayerIndex = newStartingPlayerIndex,
        currentPlayerIndex = newStartingPlayerIndex,
        turnId = state.turnId + 1,
        gamePhase = GamePhase.Round(
            RoundPhase.PlayerTurn(
                playerId = currentPlayer.id,
                turnInfo = turnInfo
            )
        )
    )
    // Deal cards to each player.
    newState = dealCards(newState)
    TRACE(INFO) { "🆕 New round started: $newState" }

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
                    player.hand.cards.size
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
                val updatedState = state.copy(
                    players = updatedPlayers,
                    currentCombinationOnMat = newCombination,
                    discardPile = newDiscardPile,
                    skipCount = 0
                )

                // 🟢 Add a NextTurn follow-up event
                followUpEvents += GameEvent.NextTurn

            }

    // 🟢 Return the resul. I cannot return updat with new state and follow-up events
    return ReducerResult(updatedState, followUpEvents)
}


private fun handlePlayerSkipped(gameState: GameState ) : ReducerResult
{

        val player = gameState.players[gameState.currentPlayerIndex]

        TRACE(DEBUG) { "${player.name} is skipping turn" }
        val newSkipCount = gameState.skipCount + 1

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
            val updatedState = gameState.copy(
                currentCombinationOnMat = Combination(mutableListOf()),
                discardPile = newDiscardPile,
                skipCount = 0,
            )

            return ReducerResult(newState = updatedState, followUpEvents = listOf(GameEvent.NextTurn))

        } else {

            // We just update the new skipcount
            val updatedState = gameState.copy(skipCount = newSkipCount)
            return ReducerResult(newState = updatedState, followUpEvents = listOf(GameEvent.NextTurn))
        }
}

private fun dealCards(gameState: GameState): GameState {
    val mutableDeck = gameState.deck.toMutableList()
    val mutablePlayers = gameState.players.map { player ->
        val updatedHand = player.hand.copy()
        var copyCount = 0;
        println("PNB PNB_DEAL player = $player, deck size is ${mutableDeck.size}")
        repeat(Constants.HAND_SIZE) {
            if (mutableDeck.isNotEmpty()) {
                val card = mutableDeck.removeAt(0)
                copyCount++
                println("PNB Card dealt: $card , ($copyCount)")

                updatedHand.addCard(card)
            } else {
                TRACE(FATAL) { "Deck is empty before dealing all cards!" }
            }
        }
        player.copy(hand = updatedHand)
    }

    // Trace each player's name and their hand
    mutablePlayers.forEach { player ->

        TRACE(INFO) { "$player.name's hand:" + player.hand.cards.joinToString(", ") { card -> "${card.value} ${card.color}" } }
    }

    return gameState.copy(
        players = mutablePlayers,
        deck = mutableStateListOf<Card>().apply { addAll(mutableDeck) }
    )
}

private fun handleNextTurn(gameState: GameState ) : ReducerResult
{
    val nextIndex = (gameState.currentPlayerIndex + 1) % gameState.players.size

    val newState = gameState.copy(
        currentPlayerIndex = nextIndex,
        turnId = gameState.turnId + 1
    )

    val nextPlayer = newState.players[nextIndex]
    TRACE(DEBUG) { "Player is now ${nextPlayer.name}($nextIndex)" }

    return ReducerResult(newState)
}

