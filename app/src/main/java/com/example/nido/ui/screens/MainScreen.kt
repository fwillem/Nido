package com.example.nido.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nido.game.rules.HAND_SIZE

import com.example.nido.data.model.Hand
import com.example.nido.data.repository.CardRepository
import com.example.nido.data.repository.CardRepository.generateDeck
import com.example.nido.data.model.Card
import com.example.nido.utils.SortMode

import com.example.nido.ui.views.ActionButtonsView
import com.example.nido.ui.views.PlayersRowView
import com.example.nido.ui.views.HandView
import com.example.nido.ui.views.MatView

import com.example.nido.utils.Constants.CARD_WIDTH
import com.example.nido.utils.Constants.CARD_HEIGHT



import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

import com.example.nido.game.GameManager

import com.example.nido.data.model.PlayerType



@Composable
fun MainScreen(
    onEndGame: () -> Unit,
    modifier: Modifier = Modifier) {


    val currentPlayer = GameManager.getCurrentPlayer()
    val playmat = GameManager.playmat
    val discardPile = GameManager.discardPile


    var sortMode by remember { mutableStateOf(SortMode.FIFO) }

       val toggleSortMode: () -> Unit = {
        sortMode = when (sortMode) {
            SortMode.FIFO -> SortMode.COLOR
            SortMode.COLOR -> SortMode.VALUE
            SortMode.VALUE -> SortMode.FIFO
        }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF006400)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Action Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            ActionButtonsView(
                mapOf(

                    "Sort Mode: ${sortMode.name}" to {
                        println("Sort Mode clicked!")  // DEBUG
                        toggleSortMode()
                    },

                    "Quit" to {
                        println("Quit")  // DEBUG
                        onEndGame()
                    }

                )
            )
        }

        // Player Information Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFF004000)),
            contentAlignment = Alignment.Center
        ) {
            PlayersRowView(
                players = GameManager.players,  // ✅ Pass the full list of players
                currentLocalPlayerIndex = GameManager.currentTurnIndex // ✅ Get from GameManager
            )
        }

        // Middle Section: MatView (Playmat + Discard Pile)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF228B22)),
            contentAlignment = Alignment.Center
        ) {
            MatView(
                playmat = GameManager.playmat,  // ✅ Use the global game playmat
                discardPile = GameManager.discardPile,
                cardWidth = CARD_WIDTH.dp,
                cardHeight = CARD_HEIGHT.dp
            )
        }

        // Bottom Section: HandView (Player's Hand)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF006400)),
            contentAlignment = Alignment.Center
        ) {
            val localPlayer = GameManager.players.first { it.playerType == PlayerType.LOCAL }

            HandView(
                hand = localPlayer.hand,  // ✅ Only the local player’s hand is shown
                cardWidth = CARD_WIDTH.dp,
                cardHeight = CARD_HEIGHT.dp,
                sortMode = sortMode,
                onDoubleClick = toggleSortMode
            )


        }
    }
}
