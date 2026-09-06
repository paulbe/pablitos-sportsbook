# Pablito’s Sportsbook

Android app for **today’s MLB slate** — live projected starters, **Daily Batters**
(Game HR% · Proj FD · Proj TB · H+R+RBI), FanDuel classic DFS lineups, a
**FanDuel DFS projections board**, an Underdog-style props board, and
**Today’s Top Picks** (Pitchers | Batters, same filters as the two boards). Dark navy + green.

## Home

**Stable**
1. **Projected Starters** — live MLB probables, filters (Prog · Proj Ks · xwOBA · Proj Outs · Proj FD), Weather boost, AWAY @ HOME
2. **Daily Batters** — live lineups, Game HR% · Proj FD · Proj TB · H+R+RBI, Games filter, Weather boost, AWAY @ HOME
3. **Today’s Top Picks** — Pitchers | Batters digest (same filters as Starters / Daily Batters)
4. **Beta** — submenu for work-in-progress boards

**Beta menu**
- **DFS Lineups** — FanDuel classic $35k optimizer
- **Underdog Props** — model board + import
- **FD DFS Projections** — ranked FanDuel-pts board

Footer: **Models** · **Settings**. DFS / Props / FD projections are no longer primary home tiles.

## Today’s Top Picks

A ranked “best of today” board. Same models as **Projected Starters** and
**Daily Batters** — not a third projection engine. Default date is
**America/Los_Angeles** today. Top **10** of the active filter.

1. **Pitchers | Batters** segment
2. Filters switch with the segment

| Side | Filters | Sort |
| --- | --- | --- |
| **Pitchers** | Prog · Proj Ks · xwOBA · Proj Outs · Proj FD | same as Option 1 (xwOBA lower is better) |
| **Batters** | Game HR% · Proj FD · Proj TB · H+R+RBI | same as Option 2 (high → low) |

Only the active filter’s metric(s) appear in the row (Floor · Proj · Ceiling when
Proj FD is selected). Persistent: name, **AWAY @ HOME · time**, Weather boost %.
PROG/STABLE/REG chip only on the **Prog** filter. No weather icons.

**Coloring**
- Pitchers: own club white; opponent tinted by **team K%** (red low / grey mid / green high).
- Batters: own club white; opponent tinted by **opposing pitcher K% inverted** (green = favorable).

Empty slate → Retry, not mock names.

## Option 5 — FD DFS Projections

A sortable player board (not the optimizer). Same slate picker and live projection
pipeline as DFS Lineups.

| Column | Source |
| --- | --- |
| Player / team / pos | MLB lineup or 26-man roster |
| Opponent + game time | MLB schedule |
| Salary | EXAMPLE rank formula or imported / bundled CSV (`$` tag) |
| **Proj FD pts** | `ProjectionService` → FanDuel scoring |
| Value | pts per $1k when salary is present |
| Ceiling | local upside (`× 1.25 + game HR%` hitters / `× 1.35` pitchers) |
| Own | placeholder `—` (no ownership feed) |
| Driver | pitcher **Proj Ks**, hitter **HR% · Proj TB** |

**Sort:** Proj pts (default) · Value · Salary · Pos. Tap again to reverse.

**Pos filter:** All / P / C / 1B / 2B / 3B / SS / OF / DH.

**Slate:** Main / Early (before 5pm PT) / Late / Showdown. Derived from the MLB
schedule. FanDuel `GET /fixture-lists` still **401** without login — the app stays
honest and does not invent an official FD slate.

Import salaries the same way as Option 3. Tap **Sample CSV** on DFS Lineups or
FD DFS Projections to share a file with this header and five example rows:

```
name,team,pos,salary,proj,mlbId
```

Required: `name`, `team`, `pos`, `salary`. Optional: `proj`, `mlbId`.
FanDuel pos labels: **P, C, 1B, 2B, 3B, SS, OF, DH**.
The bundled EXAMPLE file uses the same schema.

## FanDuel scoring (classic)

| Hitter | Pts | Pitcher | Pts |
| --- | --- | --- | --- |
| 1B | 3 | W | 6 |
| 2B | 6 | QS | 4 |
| 3B | 9 | ER | −3 |
| HR | 12 | SO | 3 |
| RBI | 3.5 | IP | 3 |
| R / BB / SB | 3.2 / 3 / 6 | | |

## Limits

- Salaries are **EXAMPLE** unless you import a CSV. Not live FanDuel prices.
- Ownership is a placeholder.
- Ceiling is a local uplift, not a Statcast-derived ceiling.
- FanDuel fixture-lists require login (401 without cookies).
- Timezone for “today”: **America/Los_Angeles**.
- Proj TB is not Statcast xTB — season rates plus the existing park/pitcher/weather stack.
- There is no standalone Total Bases home tile. Proj TB on Daily Batters and Top Picks share the same expected-TB formula.
- DFS Lineups, Underdog Props, and FD DFS Projections live under **Beta**, not on the main home list.
- Proj Outs is a local IP model (recent/season logs + opponent OPS + Weather boost + early exits), not Statcast or a pitch-count feed.
- Starter opponent K colors use season team SO/PA. Daily Batters tints the opponent by **opposing pitcher K%**, inverted (green = low-K / favorable). Sparse samples fall back to 21.6% / 23.4%. Retractable roofs stay outdoor unless MLB says closed.
- Proj FD W / QS / ER terms (starters) and Floor / Ceiling counting-stat stress (batters) are local heuristics, not FanDuel official projections.
- Daily Batters **Weather boost** uses the MLB schedule weather hydrate plus the park HR factor. It does **not** fetch Open-Meteo on that path (starters do). + is hitter-friendly.

