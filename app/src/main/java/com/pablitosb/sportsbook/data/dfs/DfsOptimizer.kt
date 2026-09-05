package com.pablitosb.sportsbook.data.dfs

import com.pablitosb.sportsbook.data.model.ContestType
import com.pablitosb.sportsbook.data.model.DfsLineup
import com.pablitosb.sportsbook.data.model.DfsPlayer
import com.pablitosb.sportsbook.data.model.LineupKind
import kotlin.math.roundToInt
import kotlin.random.Random

object DfsOptimizer {
    data class Result(
        val lineups: List<DfsLineup>,
        val error: String? = null,
    )

    fun build(
        pool: List<SlatePlayer>,
        contest: ContestType,
        stackSize: Int,
        ownLever: Int,
        seed: Long,
    ): Result {
        val usable = pool.filter { it.salary in 2000..15_000 && it.proj > 0f }
        if (usable.none { it.isPitcher } || usable.count { !it.isPitcher } < 8) {
            return Result(emptyList(), "Need at least 1 pitcher and 8 hitters on the loaded slate.")
        }
        val rand = Random(seed)
        val teams = usable.filter { !it.isPitcher }.groupBy { it.team }
            .mapValues { (_, v) -> v.sumOf { it.proj.toDouble() } }
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
        val stackA = teams.getOrNull(0)
        val stackB = teams.getOrNull(1)
        val kinds: List<Pair<LineupKind, String?>> = listOf(
            LineupKind.CASH_CORE to null,
            LineupKind.STACK_A to stackA,
            LineupKind.STACK_B to stackB,
            LineupKind.LEVERAGE to stackA,
            LineupKind.CONTRARIAN to (teams.getOrNull(2) ?: stackB),
        )
        val built = mutableListOf<DfsLineup>()
        val usedIds = mutableSetOf<Int>()
        kinds.forEachIndexed { index, (kind, stackTeam) ->
            val lineup = fillLineup(
                pool = usable,
                contest = contest,
                kind = kind,
                stackTeam = stackTeam,
                stackSize = stackSize.coerceIn(2, 5),
                ownLever = ownLever.coerceIn(1, 5),
                avoid = if (kind == LineupKind.CONTRARIAN) usedIds else emptySet(),
                fadeChalkPitcher = kind == LineupKind.LEVERAGE || kind == LineupKind.CONTRARIAN,
                rand = Random(seed + index * 17L + rand.nextInt(0, 999)),
                index = index + 1,
            )
            if (lineup != null) {
                built += lineup
                usedIds += lineup.players.map { it.mlbId }
            }
        }
        if (built.isEmpty()) {
            return Result(emptyList(), "Couldn’t fill a legal $35k FanDuel classic lineup from this slate.")
        }
        return Result(built)
    }

