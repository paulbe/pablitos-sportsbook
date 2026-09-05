# Pablito's Sportsbook

Personal baseball edge toolkit for Android. Kotlin + Jetpack Compose, Material 3, dark navy + green (`#22C55E`).

**Projected Starters is live.** Other boards still use mock/sample data (no FanDuel / Underdog APIs).

## Screens

- **Home** — four entries plus Models / Settings
- **Projected Starters** — today’s actual MLB probable SPs (America/Los_Angeles slate), ranked PROG → STABLE → REG by a documented outlook model
- **Daily HR Probability** — batters ranked by game HR% with park / weather / pitcher chips
- **DFS Lineups** — five swipeable FanDuel-style lineups (Cash core, NYY stack, LAD stack, Leverage, Contrarian)
- **Underdog Props** — Higher/Lower edges (model prob − implied)
- **Models** / **Settings** — placeholders

Regenerate / Export / Copy / Refresh / Sync / Add to slip are stubbed with toasts.

## Install the debug APK on a phone

The ready-to-install file is:

`dist/PablitosSportsbook-debug.apk`

On Android 8+: download that file, allow **Install unknown apps** for Chrome/Files if prompted, then tap **Install**.

## Open in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (Koala / Ladybug or newer is fine).
2. **File → Open** and select this project folder (the directory that contains `settings.gradle.kts`).
3. Let Gradle sync. If asked for an SDK, use **API 36** (compile/target) with **min SDK 26**.
4. Pick a phone emulator (API 26+) or a physical device with USB debugging.
5. Run `app`.

## Command line

Requires JDK 17+ and the Android SDK.

```bash
# Point Gradle at your SDK (Android Studio usually writes this for you)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

Install on a connected device or emulator:

```bash
./gradlew installDebug
```

## Live data: Projected Starters

**Today** is `LocalDate.now(America/Los_Angeles)`.

| Need | Source |
| --- | --- |
| Probable SPs, matchup, venue, weather, first pitch | [MLB Stats API](https://statsapi.mlb.com) `GET /api/v1/schedule?sportId=1&date=YYYY-MM-DD&hydrate=probablePitcher,venue,weather` |
| Team abbreviations | `GET /api/v1/teams?sportId=1` |
| Season K% / BF / GS / strike% | `GET /api/v1/people/{id}/stats?stats=season,gameLog&group=pitching&season=YYYY` |
| Recent form | Last **5** `gamesStarted` rows from that game log |

**Outlook (see `OutlookCalculator.kt`)**

- Hypotheses: probablePitcher is the official SP; SwStr/Whiff/CSW are **not** on this people-stats feed, so K% (SO/BF) is the skill signal and strike% is a thin process proxy; last 5 GS ≈ recent form; league priors K% 22.5%, strike% 64%, 26 BF.
- `projK` = shrink(`0.50·recentK + 0.30·seasonK + 0.20·processK`) toward 22.5% with an 80-BF prior (`processK = 22.5% + (strike% − 64%) × 0.40`).
- `nextStartKs ≈ projK × expectedBF` (season BF/GS, else last start, else 26).
- `outlookScore = round((projK − 22.5%)×100 + (recentK − seasonK)×180)` → **PROG ≥ +5**, **REG ≤ −5**, else **STABLE**.
- Rank by outlook score, then proj K%.

Network or parse failure shows an error/empty state with **Retry**. The old Cole/Skubal mock list is **not** used as a silent fallback.

Limits: MLB may omit a probable until later in the day; no official rate-limit header (be polite — this app fetches one slate plus one stats call per pitcher); no Baseball Savant / Statcast in v1; weather is whatever the schedule hydrate includes.

## Project layout

```
app/src/main/java/com/pablitosb/sportsbook/
  MainActivity.kt
  navigation/     Navigation Compose graph
  theme/          Navy + #22C55E Material 3 theme
  data/model/     Board models
  data/remote/    MLB Stats API client
  data/starters/  Live probable-SP fetch + outlook ranking
  data/mock/      Sample slate for HR / DFS / props only
  ui/home|starters|hr|dfs|props|models|settings
```

## Out of scope (v1)

- Live Statcast / FanDuel / Underdog integration (HR, DFS, props stay mock)
- Auth, payments, Play Store listing
