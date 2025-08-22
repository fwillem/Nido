package com.example.nido.ui.views

import com.example.nido.game.GameState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.R
import com.example.nido.game.TurnHintMsg
import com.example.nido.ui.LocalGameManager

@Composable
fun TurnHintText(state: GameState) {
    val text = when (val m = state.turnHintMsg) {
        is TurnHintMsg.PlayerSkipped         -> stringResource(R.string.hint_player_skipped, m.name)
        is TurnHintMsg.MatDiscardedNext      -> stringResource(R.string.hint_mat_discarded, m.name)
        is TurnHintMsg.YouCannotBeat         -> stringResource(R.string.hint_cannot_beat)
        is TurnHintMsg.YouMustPlayOne        -> stringResource(R.string.hint_must_play) +
                if (m.canAllIn) stringResource(R.string.hint_or_all_in) else ""
        is TurnHintMsg.YouCanPlayNOrNPlusOne -> stringResource(R.string.hint_can_play, m.n, m.n + 1)
        is TurnHintMsg.YouKept               -> stringResource(R.string.hint_you_kept, m.card)
        is TurnHintMsg.PlayerKept            -> stringResource(R.string.hint_player_kept, m.name, m.card)
        null                                -> ""
    }
    Text(text, fontWeight = FontWeight.Bold, color = Color.White)
}

@Composable
fun CommentsView(actions: Map<String, () -> Unit>) {
    val gameManager = LocalGameManager.current
    val gameState = gameManager.gameState.value

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text zone - 80%
        Box(
            modifier = Modifier.weight(0.8f, fill = true),
            contentAlignment = Alignment.Center
        ) {
            TurnHintText(gameState)
        }

        // Vertical Divider (Material3 only provides horizontal, but this works)
        VerticalDivider(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )


        // Buttons zone - 20%
        Row(
            modifier = Modifier.weight(0.2f, fill = true),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { (label, action) ->
                Button(
                    onClick = action,
                    modifier = Modifier
                        .height(16.dp)
                        .padding(horizontal = 2.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text(
                        label,
                        fontSize = 8.sp,
                        lineHeight = 8.sp
                    )
                }
            }
        }


    }
}
