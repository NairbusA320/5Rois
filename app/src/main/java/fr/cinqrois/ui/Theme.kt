package fr.cinqrois.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import fr.cinqrois.game.Suit

val Felt = Color(0xFF0B5D3B)
val FeltDark = Color(0xFF07412A)
val Gold = Color(0xFFFFC107)
val WildBackground = Color(0xFFFFF1B8)
val CardBackColor = Color(0xFF1A3A8A)

/** Couleurs de soulignement pour distinguer les groupes dans la main. */
val GroupColors = listOf(
    Color(0xFFFFC107),
    Color(0xFF4FC3F7),
    Color(0xFFFF8A65),
    Color(0xFFBA68C8),
    Color(0xFF81C784),
    Color(0xFFF06292)
)

fun suitColor(suit: Suit?): Color = when (suit) {
    Suit.HEARTS, Suit.DIAMONDS -> Color(0xFFC62828)
    Suit.SPADES, Suit.CLUBS -> Color(0xFF212121)
    Suit.STARS -> Color(0xFF1565C0)
    null -> Color(0xFF6A1B9A)
}

@Composable
fun CinqRoisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            onPrimary = Color(0xFF1B1B1B),
            secondary = Color(0xFF2E7D5B),
            onSecondary = Color.White,
            background = Felt,
            onBackground = Color.White,
            surface = FeltDark,
            onSurface = Color.White
        ),
        content = content
    )
}
