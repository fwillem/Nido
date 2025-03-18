package com.example.nido.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview // 🚀 Added Preview import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.Player
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.theme.NidoTheme

@Composable
fun ScoreScreen(
    onContinue: () -> Unit,
    onEndGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameManager = LocalGameManager.current  // ✅ Retrieve injected GameManager

    val rankings = gameManager.getPlayerRankings() // ✅ Now gets (Player, Rank) pairs
    val winners = gameManager.getGameWinners() // ✅ Overall winners
    val gameOver = gameManager.isGameOver() // ✅ Check if game is over

    Box(
        modifier = Modifier.background(NidoColors.SetupScreenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🏆 Final Rankings", fontSize = 24.sp, color = Color.White)

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ Display Ranked Players (with ranking numbers)
            rankings.forEach { (player, rank) ->
                PlayerScoreRow(player = player, rank = rank)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (gameOver) {
                Text(
                    "🎉 Winner(s): ${winners.joinToString(", ") { it.name }}",
                    fontSize = 20.sp,
                    color = Color.Yellow
                )

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
}

@Composable
fun PlayerScoreRow(player: Player, rank: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rank == 1) Color.Yellow else Color.DarkGray
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#$rank ${player.name} (${player.playerType})", fontSize = 18.sp, color = Color.White)
            Text("${player.score} pts", fontSize = 18.sp, color = Color.White)
        }
    }
}

// 🚀 Landscape Preview for ScoreScreen
@Preview(
    name = "ScoreScreen - Landscape", // 🚀 Preview name
    showBackground = true,
    widthDp = 800, // 🚀 Landscape width
    heightDp = 400 // 🚀 Landscape height
)
@Composable
fun ScoreScreenLandscapePreview() {
    NidoTheme { // 🚀 Wrap in your theme for proper styling
        ScoreScreen(
            onContinue = {},
            onEndGame = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
