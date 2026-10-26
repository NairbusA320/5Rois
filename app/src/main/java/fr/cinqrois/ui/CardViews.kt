package fr.cinqrois.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cinqrois.game.Card
import fr.cinqrois.game.HandSolver

/** Une carte à jouer. Si [card] est null, on voit le dos de la carte. */
@Composable
fun PlayingCard(
    card: Card?,
    wildRank: Int,
    modifier: Modifier = Modifier,
    width: Dp = 44.dp,
    selected: Boolean = false,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val height = width * 1.42f
    val shape = RoundedCornerShape(6.dp)
    val isWild = card != null && card.isWild(wildRank)
    val background = when {
        card == null -> CardBackColor
        isWild -> WildBackground
        else -> Color.White
    }
    val borderColor = when {
        selected -> Gold
        highlight -> Color(0xFF4FC3F7)
        else -> Color(0x66000000)
    }
    val borderWidth = if (selected || highlight) 3.dp else 1.dp
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(background)
            .border(borderWidth, borderColor, shape)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        val unit = width.value
        when {
            card == null -> Text(
                text = "♛",
                color = Color(0x99FFFFFF),
                fontSize = (unit * 0.55f).sp
            )

            card.isJoker -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "JOKER",
                    color = suitColor(null),
                    fontSize = (unit * 0.2f).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "♛",
                    color = suitColor(null),
                    fontSize = (unit * 0.6f).sp
                )
            }

            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = card.rankLabel,
                    color = suitColor(card.suit),
                    fontSize = (unit * 0.4f).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = card.suit!!.symbol,
                    color = suitColor(card.suit),
                    fontSize = (unit * 0.5f).sp
                )
            }
        }
    }
}

/** Emplacement vide (défausse sans carte). */
@Composable
fun EmptyPile(width: Dp = 64.dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .size(width = width, height = width * 1.42f)
            .border(2.dp, Color(0x55FFFFFF), shape)
    )
}

/** Une carte de la main avec, en dessous, la couleur de son groupe (ou rien si elle est isolée). */
@Composable
private fun HandSlot(
    card: Card,
    wildRank: Int,
    underline: Color?,
    selected: Boolean,
    isNew: Boolean,
    onClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier.offset(y = if (selected) (-8).dp else 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayingCard(
            card = card,
            wildRank = wildRank,
            selected = selected,
            highlight = isNew && !selected,
            onClick = onClick
        )
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(underline ?: Color.Transparent)
        )
    }
}

/**
 * La main du joueur. Avec [autoGroup], les cartes sont rangées par groupes (soulignés d'une même couleur),
 * suivies des cartes isolées ; sinon elles sont triées par couleur puis par valeur.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HandView(
    hand: List<Card>,
    wildRank: Int,
    autoGroup: Boolean,
    selectedId: Int?,
    newCardId: Int?,
    onCardClick: ((Card) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val plan = remember(hand, wildRank) { HandSolver.solve(hand, wildRank) }
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (autoGroup) {
            plan.groups.forEachIndexed { index, group ->
                val color = GroupColors[index % GroupColors.size]
                group.cards.forEach { card ->
                    HandSlot(
                        card, wildRank, color, selectedId == card.id, newCardId == card.id,
                        onCardClick?.let { click -> { click(card) } }
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            plan.leftover.forEach { card ->
                HandSlot(
                    card, wildRank, null, selectedId == card.id, newCardId == card.id,
                    onCardClick?.let { click -> { click(card) } }
                )
            }
        } else {
            val sorted = hand.sortedWith(compareBy<Card>({ it.suit?.ordinal ?: 99 }, { it.rank }, { it.id }))
            sorted.forEach { card ->
                HandSlot(
                    card, wildRank, null, selectedId == card.id, newCardId == card.id,
                    onCardClick?.let { click -> { click(card) } }
                )
            }
        }
    }
}
