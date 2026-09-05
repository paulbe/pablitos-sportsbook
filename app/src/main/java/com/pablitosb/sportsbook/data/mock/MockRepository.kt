package com.pablitosb.sportsbook.data.mock

import com.pablitosb.sportsbook.data.model.Confidence
import com.pablitosb.sportsbook.data.model.DfsLineup
import com.pablitosb.sportsbook.data.model.DfsPlayer
import com.pablitosb.sportsbook.data.model.HrBatter
import com.pablitosb.sportsbook.data.model.LineupKind
import com.pablitosb.sportsbook.data.model.Outlook
import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.model.UnderdogProp
import com.pablitosb.sportsbook.data.model.Weather

object MockRepository {

    val slateLabel: String = "MLB • Main slate"

    val starters: List<Starter> = listOf(
        Starter(1, "Gerrit Cole", "NYY", "BOS", "Yankee Stadium", Weather.SUN, 75, Outlook.PROG, 12, 29.8f, 7.2f, listOf(5.2f, 5.8f, 6.1f, 6.8f, 7.0f, 7.2f)),
        Starter(2, "Tarik Skubal", "DET", "CLE", "Comerica Park", Weather.SUN, 72, Outlook.PROG, 9, 31.4f, 8.1f, listOf(6.4f, 6.8f, 7.2f, 7.6f, 7.9f, 8.1f)),
        Starter(3, "Zack Wheeler", "PHI", "ATL", "Citizens Bank", Weather.CLOUD, 78, Outlook.PROG, 6, 28.6f, 6.9f, listOf(5.8f, 6.0f, 6.2f, 6.5f, 6.7f, 6.9f)),
        Starter(4, "Yoshinobu Yamamoto", "LAD", "SF", "Dodger Stadium", Weather.SUN, 81, Outlook.STABLE, 3, 27.1f, 6.4f, listOf(6.0f, 6.2f, 6.1f, 6.3f, 6.4f, 6.4f)),
        Starter(5, "Corbin Burnes", "BAL", "TB", "Camden Yards", Weather.CLOUD, 74, Outlook.STABLE, 1, 25.4f, 6.0f, listOf(6.2f, 6.0f, 5.9f, 6.1f, 6.0f, 6.0f)),
        Starter(6, "Logan Webb", "SF", "LAD", "Oracle Park", Weather.CLOUD, 64, Outlook.STABLE, -2, 22.8f, 5.4f, listOf(5.8f, 5.6f, 5.5f, 5.4f, 5.4f, 5.4f)),
        Starter(7, "Pablo López", "MIN", "CWS", "Target Field", Weather.SUN, 70, Outlook.REG, -6, 23.2f, 5.1f, listOf(6.4f, 6.0f, 5.7f, 5.4f, 5.2f, 5.1f)),
        Starter(8, "José Berríos", "TOR", "BAL", "Rogers Centre", Weather.CLOUD, 68, Outlook.REG, -11, 20.1f, 4.6f, listOf(6.2f, 5.8f, 5.4f, 5.0f, 4.8f, 4.6f)),
    )

