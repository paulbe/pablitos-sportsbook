package com.pablitosb.sportsbook.data.dfs

import com.pablitosb.sportsbook.data.projections.HitterProjection
import com.pablitosb.sportsbook.data.projections.PitcherProjection
import java.util.Locale

enum class SalarySource { EXAMPLE_FORMULA, EXAMPLE_FILE, IMPORTED }

data class SlatePlayer(
    val mlbId: Int,
    val name: String,
    val team: String,
    val pos: String,
    val salary: Int,
    val proj: Float,
    val ceiling: Float,
    val fdSlots: Set<String>,
    val isPitcher: Boolean,
    val inPostedLineup: Boolean,
    val gamePk: Int,
)

data class ParsedSalaryRow(
    val name: String,
    val team: String,
    val pos: String,
    val salary: Int,
    val proj: Float?,
    val mlbId: Int?,
)

object SalarySlate {
    fun exampleFormula(
        hitters: List<HitterProjection>,
        pitchers: List<PitcherProjection>,
    ): List<SlatePlayer> {
        val hSorted = hitters.sortedByDescending { it.fdPoints }
        val pSorted = pitchers.sortedByDescending { it.fdPoints }
        val fromHit = hSorted.mapIndexed { index, h ->
            SlatePlayer(
                mlbId = h.mlbId,
                name = h.name,
                team = h.team,
                pos = FdScoring.primaryPos(h.pos),
                salary = FdScoring.exampleSalary(false, h.fdPoints, index, hSorted.size),
                proj = h.fdPoints,
                ceiling = h.fdPoints * (1.25f + h.gameHrProb),
                fdSlots = h.fdSlots,
                isPitcher = false,
                inPostedLineup = h.inPostedLineup,
                gamePk = h.gamePk,
            )
        }
        val fromPit = pSorted.mapIndexed { index, p ->
            SlatePlayer(
                mlbId = p.mlbId,
                name = p.name,
                team = p.team,
                pos = "P",
                salary = FdScoring.exampleSalary(true, p.fdPoints, index, pSorted.size),
                proj = p.fdPoints,
                ceiling = p.fdPoints * 1.35f,
                fdSlots = setOf("P"),
                isPitcher = true,
                inPostedLineup = true,
                gamePk = p.gamePk,
            )
        }
        return fromPit + fromHit
    }

    fun mergeImported(
        rows: List<ParsedSalaryRow>,
        hitters: List<HitterProjection>,
        pitchers: List<PitcherProjection>,
    ): List<SlatePlayer> {
        val hById = hitters.associateBy { it.mlbId }
        val pById = pitchers.associateBy { it.mlbId }
        val hByName = hitters.associateBy { norm(it.name) }
        val pByName = pitchers.associateBy { norm(it.name) }
        return rows.mapNotNull { row ->
            if (row.salary <= 0) return@mapNotNull null
            val h = row.mlbId?.let { hById[it] } ?: hByName[norm(row.name)]
            val p = row.mlbId?.let { pById[it] } ?: pByName[norm(row.name)]
            when {
                p != null || row.pos.equals("P", true) -> {
                    val proj = row.proj ?: p?.fdPoints ?: 18f
                    SlatePlayer(
                        mlbId = p?.mlbId ?: row.mlbId ?: row.name.hashCode(),
                        name = p?.name ?: row.name,
                        team = p?.team ?: row.team,
                        pos = "P",
                        salary = row.salary,
                        proj = proj,
                        ceiling = proj * 1.35f,
                        fdSlots = setOf("P"),
                        isPitcher = true,
                        inPostedLineup = true,
                        gamePk = p?.gamePk ?: 0,
                    )
                }
                else -> {
                    val proj = row.proj ?: h?.fdPoints ?: 9f
                    val pos = row.pos.ifBlank { h?.pos ?: "UTIL" }
                    SlatePlayer(
                        mlbId = h?.mlbId ?: row.mlbId ?: row.name.hashCode(),
                        name = h?.name ?: row.name,
                        team = h?.team ?: row.team,
                        pos = FdScoring.primaryPos(pos),
                        salary = row.salary,
                        proj = proj,
                        ceiling = proj * 1.30f,
                        fdSlots = FdScoring.fdSlotsFor(pos),
                        isPitcher = false,
                        inPostedLineup = h?.inPostedLineup ?: false,
                        gamePk = h?.gamePk ?: 0,
                    )
                }
            }
        }
    }

    fun parse(text: String): List<ParsedSalaryRow> {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toList()
        if (lines.isEmpty()) return emptyList()
        val start = if (looksLikeHeader(lines.first())) 1 else 0
        return lines.drop(start).mapNotNull { line ->
            runCatching {
                val cols = splitRow(line)
                if (cols.size < 4) return@mapNotNull null
                val name = cols[0].trim().ifBlank { return@mapNotNull null }
                val team = cols.getOrNull(1).orEmpty().trim()
                val pos = cols.getOrNull(2).orEmpty().trim()
                val salary = cols.getOrNull(3)?.replace("$", "")?.replace(",", "")?.toIntOrNull()
                    ?: return@mapNotNull null
                val proj = cols.getOrNull(4)?.toFloatOrNull()
                val mlbId = cols.getOrNull(5)?.toIntOrNull()
                ParsedSalaryRow(name, team, pos, salary, proj, mlbId)
            }.getOrNull()
        }
    }

    private fun looksLikeHeader(line: String): Boolean {
        val first = splitRow(line).firstOrNull()?.lowercase(Locale.US) ?: return false
        return first == "name" || first == "player" || first == "nickname"
    }

    private fun splitRow(line: String): List<String> {
        return if (line.contains('\t')) line.split('\t') else line.split(',')
    }

    fun norm(name: String): String =
        name.lowercase(Locale.US).replace(Regex("[^a-z0-9 ]"), "").replace(Regex("\\s+"), " ").trim()
}
