# Pablito’s Sportsbook

Android app for **today’s MLB slate** — live projected starters, daily HR probability,
FanDuel classic DFS lineups, a **FanDuel DFS projections board**, and an Underdog-style
props board. Dark navy + green.

## Home

1. **Projected Starters** — live MLB probables, outlook, weather/park, xwOBA, sort
2. **Daily HR Probability** — live lineups, game HR%
3. **DFS Lineups** — FanDuel classic $35k optimizer
4. **FD DFS Projections** — ranked FanDuel-pts board for a chosen slate
5. **Underdog Props** — model board + import

Footer: **Models** · **Settings**.

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
| Driver | pitcher **Proj Ks**, hitter **HR%** |

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

## Option 1 weather (unchanged)

Open-Meteo hourly at park lat/long. MLB `azimuthAngle` for wind vs CF.
Static multi-year **HR park factor** blended into tags + boost.

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