    val hrBatters: List<HrBatter> = listOf(
        HrBatter(1, "Aaron Judge", "NYY", "BOS", "RHP", 18.4f, 8.2f, 22, 16, "Yankee Stadium", Weather.SUN, 75, 10, "Pivetta", 1.45f),
        HrBatter(2, "Shohei Ohtani", "LAD", "SF", "RHP", 16.1f, 9.1f, 20, 8, "Dodger Stadium", Weather.SUN, 81, 6, "Webb", 0.92f),
        HrBatter(3, "Kyle Schwarber", "PHI", "ATL", "RHP", 14.8f, 7.6f, 19, 14, "Citizens Bank", Weather.CLOUD, 78, 8, "Sale", 1.21f),
        HrBatter(4, "Yordan Alvarez", "HOU", "TEX", "RHP", 13.2f, 7.4f, 18, 4, "Minute Maid", Weather.SUN, 88, 7, "Eovaldi", 1.08f),
        HrBatter(5, "Matt Olson", "ATL", "PHI", "RHP", 9.4f, 5.1f, 14, 11, "Truist Park", Weather.CLOUD, 80, 3, "Wheeler", 0.88f),
        HrBatter(6, "Brent Rooker", "OAK", "SEA", "LHP", 6.3f, 3.6f, 11, -4, "Oakland Coliseum", Weather.CLOUD, 66, -10, "Castillo", 0.74f, regressionLean = true),
        HrBatter(7, "Juan Soto", "NYY", "BOS", "RHP", 9.8f, 5.4f, 15, 16, "Yankee Stadium", Weather.SUN, 75, 10, "Pivetta", 1.45f),
        HrBatter(8, "Gunnar Henderson", "BAL", "TB", "RHP", 8.1f, 4.7f, 13, 9, "Camden Yards", Weather.CLOUD, 74, 5, "McClanahan", 1.12f),
    )

    val lineups: List<DfsLineup> = listOf(
        DfsLineup(
            index = 1,
            kind = LineupKind.CASH_CORE,
            title = "Lineup 1 • Cash • Cash core",
            contest = "Cash",
            stackNote = "Balanced core",
            salary = 34_900,
            proj = 136.4f,
            ceiling = 164,
            avgOwnPct = 24,
            players = listOf(
                DfsPlayer("P", "Skubal", 11_200, 33.8f),
                DfsPlayer("C/1B", "Olson", 3_800, 12.6f),
                DfsPlayer("2B", "Altuve", 3_200, 11.4f),
                DfsPlayer("3B", "Ramírez", 3_600, 12.1f),
                DfsPlayer("SS", "Henderson", 3_400, 12.0f),
                DfsPlayer("OF", "Judge", 4_200, 18.8f),
                DfsPlayer("OF", "Soto", 3_600, 14.2f),
                DfsPlayer("OF", "Tucker", 3_400, 13.1f),
                DfsPlayer("UTIL", "Alvarez", 2_500, 8.4f),
            ),
        ),
        DfsLineup(
            index = 2,
            kind = LineupKind.STACK_A,
            title = "Lineup 2 • GPP • NYY 4-stack",
            contest = "GPP",
            stackNote = "Yankees 4",
            salary = 33_800,
            proj = 148.2f,
            ceiling = 192,
            avgOwnPct = 18,
            players = listOf(
                DfsPlayer("P", "Cole", 10_200, 31.6f),
                DfsPlayer("C/1B", "Judge", 12_100, 36.4f),
                DfsPlayer("2B", "Torres", 2_000, 12.2f),
                DfsPlayer("3B", "Volpe", 1_700, 10.1f),
                DfsPlayer("SS", "Bichette", 1_600, 11.4f),
                DfsPlayer("OF", "Stanton", 1_800, 13.8f),
                DfsPlayer("OF", "Verdugo", 1_600, 11.2f),
                DfsPlayer("OF", "Grisham", 1_400, 10.6f),
                DfsPlayer("UTIL", "Wells", 1_400, 10.9f),
            ),
        ),
        DfsLineup(
            index = 3,
            kind = LineupKind.STACK_B,
            title = "Lineup 3 • GPP • LAD 4-stack",
            contest = "GPP",
            stackNote = "Dodgers 4",
            salary = 34_200,
            proj = 144.6f,
            ceiling = 188,
            avgOwnPct = 16,
            players = listOf(
                DfsPlayer("P", "Yamamoto", 9_800, 28.4f),
                DfsPlayer("C/1B", "Freeman", 3_900, 13.6f),
                DfsPlayer("2B", "Lux", 2_200, 9.8f),
                DfsPlayer("3B", "Muncy", 2_800, 12.4f),
                DfsPlayer("SS", "Betts", 4_100, 16.2f),
                DfsPlayer("OF", "Ohtani", 5_600, 24.8f),
                DfsPlayer("OF", "Hernández", 2_400, 11.6f),
                DfsPlayer("OF", "Pages", 1_800, 10.2f),
                DfsPlayer("UTIL", "Smith", 1_600, 17.6f),
            ),
        ),
        DfsLineup(
            index = 4,
            kind = LineupKind.LEVERAGE,
            title = "Lineup 4 • GPP • Leverage",
            contest = "GPP",
            stackNote = "Low-own smash",
            salary = 33_100,
            proj = 141.0f,
            ceiling = 201,
            avgOwnPct = 9,
            players = listOf(
                DfsPlayer("P", "Wheeler", 10_400, 29.2f),
                DfsPlayer("C/1B", "Schwarber", 3_700, 15.8f),
                DfsPlayer("2B", "Stott", 2_100, 10.4f),
                DfsPlayer("3B", "Bohm", 2_200, 10.6f),
                DfsPlayer("SS", "Turner", 3_300, 12.8f),
                DfsPlayer("OF", "Castellanos", 2_400, 11.9f),
                DfsPlayer("OF", "Rooker", 2_800, 13.4f),
                DfsPlayer("OF", "Langeliers", 2_200, 12.1f),
                DfsPlayer("UTIL", "Butler", 4_000, 24.8f),
            ),
        ),
        DfsLineup(
            index = 5,
            kind = LineupKind.CONTRARIAN,
            title = "Lineup 5 • GPP • Contrarian",
            contest = "GPP",
            stackNote = "Fade chalk",
            salary = 32_600,
            proj = 132.8f,
            ceiling = 196,
            avgOwnPct = 7,
            players = listOf(
                DfsPlayer("P", "López", 8_200, 22.4f),
                DfsPlayer("C/1B", "Naylor", 3_100, 12.2f),
                DfsPlayer("2B", "Giménez", 2_400, 10.1f),
                DfsPlayer("3B", "Ramírez", 3_800, 14.6f),
                DfsPlayer("SS", "Kwan", 2_600, 10.8f),
                DfsPlayer("OF", "Laureano", 2_300, 11.4f),
                DfsPlayer("OF", "Thomas", 2_700, 12.0f),
                DfsPlayer("OF", "Cowser", 2_500, 11.7f),
                DfsPlayer("UTIL", "Henderson", 5_000, 27.6f),
            ),
        ),
    )

