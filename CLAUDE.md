# KTamilCalendar — project brief for Claude

Tamil calendar + panchangam for **Android TV / Google TV**, also phones/tablets (one adaptive APK).
Kotlin, Jetpack Compose + `androidx.tv:tv-material`, MVVM, Room, Retrofit. minSdk 21 · targetSdk 30 · compileSdk 34.

## Rules
- **Ask before any `git commit` / `git push`.** Leave changes uncommitted, summarise, ask.
- Versions in `gradle/libs.versions.toml` are pinned to the last **minSdk-21** releases — don't bump.
- UI text: import `com.kamalsoft.tamilcalendar.tv.ui.components.Text` (not tv-material's) — it implements Tamil-only mode.
- Labels are bilingual `"தமிழ் · English"`; the weather page is English-only.
- Colours only via `TvColors` (palette-driven themes); never hard-code colours.
- Layout is designed on a **960×540 dp canvas** (TV/landscape) or **412 dp wide** (`DisplayPrefs.compact`, portrait). See `ui/theme/DesignCanvas.kt`.
- Every focusable element is a `TvFocusCard` (D-pad + touch). Keep D-pad focus paths working.

## Map
- `core/astro/` panchangam engine (Astro.kt math, TamilCalendarEngine.kt rules + festivals)
- `core/holidays/` US/IL holidays, world religions (Christian/Jewish/Islamic)
- `domain/model/` TamilDayData, AppSettings (+ filtering), LocationPolicy
- `data/` Room cache (DB version bump = wipe), repositories, location detector, weather
- `di/` AppContainer + per-location CalendarSession
- `ui/` app shell (TopTabBar for TV, CompactNav for phones, TamilCalendarTvApp), screens per folder, components (TvFocusCard, Motion, Text)
- `ui/ambient/` idle ambient screen + system screensaver (DreamService) · `data/audio/Speaker` TTS · `data/notify/` reminders (AlarmManager)
- `domain/content/` sloka texts (public domain) and gochara texts; `core/astro/Planets.kt` Jupiter/Saturn/Rahu
- `tools/install_tv.sh` build+install to TV/emulator · `tools/verify_panchangam.py` accuracy check

## Skills
`build-install`, `e2e-test`, `verify-panchangam`, `add-calendar-rule`, `release` (in `.claude/skills/`).
