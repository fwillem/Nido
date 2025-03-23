package com.example.nido.ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.Player
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.theme.NidoTheme  // ✅ Added missing import
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.preview.NidoPreview
import com.example.nido.game.FakeGameManager

@Composable
fun PlayerScoreView(player: Player, rank: Int) {
    val cardHeight = if (rank == 1) 50.dp else 40.dp // 🏆 Increase height for the top-ranked player
    val fontSize = if (rank == 1) 22.sp else 14.sp // 🔤 Adjust font size
    val lineHeight = if (rank == 1) 28.sp else 20.sp // 📏 Adjust line height

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight) // ✅ Explicit height for better spacing
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rank == 1) NidoColors.ScoreScreenWinner else Color.DarkGray
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp), // ✅ Adjusted padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val textColor = if (rank == 1) Color.DarkGray else Color.White

            Text(
                text = "#$rank ${player.name} (${player.playerType.displayName})",
                fontSize = fontSize,
                lineHeight = lineHeight, // ✅ Adjusted line height
                color = textColor
            )
            Text(
                text = "${player.score} pts",
                fontSize = fontSize,
                lineHeight = lineHeight, // ✅ Keep line height consistent
                color = textColor
            )
        }
    }
}

@NidoPreview(name = "PlayerScoreView First")
@Composable
fun PlayerScoreViewPreview() {
    NidoTheme { // ✅ Wrap everything inside NidoTheme
        val fakeGameManager = FakeGameManager()
        val topRankedPlayer = fakeGameManager.getPlayerRankings().first().first // Retrieve the top-ranked player

        CompositionLocalProvider(LocalGameManager provides fakeGameManager) {
            PlayerScoreView(
                player = topRankedPlayer,
                rank = 1,
            )
        }
    }
}

@NidoPreview(name = "PlayerScoreView Second")
@Composable
fun PlayerScoreViewPreview2() {
    NidoTheme { // ✅ Wrap everything inside NidoTheme
        val fakeGameManager = FakeGameManager()
        val secondRankedPlayer = fakeGameManager.getPlayerRankings().getOrNull(1)?.first
            ?: fakeGameManager.getPlayerRankings().first().first

        CompositionLocalProvider(LocalGameManager provides fakeGameManager) {
            PlayerScoreView(
                player = secondRankedPlayer,
                rank = 2,
            )
        }
    }
}
