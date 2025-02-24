package com.example.nido.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.Player
import com.example.nido.game.rules.GameSettings

@Composable
fun ScoreScreen(
    players: List<Player>,
    onContinue: () -> Unit,
    onEndGame: () -> Unit
) {
    val winner = players.maxByOrNull { it.score }  // Player with the highest score
    val gameOver = winner?.score ?: 0 >= GameSettings.gamePointLimit

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B1B))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏆 Scoreboard", fontSize = 24.sp, color = Color.White)

        Spacer(modifier = Modifier.height(16.dp))

        players.sortedByDescending { it.score }.forEach { player ->
            PlayerScoreRow(player, isWinner = player == winner && gameOver)
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (gameOver) {
            Text("🎉 ${winner?.name} wins the game!", fontSize = 20.sp, color = Color.Yellow)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onEndGame) {
                Text("🏁 End Game")
            }
        } else {
            Button(onClick = onContinue) {
                Text("▶️ Continue Game")
            }
        }
    }
}

@Composable
fun PlayerScoreRow(player: Player, isWinner: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isWinner) Color.Yellow else Color.DarkGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${player.name} (${player.playerType})", fontSize = 18.sp, color = Color.White)
            Text("${player.score} pts", fontSize = 18.sp, color = if (isWinner) Color.Black else Color.White)
        }
    }
}
