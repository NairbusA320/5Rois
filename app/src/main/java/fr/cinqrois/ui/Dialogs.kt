package fr.cinqrois.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cinqrois.game.Engine
import fr.cinqrois.game.GameState
import fr.cinqrois.game.ROUNDS

/** Les mains de tous les joueurs, rangées, avec les points pris à la manche. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoundSummary(game: GameState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        game.players.forEachIndexed { index, player ->
            val plan = game.arrangements.getOrNull(index)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        player.name + if (game.wentOut == index) " (sorti)" else "",
                        fontWeight = FontWeight.Bold
                    )
                    Text("+${player.roundPenalty}  →  ${player.total}", fontWeight = FontWeight.Bold)
                }
                if (plan != null) {
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        plan.groups.forEach { group ->
                            group.cards.forEach { card ->
                                PlayingCard(card = card, wildRank = game.wildRank, width = 26.dp)
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        plan.leftover.forEach { card ->
                            PlayingCard(card = card, wildRank = game.wildRank, width = 26.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoundOverDialog(game: GameState, onNext: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Fin de la manche ${game.round} / $ROUNDS") },
        text = { RoundSummary(game) },
        confirmButton = {
            TextButton(onClick = onNext) { Text("Manche suivante") }
        }
    )
}

@Composable
fun GameOverDialog(game: GameState, onNewGame: () -> Unit) {
    val winners = Engine.winners(game)
    val ranking = game.players.sortedBy { it.total }
    val title = if (winners.size == 1) {
        if (winners[0] == 0) "Vous avez gagné !" else "${game.players[winners[0]].name} a gagné"
    } else {
        "Égalité"
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Classement final (le moins de points gagne) :")
                ranking.forEachIndexed { rank, player ->
                    Text(
                        "${rank + 1}. ${player.name} : ${player.total} points",
                        fontWeight = if (player.total == ranking[0].total) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNewGame) { Text("Nouvelle partie") }
        }
    )
}

@Composable
fun RulesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Règles du jeu") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RuleText("But", "Avoir le moins de points possible au bout de 11 manches.")
                RuleText(
                    "Les cartes",
                    "Deux paquets : 5 couleurs (♠ ♥ ♣ ♦ ★) de 3 à Roi, plus 6 jokers. " +
                        "À la manche 1 on reçoit 3 cartes, à la manche 2 : 4 cartes, ... à la manche 11 : 13 cartes."
                )
                RuleText(
                    "Cartes sauvages",
                    "Les jokers et les cartes du rang de la manche (3 à la manche 1, 4 à la manche 2, ... " +
                        "Valet à la manche 9, Dame à la 10, Roi à la 11) remplacent n'importe quelle carte."
                )
                RuleText(
                    "Votre tour",
                    "Piochez une carte (pioche ou dessus de la défausse), puis défaussez-en une."
                )
                RuleText(
                    "Combinaisons",
                    "Un brelan ou un carré : 3 cartes ou plus de même valeur. " +
                        "Une suite : 3 cartes ou plus qui se suivent, de la même couleur " +
                        "(on ne passe pas du Roi au 3)."
                )
                RuleText(
                    "Sortir",
                    "Quand toutes vos cartes, sauf celle que vous défaussez, font partie d'une combinaison, " +
                        "vous sortez. Les autres jouent un dernier tour."
                )
                RuleText(
                    "Points",
                    "Les cartes qui ne sont dans aucune combinaison comptent : valeur de la carte pour 3 à 10, " +
                        "Valet 11, Dame 12, Roi 13, joker 50. Une carte sauvage restante compte sa valeur."
                )
                RuleText(
                    "Aide",
                    "Les cartes d'un même groupe sont soulignées de la même couleur. " +
                        "Le bouton « Groupes » active ou coupe cette aide."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
private fun RuleText(title: String, body: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(body, fontSize = 14.sp, style = MaterialTheme.typography.bodyMedium)
    }
}
