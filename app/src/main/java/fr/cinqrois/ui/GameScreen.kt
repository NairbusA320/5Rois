package fr.cinqrois.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.cinqrois.game.Card
import fr.cinqrois.game.GameState
import fr.cinqrois.game.HandSolver
import fr.cinqrois.game.Phase
import fr.cinqrois.game.Player
import fr.cinqrois.game.ROUNDS

@Composable
fun GameScreen(vm: GameViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    val game = ui.game
    var showRules by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Felt) {
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            if (game == null) {
                StartScreen(
                    onStart = { opponents -> vm.startGame(opponents) },
                    onRules = { showRules = true }
                )
            } else {
                Table(ui, game, vm, onRules = { showRules = true })
                if (game.phase == Phase.ROUND_OVER) {
                    RoundOverDialog(game, onNext = { vm.nextRound() })
                }
                if (game.phase == Phase.GAME_OVER) {
                    GameOverDialog(game, onNewGame = { vm.quit() })
                }
            }
            if (showRules) {
                RulesDialog(onDismiss = { showRules = false })
            }
        }
    }
}

@Composable
private fun StartScreen(onStart: (Int) -> Unit, onRules: () -> Unit) {
    var opponents by rememberSaveable { mutableStateOf(1) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("♛", fontSize = 64.sp, color = Gold)
        Text(
            "5 ROIS",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = Gold
        )
        Text(
            "Le jeu de cartes, contre l'ordinateur",
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Text("Nombre d'adversaires", color = Color.White)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (n in 1..3) {
                if (n == opponents) {
                    Button(onClick = { opponents = n }) { Text(n.toString()) }
                } else {
                    OutlinedButton(onClick = { opponents = n }) {
                        Text(n.toString(), color = Color.White)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onStart(opponents) }) {
            Text("Jouer", fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRules) {
            Text("Règles du jeu", color = Color.White)
        }
    }
}

@Composable
private fun Table(ui: UiState, game: GameState, vm: GameViewModel, onRules: () -> Unit) {
    val human = game.players[0]
    val humanTurn = game.current == 0 && !game.isOver && !ui.aiPlaying
    val canDraw = humanTurn && game.phase == Phase.DRAW
    val canDiscard = humanTurn && game.phase == Phase.DISCARD

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
        // En-tête
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Manche ${game.round} / $ROUNDS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "Sauvages : ${Card.rankName(game.wildRank)} et jokers",
                    color = Gold,
                    fontSize = 13.sp
                )
            }
            Row {
                TextButton(onClick = onRules) { Text("Règles", color = Color.White) }
                TextButton(onClick = { vm.quit() }) { Text("Quitter", color = Color.White) }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Adversaires
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            game.players.forEachIndexed { index, player ->
                if (index > 0) {
                    OpponentBox(
                        player = player,
                        isCurrent = game.current == index && !game.isOver,
                        wentOut = game.wentOut == index,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Pioche, défausse et messages
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PlayingCard(
                        card = null,
                        wildRank = game.wildRank,
                        width = 64.dp,
                        highlight = canDraw,
                        onClick = if (canDraw) ({ vm.drawFromDeck() }) else null
                    )
                    Text("Pioche (${game.deck.size})", color = Color.White, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val top = game.topDiscard
                    if (top != null) {
                        PlayingCard(
                            card = top,
                            wildRank = game.wildRank,
                            width = 64.dp,
                            highlight = canDraw,
                            onClick = if (canDraw) ({ vm.drawFromDiscard() }) else null
                        )
                    } else {
                        EmptyPile(width = 64.dp)
                    }
                    Text("Défausse", color = Color.White, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                game.lastAction,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Text(
                hintText(game, ui, canDraw, canDiscard),
                color = Gold,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Main du joueur
        val goOutCard = remember(human.hand, game.wildRank, canDiscard) {
            if (canDiscard) HandSolver.goOutDiscard(human.hand, game.wildRank) else null
        }
        val pointsLeft = remember(human.hand, game.wildRank, canDiscard) {
            if (canDiscard) {
                HandSolver.discardOptions(human.hand, game.wildRank).minOf { it.second }
            } else {
                HandSolver.penalty(human.hand, game.wildRank)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FeltDark, RoundedCornerShape(12.dp))
                .border(
                    if (humanTurn) 2.dp else 0.dp,
                    if (humanTurn) Gold else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Votre main", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Total : ${human.total}", color = Color.White)
            }
            Text(
                "Points restants dans votre main : $pointsLeft",
                color = Color(0xFFB0D9C4),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            HandView(
                hand = human.hand,
                wildRank = game.wildRank,
                autoGroup = ui.autoGroup,
                selectedId = ui.selectedId,
                newCardId = if (game.current == 0) game.lastDrawn?.id else null,
                onCardClick = if (canDiscard) ({ card -> vm.selectCard(card.id) }) else null
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { vm.discardSelected() },
                    enabled = canDiscard && ui.selectedId != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Défausser") }
                Button(
                    onClick = { vm.goOut() },
                    enabled = goOutCard != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Sortir !") }
                OutlinedButton(onClick = { vm.toggleAutoGroup() }) {
                    Text(
                        if (ui.autoGroup) "Groupes : oui" else "Groupes : non",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun hintText(game: GameState, ui: UiState, canDraw: Boolean, canDiscard: Boolean): String {
    if (game.isOver) return ""
    val out = game.wentOut
    val prefix = if (out != null) "Dernier tour ! " else ""
    return when {
        canDraw -> prefix + "À vous : touchez la pioche ou la défausse."
        canDiscard -> prefix + "Touchez la carte à défausser, puis « Défausser »."
        else -> prefix + "${game.currentPlayer.name} réfléchit…"
    }
}

@Composable
private fun OpponentBox(
    player: Player,
    isCurrent: Boolean,
    wentOut: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .background(FeltDark, shape)
            .border(if (isCurrent) 2.dp else 1.dp, if (isCurrent) Gold else Color(0x33FFFFFF), shape)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(player.name, color = Color.White, fontWeight = FontWeight.Bold)
        Text("${player.hand.size} cartes", color = Color(0xFFB0D9C4), fontSize = 12.sp)
        Text("Total : ${player.total}", color = Color.White, fontSize = 12.sp)
        if (wentOut) {
            Text("est sorti", color = Gold, fontSize = 12.sp)
        }
    }
}
