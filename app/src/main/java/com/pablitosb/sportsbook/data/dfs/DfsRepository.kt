package com.pablitosb.sportsbook.data.dfs

import com.pablitosb.sportsbook.data.model.ContestType
import com.pablitosb.sportsbook.data.model.DfsLineup
import com.pablitosb.sportsbook.data.projections.ProjectionBoard
import com.pablitosb.sportsbook.data.projections.ProjectionService
import com.pablitosb.sportsbook.data.projections.SlateLoadException
import java.time.LocalDate

data class DfsBoard(
    val slate: ProjectionBoard,
    val pool: List<SlatePlayer>,
    val lineups: List<DfsLineup>,
    val salarySource: SalarySource,
    val salaryNote: String,
    val optimizeError: String? = null,
)

class DfsRepository(
    private val projections: ProjectionService = ProjectionService.shared,
) {
    suspend fun load(
        date: LocalDate,
        contest: ContestType,
        stackSize: Int,
        ownLever: Int,
        seed: Long,
        importedText: String?,
        exampleFileText: String?,
        force: Boolean = false,
    ): DfsBoard {
        val board = try {
            projections.load(date, force)
        } catch (e: SlateLoadException) {
            throw e
        } catch (e: Exception) {
            throw SlateLoadException("Couldn’t build DFS projections for $date.", e)
        }
        if (board.hitters.isEmpty() && importedText.isNullOrBlank() && exampleFileText.isNullOrBlank()) {
            return DfsBoard(
                slate = board,
                pool = emptyList(),
                lineups = emptyList(),
                salarySource = SalarySource.EXAMPLE_FORMULA,
                salaryNote = board.emptyReason ?: "No batters on this slate.",
                optimizeError = board.emptyReason,
            )
        }
        val (pool, source, note) = when {
            !importedText.isNullOrBlank() -> {
                val rows = SalarySlate.parse(importedText)
                val merged = SalarySlate.mergeImported(rows, board.hitters, board.pitchers)
                Triple(
                    merged,
                    SalarySource.IMPORTED,
                    "Imported ${merged.size} salaries — not a live FanDuel pull.",
                )
            }
            !exampleFileText.isNullOrBlank() -> {
                val rows = SalarySlate.parse(exampleFileText)
                val merged = SalarySlate.mergeImported(rows, board.hitters, board.pitchers)
                Triple(
                    merged,
                    SalarySource.EXAMPLE_FILE,
                    "EXAMPLE file salaries — not live FanDuel prices.",
                )
            }
            else -> Triple(
                SalarySlate.exampleFormula(board.hitters, board.pitchers),
                SalarySource.EXAMPLE_FORMULA,
                "EXAMPLE salaries from our projection ranks — not live FanDuel prices.",
            )
        }
        val result = DfsOptimizer.build(pool, contest, stackSize, ownLever, seed)
        return DfsBoard(
            slate = board,
            pool = pool,
            lineups = result.lineups,
            salarySource = source,
            salaryNote = note,
            optimizeError = result.error,
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
                    .append("%.1f".format(p.proj)).append('\n')
            }
            out.append('\n')
        }
        return out.toString()
    }

    fun copyLineup(lineup: DfsLineup): String {
        return buildString {
            appendLine(lineup.title)
            appendLine("Salary $${lineup.salary} / $${lineup.salaryCap}  proj ${"%.1f".format(lineup.proj)}")
            lineup.players.forEach { p ->
                appendLine("${p.pos}\t${p.name}\t$${p.salary}\t${"%.1f".format(p.proj)}")
            }
        }
    }
}
