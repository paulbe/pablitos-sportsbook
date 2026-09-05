# Pablito's Sportsbook

Personal baseball edge toolkit for Android. Kotlin + Jetpack Compose, Material 3, dark navy + green (`#22C55E`).

**Today** is always `America/Los_Angeles`. Options 1–4 read **MLB Stats API** (or a file you import). They do **not** fall back to the old hardcoded Cole/Judge demo athletes.

## What’s live vs import-needed

| Screen | Live from MLB | You must import | Never claimed live |
| --- | --- | --- | --- |
| **Projected Starters** | Probables, venue, weather, K outlook, pred vs actual | — | Statcast CSW/SwStr |
| **Daily HR Probability** | Day’s batters (posted lineups, else active roster hitters), season HR%/ISO/FB, park table, weather hydrate, opposing SP HR/9, platoon | — | Statcast barrel/xHR |
| **DFS Lineups** | Same projections → expected FanDuel points; optimizer (P, C/1B, 2B, 3B, SS, OF×3, UTIL, $35k, Cash/GPP, 5 lineups) | **Salaries** if you want real FanDuel prices | Live FanDuel salary feed |
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

### DFS salaries

There is **no live FanDuel scrape**. Default board uses **EXAMPLE** salaries derived from our projection ranks (labeled on screen). You can:

- **Import slate / paste** CSV: `name,team,pos,salary[,proj][,mlbId]`
- **Load EXAMPLE file** `app/src/main/assets/example_fd_slate.csv` (also labeled EXAMPLE)

The optimizer always runs on whatever slate is loaded. Regenerate / Export CSV / Copy lineup are real.

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

On Android 8+: allow **Install unknown apps**, then install. GitHub release **v0.1.3-debug** publishes the same file as `PablitosSportsbook-debug.apk`.

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

## Limits

- MLB may omit tomorrow’s probables or today’s lineups until posted — we then use roster hitters, not fake stars.
- Park factors are a static table, not a live Statcast park feed.
- Weather is whatever the schedule hydrate includes.
- EXAMPLE DFS salaries are a transparent formula, not FanDuel.
- No Underdog / FanDuel login. Import if you have a real slate or lines.
- Polite request volume: one slate + season dumps + rosters + pitcher logs.

## Project layout

```
app/src/main/java/com/pablitosb/sportsbook/
  data/remote/       MLB Stats API client
  data/starters/     Option 1 outlook
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
