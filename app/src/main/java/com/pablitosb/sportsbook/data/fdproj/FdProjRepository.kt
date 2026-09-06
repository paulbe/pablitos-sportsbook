package com.pablitosb.sportsbook.data.fdproj

import com.pablitosb.sportsbook.data.dfs.DfsRepository
import java.util.Locale

class FdProjRepository(
    private val dfs: DfsRepository = DfsRepository(),
) {
    suspend fun load(
        date: java.time.LocalDate,
        importedText: String?,
        exampleFileText: String?,
        selectedSlateId: String,
        force: Boolean = false,
    ): FdProjBoard {
        val snap = dfs.loadPool(date, importedText, exampleFileText, selectedSlateId, force)
        val games = snap.slate.games.associateBy { it.gamePk }
        val hitters = snap.slate.hitters.associateBy { it.mlbId }
        val pitchers = snap.slate.pitchers.associateBy { it.mlbId }
        val rows = snap.pool.map { player ->
            val hitter = hitters[player.mlbId]
            val pitcher = pitchers[player.mlbId]
            val game = games[player.gamePk]
            val opponent = hitter?.opponent
                ?: pitcher?.opponent
                ?: game?.let { if (player.team == it.homeAbbr) it.awayAbbr else it.homeAbbr }
                ?: ""
            val awayAbbr = hitter?.awayAbbr?.ifBlank { null }
                ?: pitcher?.awayAbbr?.ifBlank { null }
                ?: game?.awayAbbr.orEmpty()
            val homeAbbr = hitter?.homeAbbr?.ifBlank { null }
                ?: pitcher?.homeAbbr?.ifBlank { null }
                ?: game?.homeAbbr.orEmpty()
            val homeAway = hitter?.homeAway?.ifBlank { null }
                ?: pitcher?.homeAway?.ifBlank { null }
                ?: game?.let { if (player.team == it.homeAbbr) "home" else "away" }
                ?: ""
            val driver = when {
                player.isPitcher && pitcher != null ->
                    String.format(Locale.US, "%.1f Proj Ks", pitcher.nextStartKs)
                hitter != null ->
                    String.format(
                        Locale.US,
                        "%.0f%% HR · %.1f TB",
                        hitter.gameHrProb * 100f,
                        hitter.expectedTb,
                    )
                else -> ""
            }
            FdProjRow(
                mlbId = player.mlbId,
                name = player.name,
                team = player.team,
                opponent = opponent,
                pos = player.pos,
                salary = player.salary,
                proj = player.proj,
                ceiling = player.ceiling,
                value = FdProjSorter.value(player.proj, player.salary),
                isPitcher = player.isPitcher,
                inPostedLineup = player.inPostedLineup,
                gameTimeLabel = game?.gameTimeLabel.orEmpty(),
                driver = driver,
                projTb = hitter?.expectedTb,
                awayAbbr = awayAbbr,
                homeAbbr = homeAbbr,
                homeAway = homeAway,
            )
        }
        return FdProjBoard(
            slateDate = snap.slate.slateDate,
            fetchedAt = snap.slate.fetchedAt,
            rows = rows,
            slates = snap.slates,
            selectedSlateId = snap.selectedSlateId,
            salarySource = snap.salarySource,
            salaryNote = snap.salaryNote,
            fdApiNote = snap.fdApiNote,
            sourceLabel = snap.slate.sourceLabel,
            emptyReason = if (rows.isEmpty()) snap.salaryNote else null,
        )
    }
}
