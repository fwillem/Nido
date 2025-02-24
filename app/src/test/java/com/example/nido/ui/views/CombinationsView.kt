package com.example.nido.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.material3.value
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.CardView


@Composable
fun CombinationsView(
    combinations: List<Combination>,
    cardWidth: Dp,
    cardHeight: Dp
) {
    if (combinations.isEmpty()) {
        Text("No combinations available", color = Color.Red)
        return
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        horizontalArrangement = Arrangement.Center
    ) {
        items(combinations.size) { index ->
            val combination = combinations[index]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                Row {
                    combination.cards.forEach { card ->
                        CardView(
                            card,
                            Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                        )
                    }
                }
                Text("${combination.value}", fontSize = 16.sp, color = Color.Black)
            }
        }
    }
}