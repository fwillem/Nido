package com.example.nido.ui.views

import android.graphics.Color.RED
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.collections.component1
import kotlin.collections.component2


@Composable
fun HandView(
    hand: Hand,
    cardWidth: Dp,
    cardHeight: Dp,
    sortMode: SortMode,
    maxActionsHeight: Dp = 128.dp, // Parameter to limit the height of the actions column
    debug: Debug,
    actions: Map<String, () -> Unit>,
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
        Row (
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ){
            // Buttons zone
            Column ( // Distribute buttons evenly vertically
                modifier = Modifier
                    .heightIn(max = maxActionsHeight,min = maxActionsHeight) // Limit the height of the column
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
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


            Row(
                modifier = Modifier
                    .weight(1f), // Allow this Row to take remaining space
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
            maxActionsHeight = 100.dp, // Example max height for the preview
            debug = Debug(),
            actions = mapOf("Sort" to { }),
        )
    }
}
