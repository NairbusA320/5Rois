package fr.cinqrois.game

import kotlin.random.Random

/** Les cinq couleurs du jeu : les quatre classiques + les étoiles. */
enum class Suit(val symbol: String, val label: String) {
    SPADES("♠", "Pique"),
    HEARTS("♥", "Cœur"),
    CLUBS("♣", "Trèfle"),
    DIAMONDS("♦", "Carreau"),
    STARS("★", "Étoile")
}

/**
 * Une carte. [id] est unique dans le paquet (0..115) car le jeu utilise deux paquets identiques.
 * Un joker n'a pas de couleur ([suit] == null) et un rang de 0.
 * Rangs : 3 à 10, 11 = Valet, 12 = Dame, 13 = Roi.
 */
data class Card(val id: Int, val suit: Suit?, val rank: Int) {

    val isJoker: Boolean get() = suit == null

    /** Une carte est sauvage si c'est un joker ou si son rang est celui de la manche. */
    fun isWild(wildRank: Int): Boolean = isJoker || rank == wildRank

    /** Valeur en points si la carte reste dans la main en fin de manche. */
    val points: Int get() = if (isJoker) JOKER_POINTS else rank

    val rankLabel: String
        get() = when (rank) {
            0 -> "JOKER"
            11 -> "V"
            12 -> "D"
            13 -> "R"
            else -> rank.toString()
        }

    val label: String
        get() = if (isJoker) "Joker" else "$rankLabel${suit!!.symbol}"

    /** Deux cartes identiques (même couleur, même rang) sont interchangeables pour le calcul. */
    fun sameFace(other: Card): Boolean = suit == other.suit && rank == other.rank

    companion object {
        const val JOKER_POINTS = 50
        const val MIN_RANK = 3
        const val MAX_RANK = 13

        fun rankName(rank: Int): String = when (rank) {
            11 -> "Valet"
            12 -> "Dame"
            13 -> "Roi"
            else -> rank.toString()
        }
    }
}

/** Deux paquets de 58 cartes (5 couleurs x 11 rangs + 3 jokers) = 116 cartes. */
fun buildDeck(random: Random): List<Card> {
    val cards = ArrayList<Card>(116)
    var id = 0
    repeat(2) {
        for (suit in Suit.values()) {
            for (rank in Card.MIN_RANK..Card.MAX_RANK) {
                cards.add(Card(id++, suit, rank))
            }
        }
        repeat(3) { cards.add(Card(id++, null, 0)) }
    }
    return cards.shuffled(random)
}
