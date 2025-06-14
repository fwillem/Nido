package com.example.nido.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.nido.data.model.Card
import com.example.nido.ui.preview.NidoPreview

@Composable
fun CardDeckView (
    deck : List<Card>,
    cardWidth: Dp,
    cardHeight: Dp,
    ) {

    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        deck.forEachIndexed { index, card ->
            val overlapOffset = (cardWidth / 2) * index
            Box(
                modifier = Modifier
                    .offset(x = overlapOffset)
                    .zIndex(index.toFloat())
            ) {
                CardView(
                    card = card,
                    modifier = Modifier.size(cardWidth, cardHeight)
                )
            }
        }


    }
}

@NidoPreview(name = "CardDeckView")
@Composable
fun PreviewCardDeckView() {

    val cards =  listOf(Card(2, "RED"), Card(3, "GREEN"))
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        CardDeckView(
            cards,
            cardWidth = 80.dp,
            cardHeight = 160.dp,
        )
    }
}