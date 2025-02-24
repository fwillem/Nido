package com.example.nido.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActionButtonsView(actions: Map<String, () -> Unit>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
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