package com.pablitosb.sportsbook.ui.nfl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablitosb.sportsbook.data.nfl.QbBoard
import com.pablitosb.sportsbook.data.nfl.QbLoadException
import com.pablitosb.sportsbook.data.nfl.QbProjectionsRepository
import com.pablitosb.sportsbook.data.nfl.QbSort
import com.pablitosb.sportsbook.data.nfl.QbSorter
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface QbUiState {
    data object Loading : QbUiState
    data class Ready(val board: QbBoard) : QbUiState
    data class Empty(
        val week: Int,
        val seasonYear: Int,
        val fetchedAt: Instant,
        val message: String,
        val sourceLabel: String,
    ) : QbUiState
    data class Error(val week: Int, val message: String) : QbUiState
}

class QbProjectionsViewModel(
    private val repository: QbProjectionsRepository = QbProjectionsRepository(),
) : ViewModel() {

    var seasonYear by mutableStateOf(2026)
        private set
    var week by mutableStateOf(1)
        private set
    var ui by mutableStateOf<QbUiState>(QbUiState.Loading)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var sortKey by mutableStateOf(QbSort.PROJ_YDS)
        private set
    var sortAscending by mutableStateOf(QbSorter.defaultAscending(QbSort.PROJ_YDS))
        private set

    val minWeek = 1
    val maxWeek = 18

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching { repository.currentWeek() }.onSuccess { (year, w) ->
                seasonYear = year
                week = w
            }
            refresh(initial = true)
        }
    }

    fun selectSort(key: QbSort) {
        if (key == sortKey) {
            sortAscending = !sortAscending
        } else {
            sortKey = key
            sortAscending = QbSorter.defaultAscending(key)
        }
    }

    fun shiftWeek(delta: Int) {
        week = (week + delta).coerceIn(minWeek, maxWeek)
        refresh(initial = true)
    }

    fun refresh(initial: Boolean = false) {
        val y = seasonYear
        val w = week
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            refreshing = true
            if (initial || ui !is QbUiState.Ready) ui = QbUiState.Loading
            ui = try {
                val board = repository.load(y, w, force = !initial)
                if (board.qbs.isEmpty()) {
                    QbUiState.Empty(
                        week = w,
                        seasonYear = y,
                        fetchedAt = board.fetchedAt,
                        message = board.emptyReason ?: "No QBs for week $w.",
                        sourceLabel = board.sourceLabel,
                    )
                } else {
                    QbUiState.Ready(board)
                }
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (e: QbLoadException) {
                QbUiState.Error(w, e.message ?: "Live fetch failed.")
            } catch (e: Exception) {
                QbUiState.Error(w, e.message ?: "Live fetch failed.")
            }
            refreshing = false
        }
    }
}
