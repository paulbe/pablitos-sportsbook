# Pablito's Sportsbook

Personal baseball edge toolkit for Android. Kotlin + Jetpack Compose, Material 3, dark navy + green (`#22C55E`).

**Today** is always `America/Los_Angeles`. Options 1–4 read **MLB Stats API** (or a file you import). They do **not** fall back to the old hardcoded Cole/Judge demo athletes.

## What’s live vs import-needed

| Screen | Live from MLB | You must import | Never claimed live |
| --- | --- | --- | --- |
| **Projected Starters** | Probables, venue, Open-Meteo × HR park factor, K outlook, pred vs actual, Savant xwOBA | — | CSW / SwStr / invented weather |
| **Daily HR Probability** | Day’s batters (posted lineups, else active roster hitters), season HR%/ISO/FB, park table, weather hydrate, opposing SP HR/9, platoon | — | Statcast barrel/xHR |
| **DFS Lineups** | Same projections → expected FanDuel points; **Choose slate** (Main / Early / Late / Showdown); optimizer (classic $35k) | **Salaries** if you want real FanDuel prices (`api.fanduel.com/fixture-lists` is 401 without login) | Live FanDuel salaries labeled as live |
| **Underdog Props** | Model Ks / HR 0.5 / hits probabilities | **Lines + odds** if you want edge vs a book | Live Underdog odds |

### Daily HR formula (implemented)

`p_PA = talent × park × weather × pitcher × platoon`  
`Pr(at least one HR) = 1 − (1 − p_PA)^expectedPA`

- **Talent:** shrink(`0.70·HR/PA + 0.20·ISO-scaled + 0.10·FB-scaled`) toward 3.2% HR/PA with an 80-PA prior. ISO = SLG−AVG; FB = airOuts/(airOuts+groundOuts). No barrels on this API.
- **Park:** static venue HR multiplier (Coors 1.28, Yankee Stadium 1.15, Oracle 0.88, …).
- **Weather:** schedule hydrate wind out/in × temperature; dome/roof ≈ 1.00.
- **Pitcher:** (HR/9 / 1.15) × mild air-out tendency.
- **Platoon:** 1.08 opposite, 0.92 same-side, 1.03 switch vs RHP.

Date nav matches starters (◀ / date chip / ▶ / Today). Failure = error + **Retry**, no mock list.

### DFS salaries and Choose slate

We call `GET https://api.fanduel.com/fixture-lists` (the JSON unofficial FD clients use). **Without a FanDuel login it returns 401.** We do not scrape HTML or invent live FanDuel prices.

**Choose slate** then lists **MLB-derived** FanDuel-style pools for the day:

- **Main** — all of that date’s games
- **Early** — first pitch before 5pm America/Los_Angeles (hidden if every game is late)
- **Late** — evening games
- **Showdown** — one slate per game (classic 9-spot on that pool, **not** FanDuel MVP Showdown scoring)

Default salaries are labeled **EXAMPLE** (projection-rank formula or the bundled file). Import CSV `name,team,pos,salary[,proj][,mlbId]` to run the optimizer on real prices you paste. If FanDuel ever returns fixture-lists without auth, those rows are tagged `FanDuel live`.

FanDuel MLB points used: hitters 1B 3 · 2B 6 · 3B 9 · HR 12 · RBI 3.5 · R 3.2 · BB 3 · SB 6; pitchers W 6 · QS 4 · ER −3 · SO 3 · IP 3.

### Underdog lines

There is **no Underdog API**. Default is a **Model board** (our lines/probs only; implied and edge show —). **Refresh model** re-pulls MLB. **Sync Underdog** explains import. Paste CSV `player,market,line,side,odds` or load `example_underdog_lines.csv` (EXAMPLE). Edge = modelProb − impliedProb(American odds) only after import.

## Screens

