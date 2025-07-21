package com.example.nido.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview // 🚀 Added Preview import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.Card
import com.example.nido.data.model.Hand
import com.example.nido.data.model.Player
import com.example.nido.game.FakeGameManager
import com.example.nido.ui.LocalGameManager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.ui.views.HandView
import com.example.nido.utils.Constants
import com.example.nido.utils.SortMode
import com.example.nido.ui.views.PlayerScoreView
import com.example.nido.ui.components.NidoScreenScaffold
import androidx.compose.ui.unit.Dp   // The Dp type itself
import androidx.compose.ui.unit.dp   // The .dp extension property for Int/Float


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScoreScreen(
    onContinue: () -> Unit,
    onEndGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameManager = LocalGameManager.current  // ✅ Retrieve injected GameManager

    val rankings = gameManager.getPlayerRankings() // ✅ Now gets (Player, Rank) pairs
    val winners = gameManager.getGameWinners() // ✅ Overall winners

    //NidoScreenScaffold (cardInnerPaddingVertical = 8.dp,cardInnerPaddingHorizontal = 80.dp, outerPaddingHorizontal = 64.dp, outerPaddingVertical = 8.dp) {
    NidoScreenScaffold(
        cardInnerPaddingVertical = 8.dp,
        cardInnerPaddingHorizontal = 80.dp,
        maxContentWidth = 400.dp,   // For narrow dialogs
        maxContentHeight = null     // Or set as needed
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "🏆  Winner${if (winners.size > 1) "s" else ""}: ${winners.joinToString(", ") { it.name }}",
                fontSize = 28.sp,
                color = NidoColors.ScoreScreenWinner
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ Display Ranked Players (with ranking numbers)
            rankings.forEach { (player, rank) ->
                PlayerScoreView(player = player, rank = rank)
                //   Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onEndGame) {
                Text("🏁  OK  \uD83C\uDFC1")
            }

        }
    }
}


@Preview(
    name = "Landscape HandView Preview",
    widthDp = 800, // wider than it is tall
    heightDp = 400, // adjust as needed
    showBackground = true
)
@Composable
fun PreviewScoreScreen() {
    NidoTheme {
        CompositionLocalProvider(LocalGameManager provides FakeGameManager()) {
            ScoreScreen(
                onContinue = { },
                onEndGame = { },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
