package com.pablitosb.sportsbook.ui.dfs

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pablitosb.sportsbook.data.dfs.DfsBoard
import com.pablitosb.sportsbook.data.dfs.DfsRepository
import com.pablitosb.sportsbook.data.model.ContestType
import com.pablitosb.sportsbook.data.projections.SlateLoadException
import com.pablitosb.sportsbook.data.starters.StartersRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface DfsUiState {
    data class Loading(val slateDate: LocalDate) : DfsUiState
    data class Ready(val board: DfsBoard) : DfsUiState
    data class Empty(
        val slateDate: LocalDate,
        val fetchedAt: Instant,
        val message: String,
        val sourceLabel: String,
        val board: DfsBoard? = null,
    ) : DfsUiState
    data class Error(val slateDate: LocalDate, val message: String) : DfsUiState
}

/** Application-only constructor so the default AndroidViewModel factory can instantiate us. */
class DfsViewModel(
    app: Application,
) : AndroidViewModel(app) {

    private val repository = DfsRepository()

    val today: LocalDate get() = LocalDate.now(StartersRepository.SLATE_ZONE)
    val minDate: LocalDate
        get() {
            val now = today
            return if (now.monthValue >= 3) LocalDate.of(now.year, 3, 1) else LocalDate.of(now.year - 1, 3, 1)
        }
    val maxDate: LocalDate get() = today.plusDays(14)

    var selectedDate by mutableStateOf(today)
        private set
    var contest by mutableStateOf(ContestType.GPP)
        private set
    var stackDots by mutableIntStateOf(4)
        private set
    var ownDots by mutableIntStateOf(3)
        private set
    var seed by mutableStateOf(1L)
        private set
    var currentLineupIndex by mutableIntStateOf(0)
    var importedText by mutableStateOf<String?>(null)
        private set
    var useExampleFile by mutableStateOf(false)
        private set
    var selectedSlateId by mutableStateOf("main")
        private set
    var ui by mutableStateOf<DfsUiState>(DfsUiState.Loading(today))
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

    fun selectContest(type: ContestType) {
        contest = type
        refresh()
    }

    fun selectSlate(id: String) {
        selectedSlateId = id
        currentLineupIndex = 0
        refresh(initial = true)
    }

    fun cycleStack() {
        stackDots = if (stackDots == 5) 2 else stackDots + 1
        refresh()
    }

    fun cycleOwn() {
        ownDots = if (ownDots == 5) 1 else ownDots + 1
        refresh()
    }

    fun regenerate() {
        seed += 1
        refresh()
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

    fun exportCsv(): String {
        val ready = ui as? DfsUiState.Ready ?: return ""
        return repository.exportCsv(ready.board.lineups)
    }

    fun copyCurrent(index: Int): String {
        val ready = ui as? DfsUiState.Ready ?: return ""
        val lineup = ready.board.lineups.getOrNull(index.coerceAtLeast(0)) ?: return ""
        return repository.copyLineup(lineup)
    }

    fun refresh(initial: Boolean = false) {
        val slate = selectedDate
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            refreshing = true
            if (initial || ui !is DfsUiState.Ready) ui = DfsUiState.Loading(slate)
            ui = try {
                val example = if (useExampleFile) exampleAsset() else null
                val board = repository.load(
                    date = slate,
                    contest = contest,
                    stackSize = stackDots,
                    ownLever = ownDots,
                    seed = seed,
                    importedText = importedText,
                    exampleFileText = example,
                    selectedSlateId = selectedSlateId,
                    force = !initial,
                )
                selectedSlateId = board.selectedSlateId
                DfsUiState.Ready(board)
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (e: SlateLoadException) {
                DfsUiState.Error(slate, e.message ?: "Live fetch failed.")
            } catch (e: Exception) {
                DfsUiState.Error(slate, e.message ?: "Live fetch failed.")
            }
            refreshing = false
        }
    }
}
