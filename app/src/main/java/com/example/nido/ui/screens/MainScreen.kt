package com.example.nido.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.Card
import com.example.nido.game.GameManager
import com.example.nido.ui.views.ActionButtonsView
import com.example.nido.ui.views.HandView
import com.example.nido.ui.views.MatView
import com.example.nido.utils.Constants.CARD_HEIGHT
import com.example.nido.utils.Constants.CARD_WIDTH
import com.example.nido.utils.SortMode
import androidx.lifecycle.viewmodel.compose.viewModel // Import
import com.example.nido.game.GameViewModel // Import
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.ui.res.painterResource


@Composable
fun MainScreen(
    onEndGame: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel
) {

    val gameState by viewModel.gameManager.gameState // Observe the GameState

    val currentPlayer by remember { derivedStateOf {  GameManager.getCurrentPlayer() } } // Get CurrentPlayer
    val currentHand by remember { derivedStateOf { currentPlayer.hand } }          // Get currentHand
    val playmat by remember { derivedStateOf<List<Card>> { gameState.currentCombinationOnMat?.cards ?: emptyList() } }     // Get cards on Playmat
    val discardPile by remember { derivedStateOf { gameState.discardPile } }    // Get DiscardPile

    var sortMode by remember { mutableStateOf(SortMode.FIFO) }
    val toggleSortMode: () -> Unit = {
        sortMode = when (sortMode) {
            SortMode.FIFO -> SortMode.COLOR
            SortMode.COLOR -> SortMode.VALUE
            SortMode.VALUE -> SortMode.FIFO
        }
    }

    val selectedCards = remember { mutableStateListOf<Card>() }

    val isValidMove by remember(selectedCards) {
        mutableStateOf(GameManager.isValidMove(selectedCards)) // Observe isValidMove
    }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm") },
            text = { Text("Are you sure you want to quit the game?") },
            confirmButton = {
                Button(onClick = { onEndGame() }) { // Call the lambda directly
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Text(text = "Current Player Index:  ${viewModel.gameManager.getCurrentPlayer().id}")
        Text(text = "Current Player Id:  ${viewModel.gameManager.getCurrentPlayer().id}")
        Text(text = "Current Player Id:  ${viewModel.gameManager.getCurrentPlayer().name}")
        Text(text = "Current Player Score:  ${viewModel.gameManager.getCurrentPlayer().score}")
        Text(text = "Current Player is of type:  ${viewModel.gameManager.getCurrentPlayer().playerType}")


        Spacer(modifier = Modifier.height(16.dp))
        Text("Cards on the Playmat", fontSize = 20.sp)
        if (viewModel.gameManager.gameState.value.currentCombinationOnMat != null) {
            DisplayCombination(viewModel.gameManager.gameState.value.currentCombinationOnMat!!.cards)
        }
        else {
            Text("No cards on playmat for the moment")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Cards in the Discard Pile", fontSize = 20.sp)
        DisplayCombination(viewModel.gameManager.gameState.value.discardPile)

        Spacer(modifier = Modifier.height(16.dp))
        //Display Players Hand
        Text(
            text = "Player Hand:  ",
            fontSize = 20.sp
        )
        DisplayCombination(viewModel.gameManager.getCurrentPlayer().hand.cards)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "End Game", modifier = Modifier.clickable { showDialog = true })

    }

}
/*
@Composable
fun DisplayCombination(cards: List<Card>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        items(cards) { card ->
            CardItem(card)
        }
    }
}

@Composable
fun CardItem(card: Card) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .height(100.dp)
            .width(65.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 10.dp
        ),
        shape = RoundedCornerShape(10.dp)

    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White), // Background color for contrast
            contentAlignment = Alignment.Center // Center the content inside the Box
        ) {
            Image(
                painter = painterResource(id = card.img),
                contentDescription = "Card ${card.value} of ${card.color}",
                modifier = Modifier
                    .fillMaxSize()

            )
        }
    }
}
*/
