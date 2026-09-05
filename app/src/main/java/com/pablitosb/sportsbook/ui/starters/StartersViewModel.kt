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
import kotlinx.coroutines.launch

sealed interface StartersUiState {
    data object Loading : StartersUiState
    data class Ready(val board: StartersBoard) : StartersUiState
    data class Empty(val slateDate: LocalDate, val fetchedAt: Instant, val message: String) : StartersUiState
    data class Error(val message: String) : StartersUiState
}

class StartersViewModel(
    private val repository: StartersRepository = StartersRepository(),
) : ViewModel() {

    var ui by mutableStateOf<StartersUiState>(StartersUiState.Loading)
        private set

    var refreshing by mutableStateOf(false)
        private set

    init {
        refresh(initial = true)
    }

    fun refresh(initial: Boolean = false) {
        if (refreshing) return
        viewModelScope.launch {
            refreshing = true
            if (initial || ui is StartersUiState.Error || ui is StartersUiState.Empty) {
                ui = StartersUiState.Loading
            }
            ui = try {
                val board = repository.loadToday()
                if (board.starters.isEmpty()) {
                    StartersUiState.Empty(
                        slateDate = board.slateDate,
                        fetchedAt = board.fetchedAt,
                        message = "No probable starters posted for ${board.slateDate}. MLB may not have announced SPs yet, or there are no games on this slate.",
                    )
                } else {
                    StartersUiState.Ready(board)
                }
            } catch (e: StartersLoadException) {
                StartersUiState.Error(e.message ?: "Live fetch failed.")
            } catch (e: Exception) {
                StartersUiState.Error(e.message ?: "Live fetch failed.")
            }
            refreshing = false
        }
    }
}