## Option 1 — Projected Starters

Filters (tap again to reverse): **Prog · Proj Ks · xwOBA · Proj Outs · Proj FD**.
Other filters stay single-stat. Persistent on every row: name, **AWAY @ HOME · time**,
PROG/STABLE/REG badge, **Weather boost** %. Raw wind, temp, and rain details stay off the row.

**Opponent color** tints only the opponent abbreviation (pitcher’s club stays white):

| Color | Team K% (SO/PA) |
| --- | --- |
| Red | low — below the 33rd percentile |
| Grey | middle tertile |
| Green | high — above the 67th percentile |

Cuts come from this season’s 30-team distribution when at least 20 clubs are loaded.
Fallback (2024–25 tertile-style): **red < 21.6%**, **green > 23.4%**. A legend is on the screen.

**Proj Outs** = matchup-adjusted IP × 3:

```
baseIp   = shrink(0.55·recent IP/GS + 0.45·season IP/GS, GS, league 5.40, prior 8 GS)
oppMult  = 1 − (opp OPS − 0.711) × 0.40
wxMult   = rain ? 0.86 : 1 − WeatherBoost% × 0.22
workMult = early-exit / heavy-workload haircut
projIp   = clamp(base × opp × wx × work, 3.5 … 7.2)
projOuts = projIp × 3
```

Optional `~IP` subtitle under the outs figure.

**Proj FD** (FanDuel pitcher scoring: SO 3 · IP 3/inning · W 6 · QS 4 · ER −3):

```
Proj FD = 3×ProjKs + ProjOuts + 6×P(W) + 4×P(QS) − 3×E[ER]
```

P(W), P(QS), and E[ER] are matchup heuristics (shrunk season ERA × opponent OPS ×
Weather boost, home-start bump, rain haircut). Floor is a shorter/messier outing;
ceiling is deeper with more Ks and a higher W/QS chance. Sort is by Proj, then ceiling.

## Option 2 — Daily Batters

Same visual language as Projected Starters (no headshots, no Pablito chrome, no
PROG chip, no weather icons). Filters (tap again to reverse):
**Game HR% · Proj FD · Proj TB · H+R+RBI**. A **Games** chip under the tabs
filters the board to selected matchups (default all; date change resets).
Persistent on every row: name, **AWAY @ HOME · time**, **Weather boost** %
(hitter-perspective: + green / − red).

Only the active filter’s metric(s) appear in the middle of the row.

| Filter | What you see | Sort |
| --- | --- | --- |
| **Game HR%** | Talent × park × weather × pitcher HR/9 × platoon | high → low |
| **Proj FD** | Floor · **Proj** (green) · Ceiling | Proj, then ceiling |
| **Proj TB** | Expected total bases | high → low |
| **H+R+RBI** | Projected hits + runs + RBI | high → low |

**Proj FD** (FanDuel hitter scoring: 1B 3 · 2B 6 · 3B 9 · HR 12 · RBI 3.5 · R 3.2 · BB/HBP 3 · SB 6):

```
E[R]   = 0.11·PA + 0.40·HR + 0.15·(BB+HBP)
E[RBI] = 0.10·PA + 0.55·HR
Proj   = 3·1B + 6·2B + 9·3B + 12·HR + 3.5·RBI + 3.2·R + 3·(BB+HBP) + 6·SB
```

HR/PA already includes the game-HR stack. Floor / ceiling stress the same
counting stats (quieter night / bigger night). Not FanDuel’s official projection.

**Opponent color** tints only the opponent abbreviation (batter’s club stays white).
This is **not** away=green / home=red:

| Color | Opposing pitcher K% |
| --- | --- |
| Green | low — favorable for the batter |
| Grey | middle tertile |
| Red | high — tough for the batter |

Cuts come from today’s starter K rates (SO/BF) when at least 20 are loaded.
Fallback: **green < 21.6%**, **red > 23.4%**.

## Option 1 weather

Open-Meteo hourly at park lat/long. MLB `azimuthAngle` for wind vs CF.
Static multi-year **HR park factor** blended into tags + Weather boost.

## Matchups

Every game line is **AWAY @ HOME**. The app does not show `vs` for team matchups.

## Run locally

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Min SDK 26 · target / compile SDK 36.

## Requirements

- Android Studio (or the Android command-line SDK)
- JDK 17
- Device / emulator API 26+
