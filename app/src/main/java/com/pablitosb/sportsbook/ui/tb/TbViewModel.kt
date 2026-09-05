package com.pablitosb.sportsbook.ui.tb

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablitosb.sportsbook.data.projections.SlateLoadException
import com.pablitosb.sportsbook.data.starters.StartersRepository
import com.pablitosb.sportsbook.data.tb.TbBoard
import com.pablitosb.sportsbook.data.tb.TbRepository
import com.pablitosb.sportsbook.data.tb.TbSort
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface TbUiState {
    data class Loading(val slateDate: LocalDate) : TbUiState
    data class Ready(val board: TbBoard) : TbUiState
    data class Empty(
        val slateDate: LocalDate,
        val fetchedAt: Instant,
        val message: String,
        val sourceLabel: String,
    ) : TbUiState
    data class Error(val slateDate: LocalDate, val message: String) : TbUiState
}

class TbViewModel(
    private val repository: TbRepository = TbRepository(),
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
    var sortKey by mutableStateOf(TbSort.PROJ_TB)
        private set
    var sortAscending by mutableStateOf(false)
        private set
    var ui by mutableStateOf<TbUiState>(TbUiState.Loading(today))
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

    fun selectSort(key: TbSort) {
        if (key == sortKey) sortAscending = !sortAscending
        else {
            sortKey = key
            sortAscending = false
        }
    }

    fun refresh(initial: Boolean = false) {
        val slate = selectedDate
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            refreshing = true
            if (initial || ui !is TbUiState.Ready) ui = TbUiState.Loading(slate)
            ui = try {
                val board = repository.load(slate, force = !initial)
                if (board.batters.isEmpty()) {
                    TbUiState.Empty(
                        slateDate = board.slateDate,
                        fetchedAt = board.fetchedAt,
                        message = board.emptyReason ?: "No hitters available for $slate.",
                        sourceLabel = board.sourceLabel,
                    )
                } else {
                    TbUiState.Ready(board)
                }
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (e: SlateLoadException) {
                TbUiState.Error(slate, e.message ?: "Live fetch failed.")
            } catch (e: Exception) {
                TbUiState.Error(slate, e.message ?: "Live fetch failed.")
            }
            refreshing = false
        }
    }
}
