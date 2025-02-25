package com.example.nido.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType

@Composable
fun PlayersRowView(players: List<Player>, currentLocalPlayerIndex: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        players.forEachIndexed { index, player ->
            val isCurrent = index == currentLocalPlayerIndex
            val color = if (isCurrent) Color.Yellow else Color.White
            val playerTypeEmoji = when (player.playerType) {
                PlayerType.LOCAL -> "🧑"  // Human Player
                PlayerType.AI -> "🤖"     // AI Player
                PlayerType.REMOTE -> "🌐" // Remote Player
            }

            Text(
                text = "$playerTypeEmoji ${player.name}: ${player.hand.count()} cards",
                fontSize = 16.sp,
                color = color
            )
        }
    }
}
