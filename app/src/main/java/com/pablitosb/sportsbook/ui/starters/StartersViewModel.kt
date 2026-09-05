package com.pablitosb.sportsbook.ui.starters

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablitosb.sportsbook.data.starters.StartersBoard
import com.pablitosb.sportsbook.data.starters.StartersLoadException
import com.pablitosb.sportsbook.data.starters.StartersRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface StartersUiState {
    data class Loading(val slateDate: LocalDate) : StartersUiState
    data class Ready(val board: StartersBoard) : StartersUiState
    data class Empty(
        val slateDate: LocalDate,
        val fetchedAt: Instant,
        val message: String,
        val sourceLabel: String,
        val reconstructed: Boolean,
    ) : StartersUiState
    data class Error(val slateDate: LocalDate, val message: String) : StartersUiState
}

class StartersViewModel(
    private val repository: StartersRepository = StartersRepository(),
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

    var ui by mutableStateOf<StartersUiState>(StartersUiState.Loading(today))
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
        val clamped = date.coerceIn(minDate, maxDate)
        selectedDate = clamped
        refresh(initial = true)
    }

    fun refresh(initial: Boolean = false) {
        val slate = selectedDate
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            refreshing = true
            if (initial || ui !is StartersUiState.Ready) {
                ui = StartersUiState.Loading(slate)
            }
            ui = try {
                val board = repository.load(slate)
                if (board.starters.isEmpty()) {
                    StartersUiState.Empty(
                        slateDate = board.slateDate,
                        fetchedAt = board.fetchedAt,
                        message = board.emptyReason
                            ?: "No starters posted for ${board.slateDate}.",
                        sourceLabel = board.sourceLabel,
                        reconstructed = board.reconstructed,
                    )
                } else {
                    StartersUiState.Ready(board)
                }
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (e: StartersLoadException) {
                StartersUiState.Error(slate, e.message ?: "Live fetch failed.")
            } catch (e: Exception) {
                StartersUiState.Error(slate, e.message ?: "Live fetch failed.")
            }
            refreshing = false
        }
    }
}
