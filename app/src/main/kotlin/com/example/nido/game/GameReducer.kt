package com.example.nido.game
import androidx.compose.runtime.mutableStateListOf
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.utils.Constants
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.DEBUG
import com.example.nido.utils.TraceLogLevel.VERBOSE
import com.example.nido.utils.TraceLogLevel.FATAL
import com.example.nido.utils.TraceLogLevel.INFO
import com.example.nido.utils.TraceLogLevel.ERROR
import com.example.nido.data.model.Hand
import com.example.nido.data.model.Player
import com.example.nido.data.repository.DeckRepository
import com.example.nido.game.rules.GameRules
import com.example.nido.data.model.PlayerType
import com.example.nido.events.GameDialogEvent
import com.example.nido.game.SoundEffect
import com.example.nido.game.TurnHintMsg
import com.example.nido.game.BannerMsg
import com.example.nido.game.multiplayer.MultiplayerMode
import com.example.nido.game.rules.calculateTurnInfo
import com.example.nido.replay.GameRecorder


data class ReducerResult(
    val newState: GameState,
    val followUpEvents: List<GameEvent> = emptyList(),
    val sideEffects: List<GameSideEffect> = emptyList()
)

private fun GameState.withUpdatedCombination(
    combination: Combination,
    player: Player?
): GameState {
    val banner = if (player != null) {
        BannerMsg.Play(player.name)
    } else {
        null
    }

    return this.copy(
        currentCombinationOnMat = combination,
        bannerMsg = banner
    )
}




// Centralized refresh
private fun GameState.withUIRefresh(): GameState {
    val freshTurnInfo = calculateTurnInfo(this)
    val mode = this.multiplayerState?.mode

    return when (mode) {
        MultiplayerMode.JOINER -> {
            // Guest: UI refresh only; keep host-sent hint exactly as-is
            this.copy(turnInfo = freshTurnInfo)
        }
        MultiplayerMode.HOST, null -> {
            // Host or offline: hint is derived from state locally
            val currentPlayerType = this.players[this.currentPlayerIndex].playerType
            val freshHintMsg = buildTurnHint(this.copy(turnInfo = freshTurnInfo), currentPlayerType)
            this.copy(
                turnInfo = freshTurnInfo,
                turnHintMsg = freshHintMsg
            )
        }
    }
}


