package fr.cinqrois.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.cinqrois.game.Ai
import fr.cinqrois.game.Engine
import fr.cinqrois.game.GameState
import fr.cinqrois.game.HandSolver
import fr.cinqrois.game.Phase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class UiState(
    /** Null tant qu'aucune partie n'est lancée (écran d'accueil). */
    val game: GameState? = null,
    /** Carte sélectionnée dans la main du joueur, pour la défausser. */
    val selectedId: Int? = null,
    /** Vrai pendant que les adversaires jouent. */
    val aiPlaying: Boolean = false,
    /** Regroupe automatiquement la main en combinaisons (aide visuelle). */
    val autoGroup: Boolean = true
)

class GameViewModel : ViewModel() {

    private val random = Random.Default
    private val botNames = listOf("Léa", "Hugo", "Nina")

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var aiJob: Job? = null

    /** Le joueur humain est toujours le joueur 0. */
    private fun humanCanDraw(game: GameState): Boolean =
        game.current == 0 && game.phase == Phase.DRAW && !_ui.value.aiPlaying

    private fun humanCanDiscard(game: GameState): Boolean =
        game.current == 0 && game.phase == Phase.DISCARD && !_ui.value.aiPlaying

    fun startGame(opponents: Int) {
        aiJob?.cancel()
        val count = opponents.coerceIn(1, botNames.size)
        val names = listOf("Vous") + botNames.take(count)
        _ui.value = UiState(game = Engine.newGame(names, random), autoGroup = _ui.value.autoGroup)
        runAiIfNeeded()
    }

    fun quit() {
        aiJob?.cancel()
        _ui.value = UiState(autoGroup = _ui.value.autoGroup)
    }

    fun toggleAutoGroup() {
        _ui.update { it.copy(autoGroup = !it.autoGroup) }
    }

    fun drawFromDeck() {
        val game = _ui.value.game ?: return
        if (!humanCanDraw(game)) return
        _ui.update { it.copy(game = Engine.drawFromDeck(game, random), selectedId = null) }
    }

    fun drawFromDiscard() {
        val game = _ui.value.game ?: return
        if (!humanCanDraw(game) || game.discardPile.isEmpty()) return
        _ui.update { it.copy(game = Engine.drawFromDiscard(game), selectedId = null) }
    }

    fun selectCard(cardId: Int) {
        val game = _ui.value.game ?: return
        if (!humanCanDiscard(game)) return
        _ui.update { it.copy(selectedId = if (it.selectedId == cardId) null else cardId) }
    }

    fun discardSelected() {
        val state = _ui.value
        val game = state.game ?: return
        val id = state.selectedId ?: return
        if (!humanCanDiscard(game)) return
        val card = game.players[0].hand.firstOrNull { it.id == id } ?: return
        applyHumanDiscard(game, card)
    }

    /** Défausse la carte qui permet de sortir, si elle existe. */
    fun goOut() {
        val game = _ui.value.game ?: return
        if (!humanCanDiscard(game)) return
        val card = HandSolver.goOutDiscard(game.players[0].hand, game.wildRank) ?: return
        applyHumanDiscard(game, card)
    }

    private fun applyHumanDiscard(game: GameState, card: fr.cinqrois.game.Card) {
        _ui.update { it.copy(game = Engine.discard(game, card), selectedId = null) }
        runAiIfNeeded()
    }

    fun nextRound() {
        val game = _ui.value.game ?: return
        if (game.phase != Phase.ROUND_OVER) return
        _ui.update { it.copy(game = Engine.nextRound(game, random), selectedId = null) }
        runAiIfNeeded()
    }

    private fun runAiIfNeeded() {
        val game = _ui.value.game ?: return
        if (aiJob?.isActive == true) return
        if (game.isOver || game.currentPlayer.isHuman) return

        aiJob = viewModelScope.launch {
            _ui.update { it.copy(aiPlaying = true) }
            while (true) {
                val before = _ui.value.game ?: break
                if (before.isOver || before.currentPlayer.isHuman) break

                delay(800)
                val afterDraw = withContext(Dispatchers.Default) {
                    val hand = before.currentPlayer.hand
                    val top = before.topDiscard
                    if (top != null && Ai.takeFromDiscard(hand, top, before.wildRank)) {
                        Engine.drawFromDiscard(before)
                    } else {
                        Engine.drawFromDeck(before, random)
                    }
                }
                _ui.update { it.copy(game = afterDraw) }

                delay(800)
                val afterDiscard = withContext(Dispatchers.Default) {
                    val hand = afterDraw.currentPlayer.hand
                    Engine.discard(afterDraw, Ai.chooseDiscard(hand, afterDraw.wildRank))
                }
                _ui.update { it.copy(game = afterDiscard) }
            }
            _ui.update { it.copy(aiPlaying = false) }
        }
    }
}