    private fun fillLineup(
        pool: List<SlatePlayer>,
        contest: ContestType,
        kind: LineupKind,
        stackTeam: String?,
        stackSize: Int,
        ownLever: Int,
        avoid: Set<Int>,
        fadeChalkPitcher: Boolean,
        rand: Random,
        index: Int,
    ): DfsLineup? {
        val chalkPenalty = when {
            contest == ContestType.CASH -> 0.05f
            ownLever <= 2 -> 0.35f
            ownLever == 3 -> 0.18f
            else -> 0.02f
        }
        fun score(p: SlatePlayer): Float {
            val value = p.proj / (p.salary / 1000f).coerceAtLeast(2f)
            val ceil = if (contest == ContestType.GPP) p.ceiling * 0.08f else 0f
            val chalk = p.salary / 1000f * chalkPenalty
            val avoidPen = if (p.mlbId in avoid) 4f else 0f
            val startBonus = if (p.inPostedLineup) 0.4f else 0f
            val jitter = rand.nextFloat() * 0.35f
            return value + ceil + startBonus + jitter - chalk - avoidPen
        }

        val picked = linkedMapOf<String, SlatePlayer>()
        val used = mutableSetOf<Int>()

        val pitchers = pool.filter { it.isPitcher }.sortedByDescending { score(it) }
        val pPick = if (fadeChalkPitcher && pitchers.size >= 3) {
            pitchers.drop(2).maxByOrNull { score(it) }
        } else {
            pitchers.firstOrNull()
        } ?: return null
        picked["P"] = pPick
        used += pPick.mlbId

        if (stackTeam != null && kind != LineupKind.CASH_CORE) {
            val stackers = pool.filter { !it.isPitcher && it.team == stackTeam && it.mlbId !in used }
                .sortedByDescending { score(it) }
            val slotsLeft = mutableListOf("C/1B", "2B", "3B", "SS", "OF", "OF", "OF", "UTIL")
            var placed = 0
            for (player in stackers) {
                if (placed >= stackSize) break
                val slot = slotsLeft.firstOrNull { player.fdSlots.contains(it) } ?: continue
                picked[uniqueKey(slot, picked)] = player
                slotsLeft.remove(slot)
                used += player.mlbId
                placed++
            }
        }

        val need = mutableListOf<String>()
        if (picked.keys.none { it == "C/1B" }) need += "C/1B"
        if (picked.keys.none { it == "2B" }) need += "2B"
        if (picked.keys.none { it == "3B" }) need += "3B"
        if (picked.keys.none { it == "SS" }) need += "SS"
        val ofHave = picked.keys.count { it.startsWith("OF") }
        repeat((3 - ofHave).coerceAtLeast(0)) { need += "OF" }
        if (picked.keys.none { it == "UTIL" || it.startsWith("UTIL") }) need += "UTIL"

        for (slot in need) {
            val capLeft = FdScoring.SALARY_CAP - picked.values.sumOf { it.salary }
            val slotsAfter = need.size - need.indexOf(slot) - 1
            val minRemain = slotsAfter * 2000
            val candidates = pool.filter { player ->
                !player.isPitcher &&
                    player.mlbId !in used &&
                    player.fdSlots.contains(slot) &&
                    player.salary <= (capLeft - minRemain)
            }.sortedByDescending { score(it) }
            val choice = candidates.firstOrNull() ?: pool.filter {
                !it.isPitcher && it.mlbId !in used && it.fdSlots.contains(slot)
            }.minByOrNull { it.salary } ?: return null
            picked[uniqueKey(slot, picked)] = choice
            used += choice.mlbId
        }

        if (picked.size != 9) return null
        val salary = picked.values.sumOf { it.salary }
        if (salary > FdScoring.SALARY_CAP) return null

        val players = FdScoring.SLOTS.map { slot ->
            val player = when (slot) {
                "OF" -> picked.entries.first { it.key.startsWith("OF") }.also { picked.remove(it.key) }.value
                else -> picked.remove(slot) ?: picked.entries.first { it.key.startsWith(slot) }.also { picked.remove(it.key) }.value
            }
            DfsPlayer(slot, player.name, player.salary, player.proj, player.team, player.mlbId)
        }
        val proj = players.sumOf { it.proj.toDouble() }.toFloat()
        val stackNote = players.filter { it.pos != "P" }.groupingBy { it.team }.eachCount()
            .maxByOrNull { it.value }?.let { "${it.key} ${it.value}" } ?: "Balanced"
        val title = when (kind) {
            LineupKind.CASH_CORE -> "Lineup $index • ${contestLabel(contest)} • Cash core"
            LineupKind.STACK_A -> "Lineup $index • ${contestLabel(contest)} • $stackNote stack"
            LineupKind.STACK_B -> "Lineup $index • ${contestLabel(contest)} • $stackNote stack"
            LineupKind.LEVERAGE -> "Lineup $index • ${contestLabel(contest)} • Leverage"
            LineupKind.CONTRARIAN -> "Lineup $index • ${contestLabel(contest)} • Contrarian"
        }
        val own = when (kind) {
            LineupKind.CASH_CORE -> 22
            LineupKind.STACK_A -> 16
            LineupKind.STACK_B -> 14
            LineupKind.LEVERAGE -> 9
            LineupKind.CONTRARIAN -> 6
        }
        return DfsLineup(
            index = index,
            kind = kind,
            title = title,
            contest = contestLabel(contest),
            stackNote = stackNote,
            salary = salary,
            salaryCap = FdScoring.SALARY_CAP,
            proj = proj,
            ceiling = (proj * 1.28f).roundToInt(),
            avgOwnPct = own,
            players = players,
        )
    }

    private fun uniqueKey(slot: String, picked: Map<String, SlatePlayer>): String {
        if (!picked.containsKey(slot)) return slot
        var i = 2
        while (picked.containsKey("$slot#$i")) i++
        return "$slot#$i"
    }

    private fun contestLabel(contest: ContestType): String =
        if (contest == ContestType.CASH) "Cash" else "GPP"
}