fun gameReducer(state: GameState, event: GameEvent): ReducerResult {
    val result = when (event) {
        is GameEvent.NewRoundStarted -> handleNewRoundStarted(state)
        is GameEvent.CardPlayed -> handleCardPlayed(state, event.playedCards, event.cardKept)
        is GameEvent.NextTurn -> handleNextTurn(state)
        is GameEvent.PlayerSkipped -> handlePlayerSkipped(state)
        is GameEvent.RoundOver -> ReducerResult(state) // reserved
        is GameEvent.GameOver -> ReducerResult(state)  // reserved
        is GameEvent.AITimerExpired -> handleAITimerExpired(state, event.turnId)
        is GameEvent.QuitGame -> handleQuitGame(state)
        is GameEvent.NetworkStateSync -> handleNetworkStateSync(state, event)
        is GameEvent.TurnHintReceived -> ReducerResult(state.copy(turnHintMsg = event.hint))
        is GameEvent.BannerReceived -> ReducerResult(state.copy(bannerMsg = event.banner))
    }

    // 📝 Record the event into replay history
    val currentPlayerId = state.players.getOrNull(state.currentPlayerIndex)?.id
    GameRecorder.record(event, playerId = currentPlayerId)

    if (event is GameEvent.GameOver || event is GameEvent.QuitGame) {
        GameRecorder.endSession()
    }

    // Systematic refresh after every reducer
    val refreshed = result.newState.withUIRefresh()

    // Build SFX from (prev, next, event) and append them
    val sfx = synthesizeSfx(state, refreshed, event)
    val mx  = synthesizeMusic(state, refreshed, event)

    return result.copy(
        newState = refreshed,
        sideEffects = result.sideEffects + sfx + mx
    )
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

    val sideEffects = mutableListOf<GameSideEffect>()

    // Rebuild the state with updated values. We use the helper for the banner/snackbar to be displayed in MatVIew
    var newState = state
        .copy(
            players = clearedPlayers,
            deck = mutableDeck,
            discardPile = mutableStateListOf(),
            skipCount = 0,
            startingPlayerIndex = newStartingPlayerIndex,
            currentPlayerIndex = newStartingPlayerIndex,
            turnId = state.turnId + 1,
            currentPlayerId = currentPlayer.id,
            lastPlayerWhoPlayed = null,
            lastPlayerWhoSkipped = null,
            lastKeptCard = null
        )
        .withUpdatedCombination(Combination(mutableStateListOf()), null)
    // Deal cards to each player.
    newState = dealCards(newState)
    TRACE(VERBOSE) { "New round started: $newState" }

    // Is the current player an AI and autoPLauy is enabled we shall start the AI timer.

    if (currentPlayer.playerType == PlayerType.AI && !newState.doNotAutoPlayAI) {
        TRACE(DEBUG) { "New Round, first player is AI: ${currentPlayer.name}" }
        sideEffects += GameSideEffect.StartAITimer(newState.turnId)
    }
    else {
        TRACE(DEBUG) { "New Round, first  player is human: ${currentPlayer.name}" }
    }

    return ReducerResult(newState = newState,sideEffects = sideEffects)
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

        val rankings = GameRules.getPlayerRankings(updatedPlayers)

        val localPlayerWonGame =
            rankings.isNotEmpty() && rankings.first().first.playerType == PlayerType.LOCAL

        if (gameOver) {

            TRACE(INFO) { "Game is over! 🍾" }
            TRACE(INFO) { "Side effect Show Dialog and Game Event GameOver" }


            sideEffects += GameSideEffect.ShowDialog(
                GameDialogEvent.GameOver(
                    playerRankings = rankings
                )
            )

            // Follow-up event now includes the outcome
            followUpEvents += GameEvent.GameOver(localPlayerWonGame)

        } else {
            TRACE(INFO) { "SetDialogEvent RoundOver" }

            sideEffects += GameSideEffect.ShowDialog(
                GameDialogEvent.RoundOver(
                    winner = player,
                    playersHandScore = GameRules.getPlayerHandScores(updatedPlayers)
                )
            )

            // We also need to trigger the RoundOver event for sound management
            followUpEvents += GameEvent.RoundOver(localPlayerWonGame)

        }
        // Update newState with updated players (and any round/game state if needed)
        newState = state.copy(
            players = updatedPlayers,
            lastPlayerWhoPlayed = player,   // who completed the winning play
            lastPlayerWhoSkipped = null,    // clear any "X skipped" hint
            lastKeptCard = null             // nothing was kept on a winning play
        )
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
        newState = state
            .copy(
                players = updatedPlayers,
                discardPile = newDiscardPile,
                skipCount = 0,
                lastPlayerWhoPlayed = player,
                lastPlayerWhoSkipped = null, // Reset the last player who skipped
                lastKeptCard = cardToKeep
            )
            .withUpdatedCombination(newCombination, player)


        // Add a NextTurn follow-up event
        followUpEvents += GameEvent.NextTurn

    }


    // Return the resul.
    return ReducerResult(newState, followUpEvents, sideEffects)
}


private fun handlePlayerSkipped(gameState: GameState ) : ReducerResult
{

    val player = gameState.players[gameState.currentPlayerIndex]

    TRACE(DEBUG) { "${player.name} is skipping turn" }
    val newSkipCount = gameState.skipCount + 1
    var newState = gameState

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
        newState = gameState
            .copy(
                discardPile = newDiscardPile,
                skipCount = 0,
                lastPlayerWhoSkipped = null // clear transient skip info on reset
            )
            .withUpdatedCombination(Combination(mutableListOf()), null)
    } else {

        // We just update the new skipcount
        newState = gameState
            .copy(
                skipCount = newSkipCount,
                lastPlayerWhoSkipped = player,
            )

    }

    return ReducerResult(newState = newState, followUpEvents = listOf(GameEvent.NextTurn))

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

    // 1. Basic Turn Update
    var newState = gameState.copy(
        currentPlayerIndex = nextIndex,
        turnId = gameState.turnId + 1
    )

    val nextPlayer = newState.players[nextIndex]

    // 2. Recalculate Derived State (Rules & Hints) for the NEW player
    val newTurnInfo = calculateTurnInfo(newState)
    
    // We must update the state with the new TurnInfo BEFORE building the hint
    // because buildTurnHint relies on state.turnInfo (e.g. for canSkip logic)
    newState = newState.copy(turnInfo = newTurnInfo)

    val newHint = buildTurnHint(newState, nextPlayer.playerType)
    val newBanner = BannerMsg.Play(nextPlayer.name)

    newState = newState.copy(
        turnHintMsg = newHint,
        bannerMsg = newBanner
    )

    // 3. Side Effects
    // if the next player is the AI, we need to either launch a timer or display the Manual Play button
    if (nextPlayer.playerType == PlayerType.AI) {
        if (gameState.doNotAutoPlayAI) {
            // Nothing to do
        } else {
            sideEffects += GameSideEffect.StartAITimer(newState.turnId)
        }
    }

    TRACE(DEBUG) { "Player is now ${nextPlayer.name}($nextIndex). Hint=$newHint" }


    return ReducerResult(newState, sideEffects = sideEffects)
}

