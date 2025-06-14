package com.example.nido.ui.views

import android.graphics.Color.RED
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nido.data.model.CardColor
import com.example.nido.data.model.Card
import com.example.nido.data.model.Hand
import com.example.nido.ui.components.VersionLabel
import com.example.nido.ui.preview.NidoPreview
import com.example.nido.utils.sortedByMode
import com.example.nido.utils.SortMode
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.ui.views.CardView
import com.example.nido.utils.TestData.generateTestHand1
import com.example.nido.ui.components.VersionOptions.*
import com.example.nido.utils.Constants
import com.example.nido.utils.Constants.CARD_ON_HAND_HEIGHT
import com.example.nido.utils.Constants.CARD_ON_HAND_WIDTH
import com.example.nido.utils.Constants.SELECTED_CARD_OFFSET
import com.example.nido.utils.Debug


@Composable
fun HandView(
    hand: Hand,
    cardWidth: Dp,
    cardHeight: Dp,
    sortMode: SortMode,
    debug: Debug,
    onDoubleClick: () -> Unit,
    onSortMode: () -> Unit,
    onSelectCard: (Card) -> Unit
) {
    // TRACE(VERBOSE) { "Recomposing HandView: ${hand.cards}" }

    Box(
        modifier = Modifier
            .background(NidoColors.HandViewBackground2)
            .padding(bottom = 8.dp)
            .fillMaxSize()
    ) {
        val sortedCards by remember(hand.cards, sortMode) {
            derivedStateOf { hand.cards.sortedByMode(sortMode) }
        }

        // Cards row, centered
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.Center
        ) {
            sortedCards.forEach { card ->
                val animatedOffsetY by animateDpAsState(
                    targetValue = if (card.isSelected) (-SELECTED_CARD_OFFSET).dp else 0.dp,
                    label = "selectedCardLift"
                )

                Box(
                    modifier = Modifier
                        .offset(y = animatedOffsetY)
                        .pointerInput(card) {
                            detectTapGestures(
                                onTap = { onSelectCard(card) },
                                onDoubleTap = { onDoubleClick() }
                            )
                        }
                ) {
                    CardView(
                        card = card,
                        modifier = Modifier.size(cardWidth, cardHeight)

//                                modifier = Modifier.size(cardWidth, cardHeight)
                    )
                }
            }
        }

        // Version label
        VersionLabel(
            option = SHORT,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp, end = 32.dp)
        )
    }
}

@NidoPreview(name = "HandView")
@Composable
fun HandViewPreview() {
    NidoTheme {
        HandView(
            hand = generateTestHand1(),
            cardWidth = Constants.CARD_ON_HAND_WIDTH.dp,
            cardHeight = Constants.CARD_ON_HAND_HEIGHT.dp,
            sortMode = SortMode.COLOR,
            onDoubleClick = {},
            onSortMode = {},
            onSelectCard = {},
            debug = Debug()
        )
    }
}
