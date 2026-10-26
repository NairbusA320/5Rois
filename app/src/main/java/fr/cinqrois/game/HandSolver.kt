package fr.cinqrois.game

/**
 * Un groupe posé : un brelan / carré (même rang, couleurs libres) ou une suite
 * (même couleur, rangs consécutifs, sans passer de 13 à 3). 3 cartes minimum.
 * Pour une suite, [cards] est rangée du plus petit au plus grand rang et [runLow] est le rang de la première.
 */
data class Group(val cards: List<Card>, val isRun: Boolean, val runLow: Int = 0)

/** La meilleure façon de ranger une main : des groupes valides + les cartes qui restent (et coûtent des points). */
data class HandPlan(val groups: List<Group>, val leftover: List<Card>) {
    val penalty: Int get() = leftover.sumOf { it.points }
}

/**
 * Cherche le rangement d'une main qui minimise les points restants.
 *
 * Les cartes sauvages (jokers + cartes du rang de la manche) remplacent n'importe quelle carte.
 * Principe : on cherche par programmation dynamique comment placer les cartes « naturelles »
 * (en utilisant au besoin des sauvages), puis les sauvages restantes sont ajoutées gratuitement
 * à un groupe existant (ou forment un groupe entre elles si elles sont au moins trois).
 */
object HandSolver {

    fun solve(hand: List<Card>, wildRank: Int): HandPlan = Solver(hand, wildRank).run()

    fun penalty(hand: List<Card>, wildRank: Int): Int = solve(hand, wildRank).penalty

    /**
     * Pour chaque carte distincte de [hand] : les points qui resteraient en main si on la défaussait.
     * Un résultat à 0 signifie qu'en la défaussant on peut « sortir ».
     */
    fun discardOptions(hand: List<Card>, wildRank: Int): List<Pair<Card, Int>> {
        val seen = ArrayList<Card>()
        val result = ArrayList<Pair<Card, Int>>()
        for (card in hand) {
            if (seen.any { it.sameFace(card) }) continue
            seen.add(card)
            val rest = hand.filter { it.id != card.id }
            result.add(Pair(card, penalty(rest, wildRank)))
        }
        return result
    }

    /** La carte à défausser pour sortir (main de n+1 cartes), ou null si ce n'est pas possible. */
    fun goOutDiscard(hand: List<Card>, wildRank: Int): Card? =
        discardOptions(hand, wildRank).firstOrNull { it.second == 0 }?.first
}

private const val WILD_SLOT = -1

private class Move(val skip: Boolean, val slots: IntArray, val isRun: Boolean, val runLow: Int)

private class Best(var cost: Int, var move: Move)

private class Solver(hand: List<Card>, wildRank: Int) {

    private val naturals: List<Card> = hand
        .filter { !it.isWild(wildRank) }
        .sortedWith(compareBy<Card>({ it.suit!!.ordinal }, { it.rank }, { it.id }))
    private val wilds: List<Card> = hand
        .filter { it.isWild(wildRank) }
        .sortedByDescending { it.points }

    private val n = naturals.size
    private val wildCount = wilds.size
    private val suitOf = IntArray(n) { naturals[it].suit!!.ordinal }
    private val rankOf = IntArray(n) { naturals[it].rank }

    /** Indice de la carte identique précédente (même couleur et même rang), ou -1. */
    private val dupPrev = IntArray(n) { i ->
        if (i > 0 && suitOf[i] == suitOf[i - 1] && rankOf[i] == rankOf[i - 1]) i - 1 else -1
    }

    private val cost = HashMap<Int, Int>()
    private val choice = HashMap<Int, Move>()

    init {
        require(hand.size <= 20) { "Main trop grande : ${hand.size}" }
    }

    private fun has(mask: Int, i: Int): Boolean = ((mask shr i) and 1) == 1

    private fun without(mask: Int, i: Int): Int = mask and (1 shl i).inv()

    private fun findNatural(mask: Int, suit: Int, rank: Int): Int {
        for (i in 0 until n) {
            if (has(mask, i) && suitOf[i] == suit && rankOf[i] == rank) return i
        }
        return -1
    }