private fun handleAITimerExpired(state: GameState, turnId: Int): ReducerResult {

    val sideEffects = mutableListOf<GameSideEffect>()

    // Check if the turnId matches the current
    if (state.turnId == turnId ) {

        // Need to get the AI move
        sideEffects += GameSideEffect.GetAIMove
    } else {
        // If the turnId does not match, we ignore this event.
        TRACE(ERROR) { "AITimerExpired for wrong turnId (possible but not necessarily an error): event turnId=$turnId, current turnId=${state.turnId}" }
    }


    return ReducerResult(state, sideEffects = sideEffects)
}


private fun handleQuitGame(state: GameState): ReducerResult {
    TRACE(INFO) { "Quit Game requested" }


    // Possible side effect: show dialog
    val sideEffects = listOf<GameSideEffect>(
        GameSideEffect.ShowDialog(
            GameDialogEvent.QuitGame
        )
    )
    return ReducerResult(state, sideEffects = sideEffects)
}

/**
 * Baseline instruction:
 * A) must play 1
 * OR
 * B) can play N+ (N or N+1).
 *
 * ⚠️ Assumes state.turnInfo is up-to-date.
 * Normally safe because reducer always refresh state via withUIRefresh().
 * If you call buildTurnHint() manually, ensure you pass a state with a fresh turnInfo,
 * e.g. by calling calculateTurnInfo(state) first.
 */
private fun baselineTurnHint(gameState: GameState,  currentPlayerType: PlayerType): TurnHintMsg? {
    val n = gameState.currentCombinationOnMat.cards.size.takeIf { it > 0 } ?: 1
    val lastPlayerWhoSkipped : Player? = gameState.lastPlayerWhoSkipped

    val matIsEmpty = gameState.currentCombinationOnMat.cards.isEmpty()
    val hasLastPlay : Player? = gameState.lastPlayerWhoPlayed
    val currentPlayer = gameState.players[gameState.currentPlayerIndex]



    if (currentPlayerType != PlayerType.LOCAL) {
        // In order order to make game lively, we describe what happens
        if (lastPlayerWhoSkipped != null) {
            return TurnHintMsg.PlayerSkipped(lastPlayerWhoSkipped.name)


        } else  if ((matIsEmpty) && (hasLastPlay != null))  {
            return TurnHintMsg.MatDiscardedNext(currentPlayer.name)
        }
        else return keptSuffix(gameState)

    }

    if (gameState.turnInfo.displaySkipCounter)
    {
        return TurnHintMsg.YouCannotBeat
    }

    return if (!gameState.turnInfo.canSkip) {
        TurnHintMsg.YouMustPlayOne(canAllIn = gameState.turnInfo.canGoAllIn)
    } else {
        TurnHintMsg.YouCanPlayNOrNPlusOne(n = n)
    }
}



/** Optional suffix for "kept this card".
 * Shown as: " — YOU kept PINK 8" or " — Thorstein kept PINK 8".
 */

private fun keptSuffix(state: GameState): TurnHintMsg? {
    val player = state.lastPlayerWhoPlayed ?: return null
    val kept = state.lastKeptCard ?: return null
    val cardKept = "${kept.value} ${kept.color}"

    if (player.playerType == PlayerType.LOCAL) {
        return TurnHintMsg.YouKept(cardKept)
    } else {
        return TurnHintMsg.PlayerKept(player.name, cardKept)
    }
}

/** Final hint assembled from baseline + kept suffix. */
private fun buildTurnHint(state: GameState, currentPlayerType: PlayerType): TurnHintMsg ? {
    val base = baselineTurnHint(state, currentPlayerType)
    //  val kept = keptSuffix(state, currentPlayerType)
    return base
}


/**
 * Effects listed here are considered "silent" for now.
 * If an effect is silent, we never enqueue a PlaySound for it.
 *
 * Keep this list in sync with your UI mapping where some effects may be mapped to `null`.
 * This local gate avoids enqueuing sounds that will never play.
 */
