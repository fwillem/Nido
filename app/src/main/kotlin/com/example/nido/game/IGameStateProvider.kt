package com.example.nido.game

import androidx.compose.runtime.State
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import kotlinx.coroutines.flow.StateFlow

interface IGameStateProvider {
    val gameState: StateFlow<GameState>
    fun isGameOver(): Boolean
    fun getGameWinners(): List<Player>
    fun getPlayerRankings(): List<Pair<Player, Int>>
    fun getPlayerHandScores(): List<Pair<Player, Int>>
    fun getCurrentPlayerHandSize(): Int
    fun isCurrentPlayerLocal(): Boolean
    fun currentPlayerHasValidCombination(): Boolean
    fun isValidMove(selectedCards: List<Card>): Boolean
    fun hasPlayedAllRemainingCards(): Boolean
}
