package com.example.nido

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.nido.ui.theme.NidoTheme

@Composable
fun NidoApp(modifier: Modifier = Modifier) {

    MainScreen(modifier = modifier)

}

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