    /** Points minimum des cartes naturelles de [mask] avec [rem] sauvages encore disponibles. */
    private fun f(mask: Int, rem: Int): Int {
        if (mask == 0) return 0
        val key = mask * 32 + rem
        val known = cost[key]
        if (known != null) return known

        val c = Integer.numberOfTrailingZeros(mask)
        val afterC = without(mask, c)

        // Option 1 : la carte c reste seule.
        val best = Best(
            naturals[c].points + f(afterC, rem),
            Move(true, IntArray(0), false, 0)
        )

        // Option 2 : c fait partie d'un brelan / carré.
        val rankC = rankOf[c]
        val others = ArrayList<Int>()
        for (i in c + 1 until n) {
            if (has(mask, i) && rankOf[i] == rankC) others.add(i)
        }
        val m = others.size
        for (sub in 0 until (1 shl m)) {
            val s = Integer.bitCount(sub)
            val need = if (s >= 2) 0 else 2 - s
            if (need > rem) continue
            // Les copies identiques sont interchangeables : on prend toujours la première d'abord.
            var ok = true
            for (b in 0 until m) {
                if (((sub shr b) and 1) == 0) continue
                val p = dupPrev[others[b]]
                if (p >= 0 && p != c && has(mask, p)) {
                    val pb = others.indexOf(p)
                    if (pb < 0 || ((sub shr pb) and 1) == 0) {
                        ok = false
                        break
                    }
                }
            }
            if (!ok) continue
            var newMask = afterC
            val slots = IntArray(1 + s + need)
            var k = 0
            slots[k++] = c
            for (b in 0 until m) {
                if (((sub shr b) and 1) == 1) {
                    newMask = without(newMask, others[b])
                    slots[k++] = others[b]
                }
            }
            for (j in 0 until need) slots[k++] = WILD_SLOT
            val total = f(newMask, rem - need)
            if (total < best.cost) {
                best.cost = total
                best.move = Move(false, slots, false, 0)
            }
        }

        // Option 3 : c fait partie d'une suite de sa couleur.
        val suitC = suitOf[c]
        val lowest = maxOf(Card.MIN_RANK, rankC - rem)
        for (lo in rankC downTo lowest) {
            val lowWilds = rankC - lo
            val slots = ArrayList<Int>()
            for (j in 0 until lowWilds) slots.add(WILD_SLOT)
            slots.add(c)
            extendRun(best, afterC, rem - lowWilds, lo, rankC, suitC, slots)
        }

        cost[key] = best.cost
        choice[key] = best.move
        return best.cost
    }

    private fun extendRun(
        best: Best, mask: Int, rem: Int, lo: Int, hi: Int, suit: Int, slots: ArrayList<Int>
    ) {
        if (hi - lo + 1 >= 3) {
            val total = f(mask, rem)
            if (total < best.cost) {
                best.cost = total
                best.move = Move(false, slots.toIntArray(), true, lo)
            }
        }
        if (hi >= Card.MAX_RANK) return
        val next = hi + 1
        val natural = findNatural(mask, suit, next)
        if (natural >= 0) {
            slots.add(natural)
            extendRun(best, without(mask, natural), rem, lo, next, suit, slots)
            slots.removeAt(slots.size - 1)
        }
        if (rem > 0) {
            slots.add(WILD_SLOT)
            extendRun(best, mask, rem - 1, lo, next, suit, slots)
            slots.removeAt(slots.size - 1)
        }
    }

    fun run(): HandPlan {
        var mask = if (n == 0) 0 else (1 shl n) - 1
        var rem = wildCount
        f(mask, rem)

        val groups = ArrayList<Group>()
        val leftover = ArrayList<Card>()
        var wildPtr = 0

        while (mask != 0) {
            val move = choice[mask * 32 + rem]!!
            if (move.skip) {
                val c = Integer.numberOfTrailingZeros(mask)
                leftover.add(naturals[c])
                mask = without(mask, c)
            } else {
                val cards = ArrayList<Card>()
                var used = 0
                for (slot in move.slots) {
                    if (slot == WILD_SLOT) {
                        cards.add(wilds[wildPtr++])
                        used++
                    } else {
                        cards.add(naturals[slot])
                        mask = without(mask, slot)
                    }
                }
                rem -= used
                groups.add(Group(cards, move.isRun, move.runLow))
            }
        }

        // Les sauvages restantes s'ajoutent gratuitement à un groupe existant.
        val unplaced = ArrayList<Card>()
        while (wildPtr < wilds.size) {
            val w = wilds[wildPtr++]
            var placed = false
            for (i in groups.indices) {
                val g = groups[i]
                if (!g.isRun) {
                    groups[i] = g.copy(cards = g.cards + w)
                    placed = true
                    break
                }
            }
            if (!placed) {
                for (i in groups.indices) {
                    val g = groups[i]
                    if (g.isRun) {
                        val hi = g.runLow + g.cards.size - 1
                        if (hi < Card.MAX_RANK) {
                            groups[i] = g.copy(cards = g.cards + w)
                            placed = true
                            break
                        } else if (g.runLow > Card.MIN_RANK) {
                            groups[i] = g.copy(cards = listOf(w) + g.cards, runLow = g.runLow - 1)
                            placed = true
                            break
                        }
                    }
                }
            }
            if (!placed) unplaced.add(w)
        }
        if (unplaced.size >= 3) {
            groups.add(Group(unplaced, false))
        } else {
            leftover.addAll(unplaced)
        }

        leftover.sortWith(compareByDescending<Card> { it.points }.thenBy { it.id })
        return HandPlan(groups, leftover)
    }
}
