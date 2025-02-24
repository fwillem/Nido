package com.example.nido.ui.screens

import androidx.compose.ui.tooling.preview.Preview
import com.example.nido.ui.theme.NidoTheme

package com.example.nido

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.value
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



@Preview(
    name = "Landscape Preview",
    device = "spec:width=1280dp,height=800dp,dpi=240"
)
@Composable
fun GreetingPreview() {
    NidoTheme {
        NidoApp()
    }
}