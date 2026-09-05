package com.pablitosb.sportsbook.data.toppicks

import com.pablitosb.sportsbook.data.fdproj.FdProjRepository
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
    private val fd: FdProjRepository = FdProjRepository(),
) {
    suspend fun load(date: LocalDate, force: Boolean = false): TopPicksBoard = coroutineScope {
        val startersJob = async { runCatching { starters.load(date) } }
        val hrJob = async { runCatching { hr.load(date, force) } }
        val fdJob = async { runCatching { fd.load(date, null, null, "main", force) } }

        val startersResult = startersJob.await()
        val hrResult = hrJob.await()
        val fdResult = fdJob.await()

        if (startersResult.isFailure && hrResult.isFailure && fdResult.isFailure) {
            val cause = startersResult.exceptionOrNull()
                ?: hrResult.exceptionOrNull()
                ?: fdResult.exceptionOrNull()
            when (cause) {
                is SlateLoadException -> throw cause
                is StartersLoadException -> throw cause
                else -> throw SlateLoadException("Couldn’t load today’s picks for $date.", cause)
            }
        }

        val startersBoard = startersResult.getOrNull()
        val hrBoard = hrResult.getOrNull()
        val fdBoard = fdResult.getOrNull()

        val fetchedAt = listOfNotNull(
            startersBoard?.fetchedAt,
            hrBoard?.slate?.fetchedAt,
            fdBoard?.fetchedAt,
        ).maxOrNull() ?: Instant.now()

        val kSpots = startersBoard?.let { TopPicksSelector.kSpots(it.starters) }.orEmpty()
        val hrSpots = hrBoard?.let { TopPicksSelector.hrSpots(it.batters) }.orEmpty()
        val (fdValue, byValue) = fdBoard?.let { TopPicksSelector.fdValue(it.rows) } ?: (emptyList<TopPick>() to false)

        val source = listOfNotNull(
            startersBoard?.sourceLabel,
            hrBoard?.slate?.sourceLabel,
            fdBoard?.sourceLabel,
        ).firstOrNull().orEmpty().ifBlank { "Live slate" }

        TopPicksBoard(
            slateDate = date,
            fetchedAt = fetchedAt,
            sourceLabel = source,
            kSpots = kSpots,
            hrSpots = hrSpots,
            fdValue = fdValue,
            fdRankedByValue = byValue,
            fdSalaryNote = fdBoard?.salaryNote.orEmpty(),
            kNote = when {
                startersResult.isFailure -> startersResult.exceptionOrNull()?.message
                    ?: "Couldn’t load projected starters."
                kSpots.isEmpty() -> startersBoard?.emptyReason ?: "No probable starters posted."
                else -> null
            },
            hrNote = when {
                hrResult.isFailure -> hrResult.exceptionOrNull()?.message
                    ?: "Couldn’t load HR probability."
                hrSpots.isEmpty() -> hrBoard?.slate?.emptyReason ?: "No hitter HR spots posted."
                else -> null
            },
            fdNote = when {
                fdResult.isFailure -> fdResult.exceptionOrNull()?.message
                    ?: "Couldn’t load FanDuel projections."
                fdValue.isEmpty() -> fdBoard?.emptyReason ?: "No FD projections for this slate."
                else -> null
            },
            emptyReason = if (kSpots.isEmpty() && hrSpots.isEmpty() && fdValue.isEmpty()) {
                "No live picks for $date. MLB may not have posted the slate yet."
            } else {
                null
            },
        )
    }
}
