# Pablito's Sportsbook

Personal baseball edge toolkit for Android. Kotlin + Jetpack Compose, Material 3, dark navy + green (`#22C55E`).

v1 ships four working boards on **mock/sample data** — no Statcast, FanDuel, or Underdog live APIs.

## Screens

- **Home** — four entries plus Models / Settings
- **Projected Starters** — today's SPs ranked PROG → STABLE → REG
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

## Project layout

```
app/src/main/java/com/pablitosb/sportsbook/
  MainActivity.kt
  navigation/     Navigation Compose graph
  theme/          Navy + #22C55E Material 3 theme
  data/model/     Board models
  data/mock/      Sample slate used by every screen
  ui/home|starters|hr|dfs|props|models|settings
```

## Out of scope (v1)

- Live Statcast / FanDuel / Underdog integration
- Auth, payments, Play Store listing
