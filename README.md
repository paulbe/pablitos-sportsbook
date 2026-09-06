# Pablito’s Sportsbook

Android app for **today’s MLB slate** — live projected starters, daily HR probability,
FanDuel classic DFS lineups, a **FanDuel DFS projections board**, an Underdog-style
props board, and **Today’s Top Picks** (SP Ks · HR · TB · FD value). Dark navy + green.

## Home

**Stable**
1. **Projected Starters** — live MLB probables, filters (Prog · Proj Ks · xwOBA · Proj Outs · Proj FD), Weather boost, AWAY @ HOME
2. **Daily HR Probability** — live lineups, game HR%
3. **Today’s Top Picks** — SP Ks · HR · TB · FD value
4. **Beta** — submenu for work-in-progress boards

**Beta menu**
- **DFS Lineups** — FanDuel classic $35k optimizer
- **Underdog Props** — model board + import
- **FD DFS Projections** — ranked FanDuel-pts board

Footer: **Models** · **Settings**. DFS / Props / FD projections are no longer primary home tiles.

## Today’s Top Picks

A daily digest of four live formulas. Default date is **America/Los_Angeles** today.

| Section | How it’s chosen |
| --- | --- |
| **Top SP K spots** | Live Projected Starters, rain last, then **Proj Ks**, then outlook |
| **Top HR spots** | Daily HR Probability, ranked by **game HR%** |
| **Top TB spots** | Expected total bases: `TB ≈ PA × (1·1B + 2·2B + 3·3B + 4·HR) / PA` |
| **Top FD value** | FD DFS Projections: **pts/$1k** when salary exists, else Proj FD pts |

**TB formula** (Option 6 only — not a standalone home screen): season 1B/2B/3B/HR
rates from the MLB Stats API, shrunk toward league priors (80 PA). HR/PA uses
Daily HR `p_PA` (park × weather × pitcher HR/9 × platoon). Doubles/triples get a
muted tilt; singles stay nearly park-neutral. FD DFS Projections still show
Proj TB on hitter rows.

Each pick includes a short **Why** line. Date nav matches the other boards.
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
- Top Picks FD value uses EXAMPLE salaries (same as Option 5) unless a CSV was imported there.
- Proj TB is not Statcast xTB — season rates plus the existing park/pitcher/weather stack.
- TB is a Today’s Top Picks formula only. There is no standalone Total Bases home tile.
- DFS Lineups, Underdog Props, and FD DFS Projections live under **Beta**, not on the main home list.
- Proj Outs is a local IP model (recent/season logs + opponent OPS + Weather boost + early exits), not Statcast or a pitch-count feed.
- Opponent K colors use season team SO/PA. Sparse seasons fall back to 21.6% / 23.4%. Retractable roofs stay outdoor unless MLB says closed.
- Proj FD W / QS / ER terms are local heuristics, not book odds or a pitch-level model.

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