private val UnMutedEffects: Set<SoundEffect> = setOf(

// 🎲 Game lifecycle
    SoundEffect.NewRound,
    SoundEffect.CardPlayed,
    SoundEffect.Skip,
    // SoundEffect.TurnStart,
    SoundEffect.RoundOverWin,
    SoundEffect.RoundOverLose,
    SoundEffect.GameOverWin,
    SoundEffect.GameOverLose,

// 💡 Hints
    // SoundEffect.CannotBeat,
    // SoundEffect.MustPlayOne,
    // SoundEffect.MustPlayAllIn,
    // SoundEffect.CanPlayChoice,
    SoundEffect.MatDiscarded,
    // SoundEffect.YouKept,
    // SoundEffect.PlayerKept,
    // SoundEffect.PlayerSkippedHint
)

/** Returns true if this effect should be enqueued (i.e., not muted). */
private fun isAudible(effect: SoundEffect): Boolean = effect in UnMutedEffects

/**
 * Pure function used at the end of the reducer to decide which one-shot SFX to produce.
 * - Respects the master volume: when off => no sounds at all.
 * - Gates per-effect: if an effect is muted by design, it is not enqueued.
 * - Keeps the code simple and local to the reducer (no Android or UI dependencies).
 */
private fun synthesizeSfx(
    prev: GameState,
    next: GameState,
    event: GameEvent
): List<GameSideEffect> {
    // Master kill switch: no side effects when SFX volume is OFF
    if (next.soundEffectVolume == SoundVolume.Off) return emptyList()

    // Collect candidate effects here
    val effects = mutableListOf<SoundEffect>()

    // -------- Event → SoundEffect (1:1) --------
    when (event) {
        is GameEvent.NewRoundStarted -> effects += SoundEffect.NewRound
        is GameEvent.CardPlayed      -> effects += SoundEffect.CardPlayed
        is GameEvent.PlayerSkipped   -> effects += SoundEffect.Skip
        is GameEvent.NextTurn        -> effects += SoundEffect.TurnStart
        is GameEvent.RoundOver -> {
            effects += if (event.localPlayerWon) SoundEffect.RoundOverWin
            else                          SoundEffect.RoundOverLose
        }
        is GameEvent.GameOver -> {
            effects += if (event.localPlayerWon) SoundEffect.GameOverWin
            else                          SoundEffect.GameOverLose
        }
        is GameEvent.AITimerExpired,
        is GameEvent.QuitGame,
        is GameEvent.NetworkStateSync,
        is GameEvent.TurnHintReceived,
        is GameEvent.BannerReceived -> { /* no sound */ }
    }

    /*
    when (event) {
        is GameEvent.GameOver -> println("Toto GameOver event received in synthesizeSfx, localPlayerWon=${event.localPlayerWon}")
        else -> ""
    }
    */


// -------- Hint → SoundEffect (1:1), only on hint transitions --------
    val hintChanged = next.turnHintMsg != prev.turnHintMsg
    if (hintChanged) {
        when (val hint = next.turnHintMsg) {
            is TurnHintMsg.PlayerSkipped         -> effects += SoundEffect.PlayerSkippedHint
            is TurnHintMsg.MatDiscardedNext      -> effects += SoundEffect.MatDiscarded
            is TurnHintMsg.YouCannotBeat         -> effects += SoundEffect.CannotBeat
            is TurnHintMsg.YouMustPlayOne        -> effects += if (hint.canAllIn) SoundEffect.MustPlayAllIn else SoundEffect.MustPlayOne
            is TurnHintMsg.YouCanPlayNOrNPlusOne -> effects += SoundEffect.CanPlayChoice
            is TurnHintMsg.YouKept               -> effects += SoundEffect.YouKept
            is TurnHintMsg.PlayerKept            -> effects += SoundEffect.PlayerKept
            null                                 -> { /* no hint */ }
        }
    }

    // Gate per-effect to avoid enqueuing sounds that are intentionally silent.
    return effects
        .filter(::isAudible)   // drop muted effects
        //.distinct()          // (optional) uncomment to dedupe within the same pass
        .map { GameSideEffect.PlaySound(it) }
}


/** Decide which background music to play/stop based on the event. */
private fun synthesizeMusic(
    prev: GameState,
    next: GameState,
    event: GameEvent
): List<GameSideEffect> {
    // Respect user's music volume: if off, emit nothing
    if (next.soundMusicVolume == SoundVolume.Off) return emptyList()

    return when (event) {
        is GameEvent.NewRoundStarted -> listOf(
            GameSideEffect.PlayMusic(MusicTrack.InGame, loop = true)
        )
        is GameEvent.GameOver -> listOf(
            GameSideEffect.PlayMusic(
                track = if (event.localPlayerWon) MusicTrack.Victory else MusicTrack.Defeat,
                loop = false
            )
        )
        is GameEvent.QuitGame -> listOf(GameSideEffect.StopMusic)
        else -> emptyList()
    }
}