- **Home** — four entries plus Models / Settings
- **Projected Starters** — live slate / reconstructed pred vs actual
- **Daily HR Probability** — ranked game HR%
- **DFS Lineups** — five swipeable Cash/GPP lineups from the optimizer
- **Underdog Props** — model board + optional imported lines
- **Models** / **Settings** — what’s live vs import

## Install the debug APK

`dist/PablitosSportsbook-debug.apk`

On Android 8+: allow **Install unknown apps**, then install. GitHub release **v0.1.7-debug** publishes the same file as `PablitosSportsbook-debug.apk`.

## Open in Android Studio

1. Install [Android Studio](https://developer.android.com/studio).
2. **File → Open** this folder (`settings.gradle.kts`).
3. SDK **API 36**, min **26**.
4. Run `app`.

## Command line

JDK 17+ and the Android SDK:

```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Live MLB endpoints (same family as Option 1)

| Need | Source |
| --- | --- |
| Games, lineups, probables, venue, weather | `GET /api/v1/schedule?sportId=1&date=YYYY-MM-DD&hydrate=lineups,probablePitcher,venue,weather` |
| Team abbreviations | `GET /api/v1/teams?sportId=1` |
| Season hitting / pitching | `GET /api/v1/stats?stats=season&group=hitting\|pitching&season=YYYY&sportIds=1&playerPool=all` |
| Active roster fallback | `GET /api/v1/teams/{id}/roster?rosterType=active` |
| Hands / names | `GET /api/v1/people?personIds=…` |
| SP game logs (K outlook) | `GET /api/v1/people/{id}/stats?stats=season,gameLog&group=pitching&season=YYYY` |
| Pitcher xwOBA against | Baseball Savant `expected_statistics?type=pitcher&year=YYYY&min=1&csv=true` (`est_woba`) |
| Park lat/long + CF azimuth | Schedule `hydrate=venue(location)` (`defaultCoordinates`, `azimuthAngle`) |
| Hourly temp / wind / precip | Open-Meteo forecast (no key); archive API for older slates |

## Limits

- MLB may omit tomorrow’s probables or today’s lineups until posted — we then use roster hitters, not fake stars.
- Park factors are a static table, not a live Statcast park feed.
- Starters weather is **Open-Meteo** at the park lat/long for first-pitch hour, rotated by MLB CF `azimuthAngle`. MLB schedule hydrate is a backup only (often empty until near first pitch). Fetch failure → Neutral / —, never invented wind.
- Wind tags (`ParkWeather`) blend Open-Meteo with the same multi-year **HR park factor** as Daily HR (BB-Ref / FanGraphs style, 1.00 = average). RAIN RISK is weather-only (PF never clears it). HR parks (PF ≥ 1.12, Coors 1.28) tag HR on milder out/heat; pitcher parks (PF ≤ 0.94, Petco 0.90) need a strong out or real heat. League-average thresholds stay out ≥ 6 mph or ≥ 82°F.
- Fixed domes are always indoor. Retractable roofs are outdoor unless MLB says the roof is closed.
- xwOBA is season-to-date Statcast expected wOBA against (`est_woba`). Fetch failure or a missing pitcher is **—**, never a made-up number.
- EXAMPLE DFS salaries are a transparent formula, not FanDuel.
- No Underdog / FanDuel login. Import if you have a real slate or lines.
- Polite request volume: one slate + season dumps + rosters + pitcher logs.

## Project layout

```
app/src/main/java/com/pablitosb/sportsbook/
  data/remote/       MLB Stats API + Savant CSV + Open-Meteo
  data/mlb/          Park HR factors + roof / lat-long fallbacks
  data/starters/     Option 1 outlook + park-relative wind tags
  data/hr/           HR probability model
  data/projections/  Shared live slate (hitters + SPs + FD points)
  data/dfs/          FanDuel scoring + optimizer + salary import
  data/props/        Model board + line import
  data/mock/         Unused leftover sample (screens do not read it)
  ui/home|starters|hr|dfs|props|models|settings
app/src/main/assets/
  example_fd_slate.csv
  example_underdog_lines.csv
```
