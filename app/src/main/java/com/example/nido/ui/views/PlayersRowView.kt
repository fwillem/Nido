package com.example.nido.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType

@Composable
fun PlayersRowView(players: List<Player>, currentTurnIndex: Int, turnID: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        players.forEachIndexed { index, player ->
            val isCurrent = index == currentTurnIndex
            val color = when {
                isCurrent && player.playerType == PlayerType.LOCAL -> Color.Yellow   // Human player's turn
                isCurrent && player.playerType == PlayerType.AI -> Color.Red         // AI player's turn
                else -> Color.White
            }
            val backgroundColor = if (isCurrent) Color.DarkGray else Color.Transparent

            val playerTypeEmoji = when (player.playerType) {
                PlayerType.LOCAL -> "🧑"  // Human Player
                PlayerType.AI -> "🤖"     // AI Player
                PlayerType.REMOTE -> "🌐" // Remote Player
            }

            Box(
                modifier = Modifier
                    .background(backgroundColor)
                    .padding(4.dp)
            ) {
                Text(
                    text = "$playerTypeEmoji ${player.name}: ${player.hand.count()} (${player.score})",
                    fontSize = 16.sp,
                    color = color
                )
            }
        }

        Box(
            modifier = Modifier
                .background(Color.Transparent)
                .padding(4.dp)
        ) {

            Text(
                text = "\uD83C\uDFB2 $turnID",
                fontSize = 16.sp,
                color = Color.White
            )
        }

    }
}
