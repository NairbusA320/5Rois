package fr.cinqrois.game

import kotlin.math.abs

/** Adversaire contrôlé par l'ordinateur. */
object Ai {

    /**
     * Faut-il prendre la carte de la défausse plutôt que piocher ?
     * Oui seulement si elle fait strictement baisser les points restants une fois la meilleure
     * carte défaussée (sinon deux joueurs pourraient se renvoyer la même carte indéfiniment).
     */
    fun takeFromDiscard(hand: List<Card>, top: Card, wildRank: Int): Boolean {
        val current = HandSolver.penalty(hand, wildRank)
        val afterTaking = HandSolver.discardOptions(hand + top, wildRank).minOf { it.second }
        return afterTaking < current
    }

    /** Choisit la carte à défausser dans une main de n+1 cartes. */
    fun chooseDiscard(hand: List<Card>, wildRank: Int): Card {
        val options = HandSolver.discardOptions(hand, wildRank)
        val lowest = options.minOf { it.second }
        val candidates = options.filter { it.second == lowest }.map { it.first }
        // À points égaux, on garde les cartes qui ont du potentiel et on se défait des grosses valeurs isolées.
        return candidates
            .sortedWith(compareBy<Card>({ synergy(it, hand, wildRank) }, { -it.points }))
            .first()
    }

    private fun synergy(card: Card, hand: List<Card>, wildRank: Int): Int {
        if (card.isWild(wildRank)) return 100
        var score = 0
        for (other in hand) {
            if (other.id == card.id || other.isWild(wildRank)) continue
            if (other.rank == card.rank) score += 2
            if (other.suit == card.suit && abs(other.rank - card.rank) <= 2) score += 1
        }
        return score
    }
}
