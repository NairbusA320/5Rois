package fr.cinqrois.game

import kotlin.random.Random

const val ROUNDS = 11

enum class Phase { DRAW, DISCARD, ROUND_OVER, GAME_OVER }

data class Player(
    val name: String,
    val isHuman: Boolean,
    val hand: List<Card> = emptyList(),
    /** Total des points après les manches terminées (le plus petit gagne). */
    val total: Int = 0,
    /** Points pris à la dernière manche terminée. */
    val roundPenalty: Int = 0
)

data class GameState(
    /** Numéro de la manche, de 1 à 11. */
    val round: Int,
    val players: List<Player>,
    /** Le dessus de la pioche est la dernière carte de la liste. */
    val deck: List<Card>,
    /** Le dessus de la défausse est la dernière carte de la liste. */
    val discardPile: List<Card>,
    val current: Int,
    val dealer: Int,
    val phase: Phase,
    /** Joueur qui est « sorti » pendant cette manche, s'il y en a un. */
    val wentOut: Int? = null,
    /** Nombre de tours qu'il reste à jouer aux autres après la sortie. */
    val turnsLeft: Int = 0,
    /** Dernière carte piochée par le joueur courant (pour la mettre en évidence). */
    val lastDrawn: Card? = null,
    val lastAction: String = "",
    /** Rangement final de chaque main, rempli quand la manche est terminée. */
    val arrangements: List<HandPlan> = emptyList()
) {
    /** Les cartes de ce rang (et les jokers) sont sauvages : 3 à la manche 1, ..., Roi à la manche 11. */
    val wildRank: Int get() = round + 2

    val handSize: Int get() = round + 2

    val topDiscard: Card? get() = discardPile.lastOrNull()

    val currentPlayer: Player get() = players[current]

    val isOver: Boolean get() = phase == Phase.ROUND_OVER || phase == Phase.GAME_OVER
}

object Engine {

    fun newGame(names: List<String>, random: Random): GameState {
        require(names.size in 2..6) { "Il faut entre 2 et 6 joueurs" }
        val players = names.mapIndexed { index, name -> Player(name, isHuman = index == 0) }
        val dealer = random.nextInt(players.size)
        return startRound(players, 1, dealer, random)
    }

    private fun startRound(players: List<Player>, round: Int, dealer: Int, random: Random): GameState {
        val n = players.size
        val deck = buildDeck(random).toMutableList()
        val hands = List(n) { ArrayList<Card>() }
        repeat(round + 2) {
            for (p in 0 until n) hands[p].add(deck.removeAt(deck.size - 1))
        }
        val first = deck.removeAt(deck.size - 1)
        val dealt = players.mapIndexed { i, p -> p.copy(hand = hands[i], roundPenalty = 0) }
        val starter = (dealer + 1) % n
        return GameState(
            round = round,
            players = dealt,
            deck = deck,
            discardPile = listOf(first),
            current = starter,
            dealer = dealer,
            phase = Phase.DRAW,
            lastAction = "Manche $round : les ${Card.rankName(round + 2)} et les jokers sont sauvages."
        )
    }

    fun nextRound(state: GameState, random: Random): GameState {
        require(state.phase == Phase.ROUND_OVER) { "La manche n'est pas terminée" }
        val dealer = (state.dealer + 1) % state.players.size
        val players = state.players.map { it.copy(hand = emptyList()) }
        return startRound(players, state.round + 1, dealer, random)
    }

    fun drawFromDeck(state: GameState, random: Random): GameState {
        require(state.phase == Phase.DRAW) { "Ce n'est pas le moment de piocher" }
        var deck = state.deck
        var discard = state.discardPile
        if (deck.isEmpty()) {
            // Pioche épuisée : on mélange la défausse (sauf sa carte du dessus) pour refaire une pioche.
            val top = discard.last()
            deck = discard.dropLast(1).shuffled(random)
            discard = listOf(top)
        }
        if (deck.isEmpty()) return state
        val card = deck.last()
        return giveCard(state, deck.dropLast(1), discard, card, "${state.currentPlayer.name} pioche.")
    }

    fun drawFromDiscard(state: GameState): GameState {
        require(state.phase == Phase.DRAW) { "Ce n'est pas le moment de piocher" }
        val card = state.discardPile.last()
        return giveCard(
            state, state.deck, state.discardPile.dropLast(1), card,
            "${state.currentPlayer.name} prend ${card.label} dans la défausse."
        )
    }

    private fun giveCard(
        state: GameState, deck: List<Card>, discard: List<Card>, card: Card, message: String
    ): GameState {
        val players = state.players.toMutableList()
        val p = players[state.current]
        players[state.current] = p.copy(hand = p.hand + card)
        return state.copy(
            players = players,
            deck = deck,
            discardPile = discard,
            phase = Phase.DISCARD,
            lastDrawn = card,
            lastAction = message
        )
    }

    /** Défausse [card] de la main du joueur courant. Si le reste de la main est entièrement rangé, il « sort ». */
    fun discard(state: GameState, card: Card): GameState {
        require(state.phase == Phase.DISCARD) { "Ce n'est pas le moment de défausser" }
        val player = state.currentPlayer
        require(player.hand.any { it.id == card.id }) { "Cette carte n'est pas dans la main" }

        val newHand = player.hand.filter { it.id != card.id }
        val players = state.players.toMutableList()
        players[state.current] = player.copy(hand = newHand)

        var wentOut = state.wentOut
        var turnsLeft = state.turnsLeft
        var message = "${player.name} défausse ${card.label}."
        if (wentOut == null) {
            if (HandSolver.penalty(newHand, state.wildRank) == 0) {
                wentOut = state.current
                turnsLeft = state.players.size - 1
                message = "${player.name} défausse ${card.label} et sort ! Les autres jouent un dernier tour."
            }
        } else {
            turnsLeft -= 1
        }

        val moved = state.copy(
            players = players,
            discardPile = state.discardPile + card,
            wentOut = wentOut,
            turnsLeft = turnsLeft,
            lastDrawn = null,
            lastAction = message
        )
        return if (wentOut != null && turnsLeft <= 0) {
            endRound(moved)
        } else {
            moved.copy(current = (state.current + 1) % state.players.size, phase = Phase.DRAW)
        }
    }

    private fun endRound(state: GameState): GameState {
        val arrangements = state.players.map { HandSolver.solve(it.hand, state.wildRank) }
        val players = state.players.mapIndexed { i, p ->
            val penalty = arrangements[i].penalty
            p.copy(total = p.total + penalty, roundPenalty = penalty)
        }
        return state.copy(
            players = players,
            arrangements = arrangements,
            phase = if (state.round >= ROUNDS) Phase.GAME_OVER else Phase.ROUND_OVER
        )
    }

    /** Indices des gagnants (score total le plus bas) une fois la partie terminée. */
    fun winners(state: GameState): List<Int> {
        val best = state.players.minOf { it.total }
        return state.players.indices.filter { state.players[it].total == best }
    }
}