private fun handleNetworkStateSync(state: GameState, event: GameEvent.NetworkStateSync): ReducerResult {
    val snapshot = event.snapshot
    
    // 1. De-dupe: If we already moved past this turn, ignore
    if (snapshot.turnId < state.turnId) {
        TRACE(INFO) { "STATE_SYNC ignored (old turn): msg=${snapshot.turnId} local=${state.turnId}" }
        return ReducerResult(state)
    }
    
    // 2. Reconstruct Players from Snapshot
    // We cannot just map over state.players because it might be empty on first sync.
    // We must build the list from the authoritative source (snapshot.playersPublic).
    val myUid = state.multiplayerState?.myUid ?: ""

        val isMe = (publicView.playerId == myUid)
        TRACE(DEBUG) { "SYNC_PLAYER: id=${publicView.playerId} name=${publicView.name} type=${publicView.type} isMe=$isMe myUid=$myUid" }

        // Determine the concrete Player type to instantiate
        // If it's ME, I am Local. Everyone else is Remote (or AI if marked as AI).
        // note: The 'type' in PublicPlayerView is what the HOST sees.
        // If Host says "AI", it's an AI.
        // If Host says "LOCAL" (Host himself), to me (Guest) he is REMOTE.
        // If Host says "REMOTE" (Guest), if that ID matches mine, I am LOCAL.

        // Simpler logic: match ID.
        val resolvedType = when {
            publicView.playerId == myUid -> PlayerType.LOCAL
            publicView.type == PlayerType.AI -> PlayerType.AI
            else -> PlayerType.REMOTE
        }

        // Construct the hand
        val resolvedHand = if (isMe && snapshot.myHand != null) {
            TRACE(DEBUG) { "SYNC_HAND: Using SNAPSHOT hand for ${publicView.name} (size=${snapshot.myHand.cards.size})" }
            snapshot.myHand // My real hand
        } else {
            // For others, we only know the count. Create hidden cards.
             TRACE(DEBUG) { "SYNC_HAND: Using HIDDEN hand for ${publicView.name} (count=${publicView.handCount}) isMe=$isMe handNull=${snapshot.myHand == null}" }
            Hand.createHidden(publicView.handCount)
        }

        // Instantiate
        when (resolvedType) {
            PlayerType.LOCAL -> com.example.nido.game.LocalPlayer(
                id = publicView.playerId,
                name = publicView.name,
                avatar = "👤", // Avatar not currently synced in PublicView, default for now
                score = publicView.score,
                hand = resolvedHand
            )
            PlayerType.AI -> com.example.nido.game.ai.AIPlayer(
                id = publicView.playerId,
                name = publicView.name,
                avatar = "🤖",
                score = publicView.score,
                hand = resolvedHand
            )
            PlayerType.REMOTE -> com.example.nido.game.multiplayer.RemotePlayer(
                id = publicView.playerId,
                name = publicView.name,
                avatar = "👤",
                score = publicView.score,
                hand = resolvedHand
            )
        }
    }

    var newState = state.copy(
        players = reconstructedPlayers,
        turnId = snapshot.turnId,
        currentPlayerIndex = snapshot.currentPlayerIndex,
        currentPlayerId = snapshot.currentPlayerId,
        currentCombinationOnMat = snapshot.matCombination,
        skipCount = snapshot.skipCount,
        pointLimit = snapshot.pointLimit,
        lastPlayerWhoPlayed = snapshot.lastMove?.byPlayerId?.let { id ->
            reconstructedPlayers.find { it.id == id }
        }
    )

    // 3. Recalculate Logic for the Client
    // The snapshot gives us data, but we must re-run the rules/hints from OUR perspective.
    // e.g. If I am the active player, I want "You must play 3 cards", not the Host telling me "Guest is playing".
    
    val newTurnInfo = calculateTurnInfo(newState)
    newState = newState.copy(turnInfo = newTurnInfo)

    // We recalculate the hint. If the Host sent a specific event hint (like "Player Skipped"),
    // that usually comes via a separate MSG_TYPE_TURN_HINT.
    // However, for the "State Sync" (baseline state), we want the baseline hint (Instruction).
    val currentPlayer = newState.players[newState.currentPlayerIndex]
    val newHint = buildTurnHint(newState, currentPlayer.playerType)
    
    newState = newState.copy(turnHintMsg = newHint)
    
    return ReducerResult(newState) 
}
