---
name: add-calendar-rule
description: Add or fix a festival, observance, holiday or religious date rule (Hindu, Jain, Sikh, Buddhist, Christian, Islamic, Jewish, US/IL, school). Use when a date is missing or wrong.
---

# Add / fix a calendar rule

Where rules live:
- Hindu/Jain/Sikh/Buddhist (tithi, nakshatra, solar/lunar month): `core/astro/TamilCalendarEngine.kt` → `computeFestivals` (`FestivalRule(ta, en, kind, isHoliday, deity, religion) { predicate }`). Helpers: `solar(d, month, day)`, `inSolar(d, m)`, `lunar(d, amantaMonth, At.X)`, `tithiObs(d, tithi, At.X)`, `starObs(d, nak, At.X)`, `nearFullMoon`. Indices are 0-based (tithi 0–29, 14 = Pournami, 29 = Amavasai; months 0 = Chithirai / Chaitra). Pick the observance instant (`SUNRISE`, `NOON`, `APARAHNA`, `SUNSET`, `MIDNIGHT`) per tradition.
- Monthly observance tags (Sashti, Pradosham…): `buildDay` in the same file + `DayTag` (domain) + badge in `ui/components/Badges.kt`.
- Christian/Jewish/Islamic: `core/holidays/WorldReligions.kt`. US/Illinois: `core/holidays/UsHolidays.kt`.
- School calendars: data only, `app/src/main/assets/school_calendars.json` (new block per school year).

Always:
1. **Find a published reference date first** (WebSearch) — never test against memory. Prefer 2 years.
2. Add an assertion to `TamilCalendarEngineTest` / `WorldReligionsTest` / `HolidaysTest`.
3. Bump `TamilCalendarEngine.VERSION` if engine output changes (forces the on-device cache to rebuild).
4. Run tests (see build-install).
