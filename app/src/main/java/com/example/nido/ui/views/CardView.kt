package com.example.nido.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp


import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.nido.data.model.Card




@Composable
fun CardView(
    card: Card, modifier: Modifier = Modifier
        .width(140.dp)
        .height(230.dp)
) {
    Column {
        Box(
            modifier = modifier
        ) {
            Image(
                painter = painterResource(id = card.cardImageId),
                contentDescription = "Card Number ${card.value}, Color ${card.color.name}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

