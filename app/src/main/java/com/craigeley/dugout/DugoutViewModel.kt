package com.craigeley.dugout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craigeley.dugout.api.BoxScore
import com.craigeley.dugout.api.DivisionStandings
import com.craigeley.dugout.api.GameSummary
import com.craigeley.dugout.api.LineScore
import com.craigeley.dugout.api.MlbApi
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Tab { SCORES, STANDINGS }

data class UiState(
    val tab: Tab = Tab.SCORES,
    val date: LocalDate = LocalDate.now(),
    val games: List<GameSummary> = emptyList(),
    val gamesLoaded: Boolean = false,
    val gamesLoading: Boolean = false,
    val standings: List<DivisionStandings> = emptyList(),
    val standingsLoading: Boolean = false,
    val selected: GameSummary? = null,
    val lineScore: LineScore? = null,
    val boxScore: BoxScore? = null,
    val detailLoading: Boolean = false,
    val message: String? = null,
)

class DugoutViewModel : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    /** First load when the scores screen opens; no-op once anything is loaded. */
    fun loadInitial() {
        if (!_state.value.gamesLoaded && !_state.value.gamesLoading) refreshGames()
    }

    fun setTab(tab: Tab) {
        _state.update { it.copy(tab = tab) }
        if (tab == Tab.STANDINGS && _state.value.standings.isEmpty()) refreshStandings()
    }

    fun previousDay() = setDate(_state.value.date.minusDays(1))
    fun nextDay() = setDate(_state.value.date.plusDays(1))
    fun goToToday() = setDate(LocalDate.now())

    private fun setDate(date: LocalDate) {
        if (date == _state.value.date) return
        _state.update { it.copy(date = date, games = emptyList(), gamesLoaded = false, message = null) }
        refreshGames()
    }

    fun refreshGames(silent: Boolean = false) {
        if (_state.value.gamesLoading) return
        val date = _state.value.date
        viewModelScope.launch {
            if (!silent) _state.update { it.copy(gamesLoading = true, message = null) }
            runCatching { MlbApi.scoreboard(date.toString()) }
                .onSuccess { games ->
                    _state.update {
                        // A silent poll for a date the user has already left is stale.
                        if (it.date != date) it
                        else it.copy(games = games, gamesLoaded = true, gamesLoading = false)
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            gamesLoading = false,
                            message = if (silent) it.message else t.message ?: "Request failed",
                        )
                    }
                }
        }
    }

    fun refreshStandings() {
        if (_state.value.standingsLoading) return
        viewModelScope.launch {
            _state.update { it.copy(standingsLoading = true, message = null) }
            runCatching { MlbApi.standings(LocalDate.now().year) }
                .onSuccess { divisions ->
                    _state.update { it.copy(standings = divisions, standingsLoading = false) }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(standingsLoading = false, message = t.message ?: "Request failed")
                    }
                }
        }
    }

    fun select(game: GameSummary) {
        // Nothing to show before first pitch.
        if (game.abstractState == "Preview") return
        _state.update {
            it.copy(selected = game, lineScore = null, boxScore = null, detailLoading = true, message = null)
        }
        loadDetail(game, silent = false)
    }

    fun refreshDetail(silent: Boolean = true) {
        val game = _state.value.selected ?: return
        loadDetail(game, silent)
    }

    private fun loadDetail(game: GameSummary, silent: Boolean) {
        viewModelScope.launch {
            if (!silent) _state.update { it.copy(detailLoading = true) }
            runCatching {
                coroutineScope {
                    val line = async { MlbApi.lineScore(game.gamePk) }
                    val box = async { MlbApi.boxScore(game.gamePk) }
                    line.await() to box.await()
                }
            }
                .onSuccess { (line, box) ->
                    _state.update {
                        if (it.selected?.gamePk != game.gamePk) it
                        else it.copy(lineScore = line, boxScore = box, detailLoading = false)
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            detailLoading = false,
                            message = if (silent) it.message else t.message ?: "Request failed",
                        )
                    }
                }
        }
    }

    fun closeDetail() {
        _state.update {
            it.copy(selected = null, lineScore = null, boxScore = null, detailLoading = false)
        }
        // Pick up anything that changed while the box score was open.
        refreshGames(silent = true)
    }
}
