package com.example.nido.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview // 🚀 Import Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.R
import com.example.nido.data.model.Card
import com.example.nido.data.model.CardColor
import com.example.nido.events.GameDialogEvent
import com.example.nido.ui.theme.NidoColors


@Composable
fun CardSelectionDialog(event: GameDialogEvent.CardSelection) { // 🚀 Extracted CardSelection dialog
    AlertDialog(
        onDismissRequest = { event.onCancel() },
        title = {
            Text(
                stringResource(R.string.select_card_to_keep)
                /*,
                modifier = Modifier
                    .background(NidoColors.DialogTitleBackground.copy(alpha = 0.5f))
                    .padding(4.dp)

                 */
            )
        },
        text = {
            Row {
                event.candidateCards.forEach { card ->
                    Button(
                        onClick = { event.onConfirm(card) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = card.color.uiColor.copy(alpha = 0.85f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.padding(1.dp), // external padding, adjust if needed
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 4.dp, // 🚀 reduced horizontal padding
                            vertical = 2.dp    // 🚀 reduced vertical padding
                        )
                    ) {
                        Text("${card.value}", fontSize = 20.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { event.onCancel() },
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
       // containerColor = Color.Transparent
        containerColor = Color.White.copy(alpha = 0.5f)

    )
}

// 🚀 Helper function to create a sample CardSelection event with varying card setups.
private fun sampleCardSelectionEvent(numCards: Int): GameDialogEvent.CardSelection {
    val cards = when (numCards) {
        1 -> listOf(
            // 1 card: 1 red card with value 2
            Card(2, "RED")
        )
        2 -> listOf(
            // 2 cards: both red cards with values 2 and 3
            Card(2, "RED"),
            Card(3, "RED")
        )
        3 -> listOf(
            // 3 cards: all green cards with values 4, 5, 6
            Card(4, "GREEN"),
            Card(5, "GREEN"),
            Card(6, "GREEN")
        )
        4 -> listOf(
            // 4 cards: all with value 3 and colors: RED, MOCHA, PINK, GREEN
            Card(3, "RED"),
            Card(3, "MOCHA"),
            Card(3, "ORANGE"),
            Card(3, "BLUE")
        )
        /*
        5 -> listOf(
            // 5 cards: cycle through RED, MOCHA, PINK, GREEN, ORANGE with increasing values
            Card(6, "RED"),
            Card(6, "MOCHA"),
            Card(6, "PINK"),
            Card(6, "GREEN"),
            Card(6, "ORANGE")
        )
        6 -> listOf(
            // 6 cards: a mix of PINK, RED, GREEN, MOCHA, BLUE, ORANGE
            Card(9, "PINK"),
            Card(9, "RED"),
            Card(9, "GREEN"),
            Card(9, "MOCHA"),
            Card(9, "BLUE"),
            Card(9, "ORANGE")
        )
        7 -> listOf(
            // 7 cards: varied sequence: RED, MOCHA, PINK, GREEN, ORANGE, BLUE, PINK
            Card(1, "BLUE"),
            Card(2, "BLUE"),
            Card(3, "BLUE"),
            Card(4, "BLUE"),
            Card(5, "BLUE"),
            Card(6, "BLUE"),
            Card(7, "BLUE")
        )

         */
        8 -> listOf(
            // 8 cards: cycle through MOCHA, PINK, RED, GREEN, ORANGE, BLUE, MOCHA, PINK all with value 3
            Card(1, "MOCHA"),
            Card(2, "MOCHA"),
            Card(3, "MOCHA"),
            Card(4, "MOCHA"),
            Card(5, "MOCHA"),
            Card(6, "MOCHA"),
            Card(7, "MOCHA"),
            Card(8, "MOCHA")
        )
        else -> emptyList()
    }
    return GameDialogEvent.CardSelection(
        candidateCards = cards,
        selectedCards = emptyList(),
        onConfirm = {},
        onCancel = {}
    )
}

// 🚀 Previews

@Preview(name = "CardSelectionDialog - 1 Card", showBackground = true)
@Composable
fun PreviewCardSelectionDialog1() {
    CardSelectionDialog(event = sampleCardSelectionEvent(1))
}

@Preview(name = "CardSelectionDialog - 2 Cards", showBackground = true)
@Composable
fun PreviewCardSelectionDialog2() {
    CardSelectionDialog(event = sampleCardSelectionEvent(2))
}

@Preview(name = "CardSelectionDialog - 3 Cards", showBackground = true)
@Composable
fun PreviewCardSelectionDialog3() {
    CardSelectionDialog(event = sampleCardSelectionEvent(3))
}

@Preview(name = "CardSelectionDialog - 4 Cards", showBackground = true)
@Composable
fun PreviewCardSelectionDialog4() {
    CardSelectionDialog(event = sampleCardSelectionEvent(4))
}

@Preview(name = "CardSelectionDialog - 5 Cards", showBackground = true)
@Composable
fun PreviewCardSelectionDialog5() {
    CardSelectionDialog(event = sampleCardSelectionEvent(5))
}

@Preview(name = "CardSelectionDialog - 6 Cards", showBackground = true)
@Composable
fun PreviewCardSelectionDialog6() {
    CardSelectionDialog(event = sampleCardSelectionEvent(6))
}

@Preview(name = "CardSelectionDialog - 7 Cards", showBackground = true)
@Composable
fun PreviewCardSelectionDialog7() {
    CardSelectionDialog(event = sampleCardSelectionEvent(7))
}

@Preview(name = "CardSelectionDialog - 8 Cards", showBackground = true)
@Composable
fun PreviewCardSelectionDialog8() {
    CardSelectionDialog(event = sampleCardSelectionEvent(8))
}

@Preview(name = "CardSelectionDialog - 9 Cards", showBackground = true)
@Composable
fun PreviewCardSelectionDialog9() {
    CardSelectionDialog(event = sampleCardSelectionEvent(9))
}
