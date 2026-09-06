package com.pablitosb.sportsbook.data.toppicks

import com.pablitosb.sportsbook.data.hr.HrRepository
import com.pablitosb.sportsbook.data.projections.SlateLoadException
import com.pablitosb.sportsbook.data.starters.StartersLoadException
import com.pablitosb.sportsbook.data.starters.StartersRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TopPicksRepository(
    private val starters: StartersRepository = StartersRepository(),
    private val hr: HrRepository = HrRepository(),
) {
    suspend fun load(date: LocalDate, force: Boolean = false): TopPicksBoard = coroutineScope {
        val startersJob = async { runCatching { starters.load(date) } }
        val hrJob = async { runCatching { hr.load(date, force) } }

        val startersResult = startersJob.await()
        val hrResult = hrJob.await()

        if (startersResult.isFailure && hrResult.isFailure) {
            val cause = startersResult.exceptionOrNull() ?: hrResult.exceptionOrNull()
            when (cause) {
                is SlateLoadException -> throw cause
                is StartersLoadException -> throw cause
                else -> throw SlateLoadException("Couldn’t load today’s picks for $date.", cause)
            }
        }

        val startersBoard = startersResult.getOrNull()
        val hrBoard = hrResult.getOrNull()
        val fetchedAt = listOfNotNull(startersBoard?.fetchedAt, hrBoard?.slate?.fetchedAt).maxOrNull()
            ?: Instant.now()
        val source = listOfNotNull(startersBoard?.sourceLabel, hrBoard?.slate?.sourceLabel)
            .firstOrNull().orEmpty().ifBlank { "Live slate" }

        val pitchersNote = when {
            startersResult.isFailure -> startersResult.exceptionOrNull()?.message
                ?: "Couldn’t load projected starters."
            startersBoard?.starters.isNullOrEmpty() -> startersBoard?.emptyReason
                ?: "No probable starters posted."
            else -> null
        }
        val battersNote = when {
            hrResult.isFailure -> hrResult.exceptionOrNull()?.message
                ?: "Couldn’t load Daily Batters."
            hrBoard?.batters.isNullOrEmpty() -> hrBoard?.slate?.emptyReason
                ?: "No hitters posted."
            else -> null
        }

        TopPicksBoard(
            slateDate = date,
            fetchedAt = fetchedAt,
            sourceLabel = source,
            startersBoard = startersBoard,
            hrBoard = hrBoard,
            pitchersNote = pitchersNote,
            battersNote = battersNote,
            emptyReason = if (startersBoard?.starters.isNullOrEmpty() && hrBoard?.batters.isNullOrEmpty()) {
                "No live picks for $date. MLB may not have posted the slate yet."
            } else {
                null
            },
        )
    }
}
