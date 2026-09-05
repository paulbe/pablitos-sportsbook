package com.pablitosb.sportsbook.ui.toppicks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablitosb.sportsbook.data.projections.SlateLoadException
import com.pablitosb.sportsbook.data.starters.StartersLoadException
import com.pablitosb.sportsbook.data.starters.StartersRepository
import com.pablitosb.sportsbook.data.toppicks.TopPicksBoard
import com.pablitosb.sportsbook.data.toppicks.TopPicksRepository
import com.pablitosb.sportsbook.data.toppicks.TopPicksSection
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface TopPicksUiState {
    data class Loading(val slateDate: LocalDate) : TopPicksUiState
    data class Ready(val board: TopPicksBoard) : TopPicksUiState
    data class Empty(
        val slateDate: LocalDate,
        val fetchedAt: Instant,
        val message: String,
        val sourceLabel: String,
    ) : TopPicksUiState
    data class Error(val slateDate: LocalDate, val message: String) : TopPicksUiState
}

class TopPicksViewModel(
    private val repository: TopPicksRepository = TopPicksRepository(),
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
    var section by mutableStateOf(TopPicksSection.ALL)
        private set
    var ui by mutableStateOf<TopPicksUiState>(TopPicksUiState.Loading(today))
        private set
    var refreshing by mutableStateOf(false)
        private set

    private var loadJob: Job? = null

    init {
        refresh(initial = true)
    }

    fun shiftDays(days: Long) = goTo(selectedDate.plusDays(days))
    fun goToday() = goTo(today)
    fun goTo(date: LocalDate) {
        selectedDate = date.coerceIn(minDate, maxDate)
        refresh(initial = true)
    }

    fun selectSection(next: TopPicksSection) {
        section = if (section == next && next != TopPicksSection.ALL) TopPicksSection.ALL else next
    }

    fun refresh(initial: Boolean = false) {
        val slate = selectedDate
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            refreshing = true
            if (initial || ui !is TopPicksUiState.Ready) ui = TopPicksUiState.Loading(slate)
            ui = try {
                val board = repository.load(slate, force = !initial)
                if (!board.hasAny) {
                    TopPicksUiState.Empty(
                        slateDate = board.slateDate,
                        fetchedAt = board.fetchedAt,
                        message = board.emptyReason ?: "No live picks for $slate.",
                        sourceLabel = board.sourceLabel,
                    )
                } else {
                    TopPicksUiState.Ready(board)
                }
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (e: SlateLoadException) {
                TopPicksUiState.Error(slate, e.message ?: "Live fetch failed.")
            } catch (e: StartersLoadException) {
                TopPicksUiState.Error(slate, e.message ?: "Live fetch failed.")
            } catch (e: Exception) {
                TopPicksUiState.Error(slate, e.message ?: "Live fetch failed.")
            }
            refreshing = false
        }
    }
}
