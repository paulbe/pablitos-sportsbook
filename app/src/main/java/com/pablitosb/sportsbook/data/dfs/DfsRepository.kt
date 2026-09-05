package com.pablitosb.sportsbook.data.dfs

import com.pablitosb.sportsbook.data.model.ContestType
import com.pablitosb.sportsbook.data.model.DfsLineup
import com.pablitosb.sportsbook.data.projections.ProjectionBoard
import com.pablitosb.sportsbook.data.projections.ProjectionService
import com.pablitosb.sportsbook.data.projections.SlateLoadException
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DfsBoard(
    val slate: ProjectionBoard,
    val pool: List<SlatePlayer>,
    val lineups: List<DfsLineup>,
    val salarySource: SalarySource,
    val salaryNote: String,
    val optimizeError: String? = null,
    val slates: List<DfsSlateOption> = emptyList(),
    val selectedSlateId: String = "main",
    val fdApiNote: String = "",
)

class DfsRepository(
    private val projections: ProjectionService = ProjectionService.shared,
    private val fdClient: FdSlateClient = FdSlateClient(),
) {
    suspend fun load(
        date: LocalDate,
        contest: ContestType,
        stackSize: Int,
        ownLever: Int,
        seed: Long,
        importedText: String?,
        exampleFileText: String?,
        selectedSlateId: String = "main",
        force: Boolean = false,
    ): DfsBoard {
        val board = try {
            projections.load(date, force)
        } catch (e: SlateLoadException) {
            throw e
        } catch (e: Exception) {
            throw SlateLoadException("Couldn’t build DFS projections for $date.", e)
        }

        val fdResult = withContext(Dispatchers.IO) { runCatching { fdClient.tryListMlbSlates() }.getOrDefault(FdApiResult.AuthRequired) }
        val derived = MlbSlateBuilder.build(board.games)
        val (catalog, fdNote) = when (fdResult) {
            is FdApiResult.Live -> fdResult.slates to "FanDuel fixture-lists loaded."
            is FdApiResult.AuthRequired -> derived to
                "FanDuel api.fanduel.com/fixture-lists returned 401 (login required). Showing MLB-derived Main / Early / Late / Showdown. Salaries are EXAMPLE unless you import a CSV."
            is FdApiResult.Unavailable -> derived to
                "FanDuel slate API unavailable (${fdResult.detail}). Using MLB-derived slates. Salaries are EXAMPLE unless you import."
        }

        val imported = !importedText.isNullOrBlank()
        val slates = if (imported) {
            catalog + DfsSlateOption(
                id = "imported",
                title = "Imported CSV",
                subtitle = "Your pasted salaries · not a live FanDuel pull",
                gamePks = emptySet(),
                kind = DfsSlateKind.IMPORTED,
                origin = DfsSlateOrigin.IMPORTED,
                gameCount = 0,
            )
        } else {
            catalog
        }

        val chosenId = when {
            imported && selectedSlateId == "imported" -> "imported"
            slates.any { it.id == selectedSlateId } -> selectedSlateId
            else -> MlbSlateBuilder.defaultId(slates)
        }
        val chosen = slates.firstOrNull { it.id == chosenId }

        val hitters = if (chosen != null && chosen.kind != DfsSlateKind.IMPORTED && chosen.gamePks.isNotEmpty()) {
            board.hitters.filter { it.gamePk in chosen.gamePks }
        } else {
            board.hitters
        }
        val pitchers = if (chosen != null && chosen.kind != DfsSlateKind.IMPORTED && chosen.gamePks.isNotEmpty()) {
            board.pitchers.filter { it.gamePk in chosen.gamePks }
        } else {
            board.pitchers
        }

        if (hitters.isEmpty() && importedText.isNullOrBlank() && exampleFileText.isNullOrBlank()) {
            return DfsBoard(
                slate = board,
                pool = emptyList(),
                lineups = emptyList(),
                salarySource = SalarySource.EXAMPLE_FORMULA,
                salaryNote = board.emptyReason ?: "No batters on this slate.",
                optimizeError = board.emptyReason,
                slates = slates,
                selectedSlateId = chosenId,
                fdApiNote = fdNote,
            )
        }

        val (pool, source, note) = try {
            when {
                !importedText.isNullOrBlank() && chosenId == "imported" -> {
                    val rows = SalarySlate.parse(importedText)
                    val merged = SalarySlate.mergeImported(rows, board.hitters, board.pitchers)
                    Triple(merged, SalarySource.IMPORTED, "Imported ${merged.size} salaries — not a live FanDuel pull.")
                }
                !exampleFileText.isNullOrBlank() -> {
                    val rows = SalarySlate.parse(exampleFileText)
                    val merged = SalarySlate.mergeImported(rows, hitters, pitchers)
                    Triple(merged, SalarySource.EXAMPLE_FILE, "EXAMPLE file salaries — not live FanDuel prices.")
                }
                else -> Triple(
                    SalarySlate.exampleFormula(hitters, pitchers),
                    SalarySource.EXAMPLE_FORMULA,
                    "EXAMPLE salaries from our projection ranks — not live FanDuel prices.",
                )
            }
        } catch (e: Exception) {
            Triple(
                emptyList(),
                SalarySource.EXAMPLE_FORMULA,
                "Couldn’t parse salaries: ${e.message}",
            )
        }

        val result = runCatching {
            DfsOptimizer.build(pool, contest, stackSize, ownLever, seed)
        }.getOrElse { DfsOptimizer.Result(emptyList(), it.message ?: "Optimizer failed.") }

        return DfsBoard(
            slate = board,
            pool = pool,
            lineups = result.lineups,
            salarySource = source,
            salaryNote = note,
            optimizeError = result.error,
            slates = slates,
            selectedSlateId = chosenId,
            fdApiNote = fdNote,
        )
    }

    fun exportCsv(lineups: List<DfsLineup>): String {
        val out = StringBuilder()
        lineups.forEach { lineup ->
            out.append("# ").append(lineup.title).append('\n')
            out.append("pos,name,team,salary,proj\n")
            lineup.players.forEach { p ->
                out.append(p.pos).append(',')
                    .append(p.name).append(',')
                    .append(p.team).append(',')
                    .append(p.salary).append(',')
                    .append("%.1f".format(Locale.US, p.proj)).append('\n')
            }
            out.append('\n')
        }
        return out.toString()
    }

    fun copyLineup(lineup: DfsLineup): String {
        return buildString {
            appendLine(lineup.title)
            appendLine("Salary $${lineup.salary} / $${lineup.salaryCap}  proj ${"%.1f".format(Locale.US, lineup.proj)}")
            lineup.players.forEach { p ->
                appendLine("${p.pos}\t${p.name}\t$${p.salary}\t${"%.1f".format(Locale.US, p.proj)}")
            }
        }
    }
}