    val props: List<UnderdogProp> = listOf(
        UnderdogProp(1, "Gerrit Cole", "NYY", "Ks Higher", "6.5", -115, 61.0f, 53.5f, Confidence.VERY_HIGH),
        UnderdogProp(2, "Tarik Skubal", "DET", "Ks Higher", "7.5", -122, 58.4f, 55.0f, Confidence.HIGH),
        UnderdogProp(3, "Aaron Judge", "NYY", "HR Higher", "0.5", +128, 32.6f, 27.2f, Confidence.HIGH),
        UnderdogProp(4, "Shohei Ohtani", "LAD", "Hits Higher", "1.5", -105, 54.8f, 51.2f, Confidence.MEDIUM),
        UnderdogProp(5, "Kyle Schwarber", "PHI", "HR Higher", "0.5", +142, 28.1f, 24.4f, Confidence.MEDIUM),
        UnderdogProp(6, "Zack Wheeler", "PHI", "Ks Lower", "6.5", +110, 49.2f, 47.6f, Confidence.LOW),
        UnderdogProp(7, "Yordan Alvarez", "HOU", "TB Higher", "1.5", -118, 55.0f, 54.1f, Confidence.LOW),
        UnderdogProp(8, "Juan Soto", "NYY", "Hits Higher", "1.5", +104, 48.6f, 49.0f, Confidence.LOW),
    )

    val plusEvCount: Int get() = props.count { it.edgePct > 0f }
    val avgEdge: Float get() = props.filter { it.edgePct > 0f }.map { it.edgePct }.average().toFloat()
}
