package com.example.nido.game
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.game.GameEvent
import com.example.nido.utils.Constants
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.DEBUG
import com.example.nido.utils.TraceLogLevel.VERBOSE
import com.example.nido.utils.TraceLogLevel.FATAL
import com.example.nido.utils.TraceLogLevel.INFO
import com.example.nido.data.model.Hand
import com.example.nido.data.repository.DeckRepository
import com.example.nido.game.rules.GameRules
import com.example.nido.data.model.PlayerType
import com.example.nido.events.GameDialogEvent
import com.example.nido.utils.TraceLogLevel


data class ReducerResult(
    val newState: GameState,
    val followUpEvents: List<GameEvent> = emptyList(),
    val sideEffects: List<GameSideEffect> = emptyList()
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
        is GameEvent.AITimerExpired -> handleAITimerExpired(state, event.turnId)
        else -> {
            TRACE(FATAL) { "Unhandled event: $event" }
            ReducerResult(state) // Return the current state if no valid event is matched.
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
    val sideEffects = mutableListOf<GameSideEffect>()

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

    // Reset the turn hint.
    newState = dealCards(newState)
    val resetState = newState.copy(
        lastActivePLayer = null,
        lastKeptCard = null
    )
    val newStateWithHint = resetState.copy(turnHint = buildTurnHint(resetState))

    // Is the current player an AI and autoPLauy is enabled we shall start the AI timer.

    if (currentPlayer.playerType == PlayerType.AI && !newStateWithHint.doNotAutoPlayAI) {
        TRACE(DEBUG) { "New Round, first player is AI: ${currentPlayer.name}" }
        sideEffects += GameSideEffect.StartAITimer(newStateWithHint.turnId)
    }
    else {
        TRACE(DEBUG) { "New Round, first  player is human: ${currentPlayer.name}" }
    }

    return ReducerResult(newState = newStateWithHint,sideEffects = sideEffects)
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
            val sideEffects = mutableListOf<GameSideEffect>()

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
                    TRACE(INFO) { "Side effect Show Dialog and Game Event GameOver" }

                    // Add a ShowDialog side effect for game over
                    sideEffects += GameSideEffect.ShowDialog(
                        GameDialogEvent.GameOver(
                            playerRankings = GameRules.getPlayerRankings(updatedPlayers)
                        )
                    )

                    // Add a GameEvent for game over
                    followUpEvents += GameEvent.GameOver

                } else {
                    TRACE(INFO) { "SetDialogEvent RoundOver" }

                    sideEffects += GameSideEffect.ShowDialog(
                        GameDialogEvent.RoundOver(
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
    return ReducerResult(newState, followUpEvents, sideEffects)
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
        var copyCount = 0
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
    val sideEffects = mutableListOf<GameSideEffect>()

    val nextIndex = (gameState.currentPlayerIndex + 1) % gameState.players.size

    // Compute new player and phase
    val newState = gameState.copy(
        currentPlayerIndex = nextIndex,
        turnId = gameState.turnId + 1
    )

    val nextPlayer = newState.players[nextIndex]

    // if the next player is the AI, we need to either launch a timer or display the Manual Play button
    if (nextPlayer.playerType == PlayerType.AI) {
        if (gameState.doNotAutoPlayAI) {
            // Nothing to do
        } else {
            sideEffects += GameSideEffect.StartAITimer(newState.turnId)
        }
    }

    TRACE(DEBUG) { "Player is now ${nextPlayer.name}($nextIndex)" }

    // 🟢 Copy turnPhase into the new state
    return ReducerResult(newState, sideEffects = sideEffects    )
}

private fun handleAITimerExpired(state: GameState, turnId: Int): ReducerResult {

    val sideEffects = mutableListOf<GameSideEffect>()

    // Check if the turnId matches the current
    if (state.turnId == turnId ) {

        // Need to get the AI move
        sideEffects += GameSideEffect.GetAIMove
    } else {
        // If the turnId does not match, we ignore this event.
        TRACE(TraceLogLevel.ERROR) { "AITimerExpired for wrong turnId (possible but not necessarily an error): event turnId=$turnId, current turnId=${state.turnId}" }
    }


    return ReducerResult(state, sideEffects = sideEffects)
}

/** "YOU" if player is the local human, otherwise the player's name. */
/* TODO TO LOCALIZE */
private fun displayNameFor(state: GameState, playerId: String?): String {
    if (playerId == null) return ""
    return if (playerId == state.playerId) "YOU" else {
        state.players.firstOrNull { it.id == playerId }?.name ?: ""
    }
}

/** Baseline instruction: A) must play 1  OR  B) can play N+ (N or N+1). */
private fun baselineTurnHint(state: GameState): String {
    // N is the size of the current combo on the mat; if empty, N = 1.
    val n = state.currentCombinationOnMat.cards.size.takeIf { it > 0 } ?: 1

    // If the starting player must open a round, cannot skip.
    // We lean on TurnInfo.canSkip when available; default false if not maintained.
    val mustPlay = !state.turnInfo.canSkip && state.currentCombinationOnMat.cards.isEmpty()

    return if (mustPlay) {
        "You must play 1"
    } else {
        "You can play ${n}+"
    }
}

/** Optional suffix for "kept this card". Shown as: " — Thorstein kept PINK 8". */
private fun keptSuffix(state: GameState): String {
    val actor = state.lastActivePLayer?.id ?: return ""
    val kept = state.lastKeptCard ?: return ""
    val who = displayNameFor(state, actor)
    return " — $who kept ${kept.color} ${kept.value}"
}

/** Final hint. */
private fun buildTurnHint(state: GameState): String {
    val base = baselineTurnHint(state)
    val kept = keptSuffix(state)
    return base + kept
}
