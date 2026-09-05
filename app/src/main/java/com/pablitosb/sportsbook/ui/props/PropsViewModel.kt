package com.pablitosb.sportsbook.ui.props

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pablitosb.sportsbook.data.props.ParsedPropLine
import com.pablitosb.sportsbook.data.props.PropsBoard
import com.pablitosb.sportsbook.data.props.PropsRepository
import com.pablitosb.sportsbook.data.projections.SlateLoadException
import com.pablitosb.sportsbook.data.starters.StartersRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface PropsUiState {
    data class Loading(val slateDate: LocalDate) : PropsUiState
    data class Ready(val board: PropsBoard) : PropsUiState
    data class Empty(
        val slateDate: LocalDate,
        val fetchedAt: Instant,
        val message: String,
        val sourceLabel: String,
    ) : PropsUiState
    data class Error(val slateDate: LocalDate, val message: String) : PropsUiState
}

/** Application-only constructor so the default AndroidViewModel factory can instantiate us. */
class PropsViewModel(
    app: Application,
) : AndroidViewModel(app) {

    private val repository = PropsRepository()

    val today: LocalDate get() = LocalDate.now(StartersRepository.SLATE_ZONE)
    val minDate: LocalDate
        get() {
            val now = today
            return if (now.monthValue >= 3) LocalDate.of(now.year, 3, 1) else LocalDate.of(now.year - 1, 3, 1)
        }
    val maxDate: LocalDate get() = today.plusDays(14)

    var selectedDate by mutableStateOf(today)
        private set
    var imported by mutableStateOf<List<ParsedPropLine>>(emptyList())
        private set
    var minEdge by mutableStateOf(0f)
        private set
    var ui by mutableStateOf<PropsUiState>(PropsUiState.Loading(today))
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

    fun cycleMinEdge() {
        minEdge = if (minEdge >= 5f) 0f else minEdge + 1f
    }

    fun applyImport(text: String) {
        imported = runCatching { PropsRepository.parseImport(text) }.getOrDefault(emptyList())
        refresh(initial = true)
    }

    fun loadExampleFile() {
        val text = runCatching {
            getApplication<Application>().assets.open("example_underdog_lines.csv")
                .bufferedReader().use { it.readText() }
        }.getOrDefault("")
        applyImport(text)
    }

    fun clearImport() {
        imported = emptyList()
        refresh(initial = true)
    }

    fun refresh(initial: Boolean = false) {
        val slate = selectedDate
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            refreshing = true
            if (initial || ui !is PropsUiState.Ready) ui = PropsUiState.Loading(slate)
            ui = try {
                val board = repository.load(slate, imported, force = !initial)
                if (board.props.isEmpty()) {
                    PropsUiState.Empty(
                        slateDate = board.slate.slateDate,
                        fetchedAt = board.slate.fetchedAt,
                        message = board.slate.emptyReason ?: "No model props for $slate.",
                        sourceLabel = board.sourceLabel,
                    )
                } else {
                    PropsUiState.Ready(board)
                }
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (e: SlateLoadException) {
                PropsUiState.Error(slate, e.message ?: "Live fetch failed.")
            } catch (e: Exception) {
                PropsUiState.Error(slate, e.message ?: "Live fetch failed.")
            }
            refreshing = false
        }
    }
}
