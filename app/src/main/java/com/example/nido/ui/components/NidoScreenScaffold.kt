package com.example.nido.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nido.ui.theme.NidoColors

@Composable
fun NidoScreenScaffold(
    modifier: Modifier = Modifier,
    outerBackground: Color = NidoColors.ScoreScreenBackground,
    outerPaddingHorizontal: Dp = 16.dp,
    outerPaddingVertical: Dp = 16.dp,
    cardBackground: Color = NidoColors.LandingMainBackground,
    cardBorderColor: Color = NidoColors.LandingMainStroke,
    cardCornerRadius: Dp = 32.dp,
    cardBorderWidth: Dp = 2.dp,
    cardShadow: Dp = 8.dp,
    cardInnerPaddingHorizontal: Dp = 32.dp,
    cardInnerPaddingVertical: Dp = 32.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(outerBackground)
            .padding(
                horizontal = outerPaddingHorizontal,
                vertical = outerPaddingVertical
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = cardBorderWidth,
                    color = cardBorderColor,
                    shape = RoundedCornerShape(cardCornerRadius)
                )
                .shadow(
                    elevation = cardShadow,
                    shape = RoundedCornerShape(cardCornerRadius)
                )
                .background(
                    color = cardBackground,
                    shape = RoundedCornerShape(cardCornerRadius)
                )
                .padding(
                    horizontal = cardInnerPaddingHorizontal,
                    vertical = cardInnerPaddingVertical
                )
        ) {
            content()
        }
    }
}
