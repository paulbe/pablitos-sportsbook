package com.pablitosb.sportsbook.ui.fdproj

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pablitosb.sportsbook.data.fdproj.FdPosFilter
import com.pablitosb.sportsbook.data.fdproj.FdProjBoard
import com.pablitosb.sportsbook.data.fdproj.FdProjRepository
import com.pablitosb.sportsbook.data.fdproj.FdProjSort
import com.pablitosb.sportsbook.data.projections.SlateLoadException
import com.pablitosb.sportsbook.data.starters.StartersRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface FdProjUiState {
    data class Loading(val slateDate: LocalDate) : FdProjUiState
    data class Ready(val board: FdProjBoard) : FdProjUiState
    data class Empty(
        val slateDate: LocalDate,
        val fetchedAt: Instant,
        val message: String,
        val sourceLabel: String,
        val board: FdProjBoard? = null,
    ) : FdProjUiState
    data class Error(val slateDate: LocalDate, val message: String) : FdProjUiState
}

class FdProjViewModel(
    app: Application,
) : AndroidViewModel(app) {

    private val repository = FdProjRepository()

    val today: LocalDate get() = LocalDate.now(StartersRepository.SLATE_ZONE)
    val minDate: LocalDate
        get() {
            val now = today
            return if (now.monthValue >= 3) LocalDate.of(now.year, 3, 1) else LocalDate.of(now.year - 1, 3, 1)
        }
    val maxDate: LocalDate get() = today.plusDays(14)

    var selectedDate by mutableStateOf(today)
        private set
    var selectedSlateId by mutableStateOf("main")
        private set
    var sortKey by mutableStateOf(FdProjSort.PROJ)
        private set
    var sortAscending by mutableStateOf(false)
        private set
    var posFilter by mutableStateOf(FdPosFilter.ALL)
        private set
    var importedText by mutableStateOf<String?>(null)
        private set
    var useExampleFile by mutableStateOf(false)
        private set
    var ui by mutableStateOf<FdProjUiState>(FdProjUiState.Loading(today))
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
        selectedSlateId = "main"
        refresh(initial = true)
    }

    fun selectSlate(id: String) {
        selectedSlateId = id
        refresh(initial = true)
    }

    fun selectSort(key: FdProjSort) {
        if (key == sortKey) sortAscending = !sortAscending
        else {
            sortKey = key
            sortAscending = key == FdProjSort.POS
        }
    }

    fun selectPos(filter: FdPosFilter) {
        posFilter = if (posFilter == filter) FdPosFilter.ALL else filter
    }

    fun applyImport(text: String) {
        importedText = text
        useExampleFile = false
        selectedSlateId = "imported"
        refresh(initial = true)
    }

    fun loadExampleFile() {
        importedText = null
        useExampleFile = true
        refresh(initial = true)
    }

    fun clearImport() {
        importedText = null
        useExampleFile = false
        if (selectedSlateId == "imported") selectedSlateId = "main"
        refresh(initial = true)
    }

    fun exampleAsset(): String = runCatching {
        getApplication<Application>().assets.open("example_fd_slate.csv").bufferedReader().use { it.readText() }
    }.getOrDefault("")

    fun refresh(initial: Boolean = false) {
        val slate = selectedDate
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            refreshing = true
            if (initial || ui !is FdProjUiState.Ready) ui = FdProjUiState.Loading(slate)
            ui = try {
                val example = if (useExampleFile) exampleAsset() else null
                val board = repository.load(
                    date = slate,
                    importedText = importedText,
                    exampleFileText = example,
                    selectedSlateId = selectedSlateId,
                    force = !initial,
                )
                selectedSlateId = board.selectedSlateId
                if (board.rows.isEmpty()) {
                    FdProjUiState.Empty(
                        slateDate = board.slateDate,
                        fetchedAt = board.fetchedAt,
                        message = board.emptyReason ?: "No projections for this slate.",
                        sourceLabel = board.sourceLabel,
                        board = board,
                    )
                } else {
                    FdProjUiState.Ready(board)
                }
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (e: SlateLoadException) {
                FdProjUiState.Error(slate, e.message ?: "Live fetch failed.")
            } catch (e: Exception) {
                FdProjUiState.Error(slate, e.message ?: "Live fetch failed.")
            }
            refreshing = false
        }
    }
}
