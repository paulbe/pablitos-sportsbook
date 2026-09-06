package com.pablitosb.sportsbook.ui.hr

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablitosb.sportsbook.data.hr.HrBoard
import com.pablitosb.sportsbook.data.hr.HrRepository
import com.pablitosb.sportsbook.data.hr.HrSort
import com.pablitosb.sportsbook.data.hr.HrSorter
import com.pablitosb.sportsbook.data.projections.SlateLoadException
import com.pablitosb.sportsbook.data.starters.StartersRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface HrUiState {
    data class Loading(val slateDate: LocalDate) : HrUiState
    data class Ready(val board: HrBoard) : HrUiState
    data class Empty(
        val slateDate: LocalDate,
        val fetchedAt: Instant,
        val message: String,
        val sourceLabel: String,
    ) : HrUiState
    data class Error(val slateDate: LocalDate, val message: String) : HrUiState
}

class HrViewModel(
    private val repository: HrRepository = HrRepository(),
) : ViewModel() {

    val today: LocalDate get() = LocalDate.now(StartersRepository.SLATE_ZONE)
    val minDate: LocalDate
        get() {
            val now = today
            return if (now.monthValue >= 3) LocalDate.of(now.year, 3, 1) else LocalDate.of(now.year - 1, 3, 1)
        }
    val maxDate: LocalDate get() = today.plusDays(14)

    var selectedDate by mutableStateOf(today)
        private set
    var ui by mutableStateOf<HrUiState>(HrUiState.Loading(today))
        private set
    var refreshing by mutableStateOf(false)
        private set
    var sortKey by mutableStateOf(HrSort.GAME_HR)
        private set
    var sortAscending by mutableStateOf(HrSorter.defaultAscending(HrSort.GAME_HR))
        private set
    var selectedGamePks by mutableStateOf<Set<Int>>(emptySet())
        private set
    var allGamesSelected by mutableStateOf(true)
        private set

    private var gameFilterDate: LocalDate? = null
    private var loadJob: Job? = null

    init {
        refresh(initial = true)
    }

    fun selectSort(key: HrSort) {
        if (key == sortKey) {
            sortAscending = !sortAscending
        } else {
            sortKey = key
            sortAscending = HrSorter.defaultAscending(key)
        }
    }

    fun shiftDays(days: Long) = goTo(selectedDate.plusDays(days))
    fun goToday() = goTo(today)
    fun goTo(date: LocalDate) {
        val next = date.coerceIn(minDate, maxDate)
        if (next != selectedDate) {
            allGamesSelected = true
            gameFilterDate = null
            selectedGamePks = emptySet()
        }
        selectedDate = next
        refresh(initial = true)
    }

    fun applyGameFilter(pks: Set<Int>, allPks: Set<Int>) {
        if (pks.isEmpty()) return
        selectedGamePks = pks
        allGamesSelected = pks.size == allPks.size && pks.containsAll(allPks)
    }

    private fun syncGames(board: HrBoard) {
        val pks = board.slate.games.map { it.gamePk }.toSet()
        val date = board.slate.slateDate
        if (gameFilterDate != date || allGamesSelected) {
            selectedGamePks = pks
            allGamesSelected = true
            gameFilterDate = date
        } else {
            selectedGamePks = selectedGamePks.intersect(pks)
            if (selectedGamePks.isEmpty()) {
                selectedGamePks = pks
                allGamesSelected = true
            }
        }
    }

    fun refresh(initial: Boolean = false) {
        val slate = selectedDate
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            refreshing = true
            if (initial || ui !is HrUiState.Ready) ui = HrUiState.Loading(slate)
            ui = try {
                val board = repository.load(slate, force = !initial)
                if (board.batters.isEmpty()) {
                    HrUiState.Empty(
                        slateDate = board.slate.slateDate,
                        fetchedAt = board.slate.fetchedAt,
                        message = board.slate.emptyReason
                            ?: "No hitters available for $slate. Lineups may not be posted yet.",
                        sourceLabel = board.slate.sourceLabel,
                    )
                } else {
                    syncGames(board)
                    HrUiState.Ready(board)
                }
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (e: SlateLoadException) {
                HrUiState.Error(slate, e.message ?: "Live fetch failed.")
            } catch (e: Exception) {
                HrUiState.Error(slate, e.message ?: "Live fetch failed.")
            }
            refreshing = false
        }
    }
}
