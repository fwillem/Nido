package com.example.nido.ui.views

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nido.data.model.Card
import com.example.nido.game.FakeGameManager
import com.example.nido.game.rules.calculateTurnInfo
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.utils.Debug
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.INFO
import com.example.nido.utils.SortMode
import com.example.nido.utils.sortedByMode
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun MatView(
    playmat: SnapshotStateList<Card>?,
    debug: Debug,
    currentPlayerHand: List<Card>,
    onPlayCombination: (List<Card>, Card?) -> Unit,
    onWithdrawCards: (List<Card>) -> Unit,
    onSkip: () -> Unit,
    cardWidth: Dp,
    cardHeight: Dp,
    bannerWidthFraction: Float = 1f,
    bannerCornerRadius: Dp = 12.dp,

    // Contraintes de taille du Snackbar (par défaut non spécifiées)
    bannerMinWidth: Dp = 164.dp,
    bannerMaxWidth: Dp = 256.dp,
    bannerMinHeight: Dp = Dp.Unspecified,
    bannerMaxHeight: Dp = 36.dp,
    // Contrôle de la typographie du texte du Snackbar
    bannerFontSize: TextUnit = 16.sp,
    bannerFontWeight: FontWeight = FontWeight.Bold,
    // Décalage vertical depuis le bas de l’écran (réduit pour descendre le banner)
    bannerBottomPadding: Dp = 8.dp,
) {
    val gameManager = LocalGameManager.current
    val gameState = gameManager.gameState.value
    val turnInfo = calculateTurnInfo(gameState)


    TRACE(INFO) { "🌀 MatView recomposed" }

    Row(modifier = Modifier.fillMaxSize()) {
        if (debug.displayAIsHands) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(NidoColors.SelectedCardBackground)
                    .fillMaxSize()
            ) {
                val sortedCards by remember(currentPlayerHand) {
                    derivedStateOf { currentPlayerHand.sortedByMode(SortMode.COLOR) }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                ) {
                    sortedCards.forEachIndexed { index, card ->
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
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .background(NidoColors.PlaymatBackground)
                .fillMaxSize()
        ) {
            // Centre le groupe de cartes dans toute la largeur visuelle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier,
                ) {
                    playmat?.let {
                        val sortedCards by remember(playmat) {
                            derivedStateOf { playmat.sortedByMode(SortMode.VALUE) }
                        }
                        for (sortedCard in sortedCards) {
                            CardView(
                                card = sortedCard,
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(cardHeight)
                            )
                        }
                    }
                }
            }

            // SnackBar centré horizontalement avec largeur/hauteur limitées
            /***
             * Le Snackbar est dans un BoxWithConstraints centré en bas.
             * Sa largeur est calculée comme: width = (nombreDeCartes * cardWidth) * bannerWidthFraction.
             * Elle est ensuite bornée par bannerMinWidth, bannerMaxWidth et la largeur dispo.
             * Le Snackbar est centré horizontalement (align(Alignment.Center)).
             * Si le tapis est vide, la largeur tombe sur maxWidth * bannerWidthFraction pour éviter width = 0.
             * bannerWidthFraction par défaut = 1f pour correspondre exactement à la largeur de la rangée.
             * Résultat
             * Le Snackbar se réaligne avec les bords de la rangée (qui est centrée).
             * La contrainte de taille continue de fonctionner (min/max width/height + maxLines/ellipsis).
             * Vérifications
             * Compilation de MatView.kt: PASS (aucune erreur signalée).
             * Astuces
             * Si vous voulez un Snackbar un peu plus étroit que la rangée, baissez bannerWidthFraction (ex: 0.9f).
             * Pour une borne stricte, combinez bannerWidthFraction = 1f avec bannerMaxWidth (ex: 420.dp).
             * Rappel du calcul
             * Largeur cible = (nbCartes * cardWidth) * bannerWidthFraction.
             * Puis bornage: ≥ bannerMinWidth (si défini), ≤ bannerMaxWidth (si défini), et ≤ largeur dispo du parent.
             * Si le tapis est vide: largeur = parentWidth * bannerWidthFraction.
             * Cas d’usage typiques
             * Plus étroit que la rangée: mettez bannerWidthFraction < 1 (ex: 0.9f). Laissez bannerMaxWidth non contraignant (Unspecified ou plus grand que la largeur calculée).
             * Borne stricte en largeur: mettez bannerWidthFraction = 1f et bannerMaxWidth = la valeur voulue (ex: 420.dp). La largeur ne dépassera jamais cette borne, même si la rangée est plus large.
             * Largeur fixe: réglez bannerMinWidth = X et bannerMaxWidth = X. Attention: dans ce cas, bannerWidthFraction n’a plus d’effet (ex: votre 128.dp/128.dp force 128.dp tout le temps).
             * Tapis vide: la largeur devient parentWidth * fraction; utilisez bannerMinWidth pour éviter une barre trop petite.
             * Exemples chiffrés
             * 10 cartes × 80.dp = 800.dp; fraction 0.9 ⇒ 720.dp; bannerMaxWidth = 420.dp ⇒ largeur finale 420.dp.
             * Tapis vide, parent 800.dp, fraction 0.5 ⇒ 400.dp; bannerMinWidth = 450.dp ⇒ largeur finale 450.dp.
             * Bonnes pratiques
             * Gardez bannerMinWidth ≤ bannerMaxWidth.
             * Si vous forcez une largeur (min=max), ne vous attendez pas à voir un effet de bannerWidthFraction.
             * Pour recoller visuellement aux cartes, gardez bannerWidthFraction = 1f (par défaut), sans max, ou bien mettez un max légèrement supérieur à la largeur de la rangée.
             * Si besoin, je peux ajuster pour intégrer un “padding de sécurité” (ex: tenir compte des 8.dp de padding du conteneur des cartes) afin que les bords s’alignent au pixel près.
             */
            val banner = gameState.matBanner
            if (banner != null) {
                BoxWithConstraints(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = bannerBottomPadding)                        .fillMaxWidth()
                ) {
                    val cardsCount = playmat?.size ?: 0
                    val fraction = bannerWidthFraction.coerceIn(0f, 1f)
                    val cardsWidth = cardWidth * cardsCount
                    // largeur désirée: si pas de cartes, utiliser une fraction de la largeur dispo
                    var w = if (cardsCount > 0) cardsWidth * fraction else this.maxWidth * fraction
                    // appliquer bornes et largeur dispo
                    if (bannerMaxWidth != Dp.Unspecified && w > bannerMaxWidth) w = bannerMaxWidth
                    if (bannerMinWidth != Dp.Unspecified && w < bannerMinWidth) w = bannerMinWidth
                    if (w > this.maxWidth) w = this.maxWidth

                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(w)
                            .heightIn(
                                min = bannerMinHeight,
                                max = bannerMaxHeight,
                            ),
                        containerColor = NidoColors.HandViewBackground.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(bannerCornerRadius)

                    ) {
                        Text(
                            banner,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = bannerFontWeight,
                            fontSize = bannerFontSize,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            TurnActionButtons(
                turnInfo = turnInfo,
                playmat = playmat,
                onPlayCombination = onPlayCombination,
                onWithdrawCards = onWithdrawCards,
                onSkip = onSkip,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Preview(
    name = "MatView - 2 on Playmat, 3 on Selected",
    showBackground = true,
    widthDp = 800,
    heightDp = 400
)
@Composable
fun PreviewMatViewScenario1() {
    NidoTheme {
        // Provide the FakeGameManager to the CompositionLocal so that
        // every call to LocalGameManager.current returns the fake instance.
        CompositionLocalProvider(LocalGameManager provides FakeGameManager()) {
            val playmatCards = remember { mutableStateListOf(
                Card(2, "RED"),
                Card(3, "GREEN")
            )}

            val currentPLayerHand = remember { mutableStateListOf(
                Card(9, "ORANGE"),
                Card(7, "PINK")
            )}



            val onPlayCombination: (List<Card>, Card?) -> Unit = { _, _ -> }
            val onWithdrawCards: (List<Card>) -> Unit = { _ -> }
            val onSkip: () -> Unit = {}

            MatView(
                playmat = playmatCards,
                currentPlayerHand = currentPLayerHand,
                debug = Debug(true,false),
                onPlayCombination = onPlayCombination,
                onWithdrawCards = onWithdrawCards,
                onSkip = onSkip,
                cardWidth = 80.dp,
                cardHeight = 120.dp,
            )
        }
    }
}
