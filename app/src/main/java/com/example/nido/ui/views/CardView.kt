package com.example.nido.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.nido.data.model.Card
import com.example.nido.ui.preview.NidoPreview
import com.example.nido.ui.theme.NidoColors

@Composable
fun CardView(
    card: Card,
    modifier: Modifier = Modifier
        .width(140.dp)
        .height(280.dp)
) {
    val shape = RoundedCornerShape(18.dp)
    val ivoryOverlay = Color(0xFFFFF8E1).copy(alpha = 0.2f)
    val borderColor = Color.White

    Box(
        modifier = modifier
            .graphicsLayer {
                shadowElevation = 16.dp.toPx() // Increased elevation
                this.shape = shape
                clip = true
                ambientShadowColor = Color.Black.copy(alpha = 0.3f) // Stronger shadow
                spotShadowColor = Color.Black.copy(alpha = 0.3f)
            }
            .background(Color.White, shape) // ✅ Solid base to prevent background bleed
            .border(width = 2.dp, color = borderColor, shape = shape)
            .drawWithContent {
                drawContent()
                drawRect(ivoryOverlay)
            }
    ) {
        Image(
            painter = painterResource(id = card.cardImageId),
            contentDescription = "Card Number ${card.value}, Color ${card.color.name}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@NidoPreview(name = "CardView")
@Composable
fun PreviewCardView() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        CardView(card = Card(1, "RED"))
    }
}

@NidoPreview(name = "CardView2")
@Composable
fun PreviewCardView2() {
    Box(
        modifier = Modifier.fillMaxSize().background(NidoColors.MatViewBackground),
        contentAlignment = Alignment.Center
    ) {
        CardView(card = Card(1, "RED"))
    }
}
