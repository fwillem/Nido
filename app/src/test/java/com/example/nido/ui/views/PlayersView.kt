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

@Composable
fun PlayersRowView(playerCounts: List<Int>, currentPlayerIndex: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        playerCounts.forEachIndexed { index, count ->
            if (index == currentPlayerIndex) {
                Text("🧑 You: $count cards", fontSize = 16.sp, color = Color.Yellow)
            } else {
                Text("Player ${index + 1}: $count cards", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}
