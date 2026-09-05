package com.pablitosb.sportsbook.data.props

import com.pablitosb.sportsbook.data.mlb.StatMath
import com.pablitosb.sportsbook.data.model.Confidence
import com.pablitosb.sportsbook.data.model.PropLineSource
import com.pablitosb.sportsbook.data.model.UnderdogProp
import com.pablitosb.sportsbook.data.projections.ProjectionBoard
import com.pablitosb.sportsbook.data.projections.ProjectionService
import com.pablitosb.sportsbook.data.projections.SlateLoadException
import java.time.LocalDate
import java.util.Locale

data class ParsedPropLine(
    val player: String,
    val market: String,
    val line: Double,
    val side: String,
    val odds: Int,
)

data class PropsBoard(
    val slate: ProjectionBoard,
    val props: List<UnderdogProp>,
    val importedCount: Int,
    val sourceLabel: String,
)

class PropsRepository(
    private val projections: ProjectionService = ProjectionService.shared,
) {
    suspend fun load(
        date: LocalDate,
        imported: List<ParsedPropLine> = emptyList(),
        force: Boolean = false,
    ): PropsBoard {
        val board = try {
            projections.load(date, force)
        } catch (e: SlateLoadException) {
            throw e
        } catch (e: Exception) {
            throw SlateLoadException("Couldn’t build the props board for $date.", e)
        }
        val model = buildModelBoard(board)
        val merged = if (imported.isEmpty()) {
            model
        } else {
            applyImports(model, imported)
        }
        val ranked = rank(merged)
        val importedCount = ranked.count { it.source == PropLineSource.IMPORTED }
        return PropsBoard(
            slate = board,
            props = ranked,
            importedCount = importedCount,
            sourceLabel = if (importedCount > 0) "Imported lines + model" else "Model board",
        )
    }

    private fun buildModelBoard(board: ProjectionBoard): List<UnderdogProp> {
        val ks = board.pitchers.map { p ->
            val line = StatMath.nearestHalf(p.nextStartKs)
            val over = StatMath.poissonOver(line.toDouble(), p.nextStartKs.toDouble())
            val higher = over >= 0.5f
            UnderdogProp(
                rank = 0,
                player = p.name,
                team = p.team,
                propLabel = if (higher) "Ks Higher" else "Ks Lower",
                line = formatLine(line),
                odds = null,
                modelProb = if (higher) over * 100f else (1f - over) * 100f,
                impliedProb = null,
                confidence = confidence(p.seasonBf, kotlin.math.abs(over - 0.5f)),
                source = PropLineSource.MODEL_BOARD,
                market = "pitcher_ks",
            )
        }
        val hr = board.hitters
            .sortedByDescending { it.gameHrProb }
            .take(40)
            .map { h ->
                val p = h.gameHrProb
                UnderdogProp(
                    rank = 0,
                    player = h.name,
                    team = h.team,
                    propLabel = "HR Higher",
                    line = "0.5",
                    odds = null,
                    modelProb = p * 100f,
                    impliedProb = null,
                    confidence = confidence(h.seasonPa, p),
                    source = PropLineSource.MODEL_BOARD,
                    market = "batter_hr",
                )
            }
        val hits = board.hitters
            .filter { it.inPostedLineup || it.seasonPa >= 80 }
            .sortedByDescending { it.expectedHits }
            .take(24)
            .map { h ->
                val line = if (h.expectedHits >= 1.20f) 1.5 else 0.5
                val over = StatMath.poissonOver(line, h.expectedHits.toDouble())
                val higher = over >= 0.5f
                UnderdogProp(
                    rank = 0,
                    player = h.name,
                    team = h.team,
                    propLabel = if (higher) "Hits Higher" else "Hits Lower",
                    line = formatLine(line.toFloat()),
                    odds = null,
                    modelProb = if (higher) over * 100f else (1f - over) * 100f,
                    impliedProb = null,
                    confidence = confidence(h.seasonPa, kotlin.math.abs(over - 0.5f)),
                    source = PropLineSource.MODEL_BOARD,
                    market = "batter_hits",
                )
            }
        return ks + hr + hits
    }

    private fun applyImports(model: List<UnderdogProp>, imported: List<ParsedPropLine>): List<UnderdogProp> {
        return model.map { prop ->
            val match = imported.firstOrNull { row ->
                namesMatch(row.player, prop.player) && marketMatch(row.market, prop.market)
            } ?: return@map prop
            val line = match.line.toFloat()
            val sideHigher = match.side.contains("high", true) || match.side == "o" || match.side == "over"
            val modelProb = if (prop.market == "batter_hr") {
                if (sideHigher) prop.modelProb else 100f - prop.modelProb
            } else {
                val overPct = if (prop.propLabel.contains("Higher")) prop.modelProb else 100f - prop.modelProb
                if (sideHigher) overPct else 100f - overPct
            }
            val implied = StatMath.impliedFromAmerican(match.odds) * 100f
            prop.copy(
                propLabel = when (prop.market) {
                    "pitcher_ks" -> if (sideHigher) "Ks Higher" else "Ks Lower"
                    "batter_hr" -> if (sideHigher) "HR Higher" else "HR Lower"
                    else -> if (sideHigher) "Hits Higher" else "Hits Lower"
                },
                line = formatLine(line),
                odds = match.odds,
                modelProb = modelProb,
                impliedProb = implied,
                source = PropLineSource.IMPORTED,
            )
        }
    }

    private fun rank(props: List<UnderdogProp>): List<UnderdogProp> {
        val sorted = props.sortedWith(
            compareByDescending<UnderdogProp> { it.edgePct ?: -999f }
                .thenByDescending { it.modelProb },
        )
        return sorted.mapIndexed { i, p -> p.copy(rank = i + 1) }
    }

    private fun confidence(sample: Int, lean: Float): Confidence {
        return when {
            sample >= 200 && lean >= 0.10f -> Confidence.VERY_HIGH
            sample >= 120 && lean >= 0.06f -> Confidence.HIGH
            sample >= 60 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }
    }

    private fun namesMatch(a: String, b: String): Boolean {
        val na = norm(a)
        val nb = norm(b)
        return na == nb || na.contains(nb) || nb.contains(na)
    }

    private fun marketMatch(raw: String, market: String): Boolean {
        val m = raw.lowercase(Locale.US).replace(" ", "_")
        return when (market) {
            "pitcher_ks" -> m.contains("k") || m.contains("strike")
            "batter_hr" -> m.contains("hr") || m.contains("homer") || m.contains("home")
            "batter_hits" -> m.contains("hit")
            else -> false
        }
    }

    private fun norm(name: String): String =
        name.lowercase(Locale.US).replace(Regex("[^a-z0-9 ]"), "").trim()

    private fun formatLine(line: Float): String =
        if (line == line.toInt().toFloat()) line.toInt().toString()
        else String.format(Locale.US, "%.1f", line)

    companion object {
        fun parseImport(text: String): List<ParsedPropLine> {
            val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
            if (lines.isEmpty()) return emptyList()
            val start = if (lines.first().lowercase(Locale.US).startsWith("player") ||
                lines.first().lowercase(Locale.US).startsWith("name")
            ) 1 else 0
            return lines.drop(start).mapNotNull { line ->
                val cols = if (line.contains('\t')) line.split('\t') else line.split(',')
                if (cols.size < 5) return@mapNotNull null
                val player = cols[0].trim().ifBlank { return@mapNotNull null }
                val market = cols[1].trim()
                val lineVal = cols[2].trim().toDoubleOrNull() ?: return@mapNotNull null
                val side = cols[3].trim()
                val odds = cols[4].trim().replace("+", "").toIntOrNull() ?: return@mapNotNull null
                ParsedPropLine(player, market, lineVal, side, odds)
            }
        }
    }
}
