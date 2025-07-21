package com.example.nido.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.*
import com.example.nido.data.repository.CardRepository.getBackCover
import com.example.nido.game.LocalPlayer
import com.example.nido.game.ai.AIPlayer
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.utils.Constants

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

            val backCoverCard = getBackCover()

            Box(
                modifier = Modifier
                    .background(backgroundColor)
                    .padding(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$playerTypeEmoji ${player.name}: ${player.hand.count()} ",
                        fontSize = 16.sp,
                        color = color
                    )

                    CardView(
                        card = backCoverCard,
                        modifier = Modifier
                            .width(8.dp)
                            .height(16.dp)
                    )
                    Text(
                        text = " ${player.score} ⭐",
                        fontSize = 16.sp,
                        color = color
                    )
                }
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

// 🚀 Dummy Players for Preview
@Preview(
    name = "PlayersRowView - 4 Players",
    showBackground = true,
    widthDp = 800,
    heightDp = 200
)
@Composable
fun PreviewPlayersRowView() {
    NidoTheme {
        val players = remember {
            listOf(
                LocalPlayer(
                    id = "1",
                    name = "Alice",
                    avatar = "",
                    score = 15,
                    hand = Hand(
                        mutableStateListOf(
                            Card(3, "RED"),
                            Card(5, "BLUE")
                        )
                    )
                ),
                AIPlayer(
                    id = "2",
                    name = "Bob",
                    avatar = "",
                    score = 10,
                    hand = Hand(
                        mutableStateListOf(
                            Card(7, "GREEN"),
                            Card(9, "MOCHA")
                        )
                    )
                ),
                AIPlayer(
                    id = "3",
                    name = "Carol",
                    avatar = "",
                    score = 20,
                    hand = Hand(
                        mutableStateListOf(
                            Card(2, "PINK"),
                            Card(4, "BLUE")
                        )
                    )
                ),
                AIPlayer(
                    id = "4",
                    name = "Bot 3",
                    avatar = "",
                    score = 5,
                    hand = Hand(
                        mutableStateListOf(
                            Card(1, "MOCHA"),
                            Card(8, "RED")
                        )
                    )
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Constants.PLAYERS_ROW_HEIGHT.dp)
                .background(NidoColors.PlayersRowBackground),

            ) {
            PlayersRowView(
                players = players,
                currentTurnIndex = 1,
                turnID = 12
            )
        }

    }
}
