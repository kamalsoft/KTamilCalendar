# How the panchangam is calculated

This is the reference for every number the app shows for a day. The engine lives in
`shared/src/main/java/com/kamalsoft/tamilcalendar/tv/core/astro/` (Kotlin) and is ported line for line to
`web/src/main/resources/web/engine/` (JavaScript). `web/tools/check_engine.mjs` checks that both give the same output,
and `tools/verify_panchangam.py` checks the astronomy against the Swiss Ephemeris.

Unless stated otherwise:

- A **Tamil day** runs from one sunrise to the next. The day is named by its civil date.
- All times are local time in the place's time zone.
- Angles are in degrees, zodiac longitudes run 0–360° from Mesham (0°), and `floor` means the mathematical floor.

---

## 1. Astronomy (`Astro.kt`, `astro.js`)

### 1.1 Julian day and time
- `julianDay(instant) = epochMillis / 86 400 000 + 2 440 587.5` (UT).
- Terrestrial time: `ΔT = 69.2 s` (2024–2028), added in the astronomy formulas.

### 1.2 Sun
- Meeus, *Astronomical Algorithms* ch. 25: mean longitude, mean anomaly, equation of the centre, then the apparent
  longitude with nutation. Accuracy about 0.01°.
- Tropical longitude `sunLongitude(jd)`; sidereal `sunSidereal = norm360(tropical − ayanamsa)`.

### 1.3 Moon
- Meeus ch. 47, the full 60-term longitude series. Accuracy about 0.003°.
- `moonSidereal = norm360(moonLongitude − ayanamsa)`.

### 1.4 Ayanamsa (Lahiri)
- `ayanamsa(jd) = 23.857092 + 1.396971·T + 0.000308·T²`, where `T` is Julian centuries from J2000.
- Gives about 24.2° in 2026.

### 1.5 Sunrise and sunset (`sunriseSunset`)
- Solar declination and the equation of time from the mean solar longitude.
- Hour angle `w` where the Sun's altitude is −0.833° (refraction plus the solar radius):
  `cos w = (sin(−0.833°) − sin φ · sin δ) / (cos φ · cos δ)`.
- Sunrise = transit − `w/360` day, sunset = transit + `w/360` day.
- Accuracy against the Swiss Ephemeris: sunrise mean error 0.26 min, sunset 0.78 min (Naperville, one year).

### 1.6 Sky events
- `elongation = norm360(moonLongitude − sunLongitude)`. This is the Moon–Sun angle used for tithi and karanam.
- `yogaSum = norm360(sunSidereal + moonSidereal)`.
- `findCrossing(from, maxDays, step, target, angle)`: steps forward in `step`-day increments until the angle crosses
  `target`, then bisects the interval 50 times. Used for every "ends at" time.

---

## 2. Tithi (lunar day) — `TamilCalendarEngine.buildDay`

- Index `t = floor(elongation(sunrise) / 12)`, 0–29. Index 0 is Prathamai (Shukla), 14 is Pournami, 15 is Prathamai
  (Krishna), 29 is Amavasai.
- Paksham: Shukla if `t < 15`, else Krishna.
- Name: `Panchangam.tithiNames`, from `TamilNames.tithis[t mod 15]`, with Pournami and Amavasai named separately.
- End time: the first moment after sunrise when `elongation` reaches `((t + 1) mod 30) · 12`.
- Tithi on the day is taken **at sunrise**. Festival rules that need a different time use the other instants defined in
  `At` (sunset, midnight, moonrise, and the "Aparahna" instant, see §8).

## 3. Nakshatram (star)

- Index `n = floor(moonSidereal / (360/27)) mod 27`, where 360/27 = 13°20′. Index 0 is Aswini.
- Name from `TamilNames.nakshatras`. The end time uses `moonSidereal` as the angle.

## 4. Yogam

- Index `y = floor(yogaSum / (360/27)) mod 27`.
- Name from `TamilNames.yogas`. The end time uses `yogaSum` as the angle.

## 5. Karanam (half tithi) — `DayRules.karanaName`

- Index `k = floor(elongation(sunrise) / 6)`, 0–59.
- `k = 0` is Kimstughna. `k = 1..56` repeat the seven movable karanams in order: Bava, Balava, Kaulava, Taitila,
  Gara, Vanija, Vishti (Bhadra). `k = 57, 58, 59` are Shakuni, Chatushpada, Naga.
- Vishti (`(k − 1) mod 7 = 6`) is marked as "avoid for starts".

## 6. Moon sign (Chandra rasi) and Sun sign

- `moonRasi = floor(moonSidereal(sunrise) / 30)`, with the time it leaves the sign found by `endOf`.
- `sunRasi = floor(sunSidereal(sunrise) / 30)`.

## 7. Chandrashtamam

- The 8th sign from the Moon's sign: `rasi = (moonRasi + 5) mod 12`.
- Nakshatras in that sign are the 27 star divisions overlapping the 30° sign.
- Shown until the Moon leaves its sign (`moonRasiEndsAt`).

## 8. Tamil month and day

- A Tamil solar month starts on the civil day the Sun enters a sidereal sign. If the entry is before local sunset the
  month starts that civil day, otherwise the next day. The entry time is found with `findCrossing` on `sunSidereal`.
- `tamilDay = (civil date − month start) + 1`.
- Month index 0 is Chithirai, 11 is Panguni. The Tamil year follows the Chithirai new year.
- Thiruvalluvar Aandu increments on Thai 1.
- The 60-year cycle index is `floor(startYear − 1987) mod 60`.

---

## 9. Lagnam (rising sign) — `Astro.ascendantSidereal`, `lagnaSegments`

- The sidereal ascendant at a UT instant and a place:
  - Greenwich mean sidereal time `GMST = 280.46061837 + 360.98564736629·(jd − J2000) + 0.000387933·T² − T³/38 710 000`.
  - Local sidereal time `LST = GMST + longitude`.
  - Obliquity `ε = 23.4392911 − 0.0130042·T`.
  - Tropical ascendant `atan2(cos LST, −(sin LST·cos ε + tan φ·sin ε))`, then minus the Lahiri ayanamsa.
- The sign is `floor(ascendant / 30)`.
- Scan from sunrise to the next sunrise in 10-minute steps. Each change of sign is refined by 20 bisection steps,
  to about a second.
- Result: a list of `(rasi, start time)`. Usually 11–14 signs per day, each lasting about 1–3 hours.
- Accuracy: 4 392 sign changes in a year, mean error 0.21 min, worst 0.25 min against Swiss Ephemeris houses.

## 10. Sraardha tithi (`sraardhaTithiOf`, `aparahnaCounts`)

- Aparahna Kala is the 4th fifth of the local daytime: `U = (sunset − sunrise) / 5`, window `[sunrise + 3U, sunrise + 4U]`.
- The window is sampled at 16 evenly spaced instants. The tithi `floor(elongation / 12)` at each sample is counted.
- **Majority:** the tithi with more than half of the samples (9 or more) is the Sraardha tithi.
- **Tie rule (validator Step 3C.3):** a tithi with exactly half (8 of 16) is the Sraardha tithi only if it also has exactly
  half on the previous day. The second day takes it, so the first day gets none.
- Amavasai (29) is never used. Otherwise there is no Sraardha tithi.
- Verified against Swiss Ephemeris for 365 days (Naperville), 0 mismatches, using the same sampling and the tie rule.
- Not yet implemented: the Darsha (Amavasai) day selection with Madhyahna (§13a), and the Mahalaya-specific days.

---

## 11. Timing windows — `timingsFor`

Every place uses the same rule (validator Step 3B.5): the daytime from sunrise to sunset is divided into eight equal
parts of `(sunset − sunrise) / 8`. The `TimingConvention` setting no longer changes the windows. The former India
convention (fixed 90-minute blocks from 06:00) is removed because the validator rules it out.

Each window is one part, numbered 1–8 from the start of the day:

- **Rahu Kalam** part by weekday (Sunday = 0): `[8, 2, 7, 5, 6, 4, 3]`.
- **Yamagandam** part: `[5, 4, 3, 2, 1, 7, 6]`.
- **Kuligai** part: `[7, 6, 5, 4, 3, 2, 1]`.
- **Gowri**: eight parts, starting from the cycle index `(weekday + n − 1) mod 8` into the cycle
  `[Uthi, Amirdham, Rogam, Laabam, Dhanam, Sugam, Soram, Visham]`. Uthi, Amirdham, Laabam, Dhanam and Sugam are good;
  Rogam, Soram and Visham are not.
- **Nalla Neram** = the good Gowri parts, with Rahu Kalam and Yamagandam cut out, merged where adjacent. Only pieces of
  at least 30 minutes are kept.

## 12. Daily tags (festival-like flags) — `buildDay`

Each tag checks a tithi or star at a fixed instant. `observed` treats a tithi that is skipped (kshaya) or repeated
(vriddhi) correctly by looking at the day before and after.

- **Pournami**: tithi 14 at sunrise. **Amavasai**: tithi 29 at sunrise.
- **Ekadasi**: tithi 10 or 25 at sunrise. **Pradosham**: tithi 12 or 27 at sunset.
- **Krithigai**: star 2 at sunrise. **Thiruvonam**: star 21 at sunrise.
- **Sashti** (Valarpirai): tithi 5 at sunset. **Masi Sivaratri**: tithi 28 at midnight.
- **Sankatahara Chathurthi**: tithi 18 at moonrise (the K4 instant).
- **Purattasi Sani**: Purattasi month and a Saturday. **Aadi Velli**: Aadi month and a Friday.
- **Subha Muhurtham** (a computed candidate): see §13.

## 13. Subha Muhurtham candidate — `isMuhurthamCandidate`

A day is a candidate when all of these hold:

0. The day is not a Kari Naal day (validator gate 1, §14.4).
   Tuesday and Saturday are excluded from the Subha Muhurtham tag (the marriage and housewarming rule in the validator's
   section E). They are flagged in the day view, not blacked out.
1. The Tamil month is not Aadi (3), Purattasi (5) or Margazhi (8).
2. The Moon's month is not an adhika (extra) month.
3. The weekday is not Tuesday or Saturday.
4. Shukla: tithi index in {1, 2, 4, 6, 9, 10, 12}, i.e. Dwitiyai, Tritiyai, Panchami, Sapthami, Dasami, Ekadasi,
   Thrayodasi (the validator's best tithis). Krishna: tithi − 15 in {0, 1, 2, 3, 4}, i.e. Prathama to Panchami
   (the validator's rule). Saptami and Dashami are not included until the open question is answered.
5. The yogam is not malefic (validator section D, audited): 0-based {0, 5, 8, 9, 12, 14, 16, 18, 26}, i.e. 1-based
   1 Vishkambam, 6 Atiganda, 9 Shula, 10 Ganda, 13 Vyaghata, 15 Vajra, 17 Vyatipata, 19 Parigha, 27 Vaidhriti.
   Sukarma (7) is auspicious. Whole-day rejection: the day is rejected if a malefic yogam occurs at any time between sunrise
   and sunset. The engine samples the daylight every 30 minutes, which cannot miss a yogam (each lasts about a day).
   Partial-ghati rules are not implemented.
6. The star index is one of {3, 4, 9, 11, 12, 14, 16, 18, 20, 21, 22, 25, 26}: Rohini, Mirugasirisham, Magam,
   Uthiram, Hastham, Swathi, Anusham, Moolam, Uthiradam, Thiruvonam (Shravana), Avittam (Dhanishta), Uthirattathi
   (Uttara Bhadrapada), Revati. This is the validator's auspicious list.

The window shown is the first Nalla Neram before noon.

**Not implemented yet** (see `docs/BACKLOG.md`): Thara Balam, the personal Chandrashtamam check, and the search for a
Lagna window.

---

## 13a. Validator rules: status in the app

Each rule is taken from the astronomer-validator skill (`.claude/skills/tamil-panchangam-astronomical-validator/SKILL.md`).
"Implemented" means the engine computes it and the tests or parity checks cover it.

| Rule | Status |
|---|---|
| Krishna Paksha Muhurtham: Prathama to Panchami only (Krishna tithi 1–5). Saptami and Dashami not included (open question) | Implemented (engine 9) |
| Malefic yogas (1-based 1, 6, 9, 10, 13, 15, 17, 19, 27) block Subha Muhurtham if they occur at any time between sunrise and sunset. Sampled every 30 min. Sukarma (7) is auspicious | Implemented (engine 10–11) |
| Subha Muhurtham excludes Kari Naal days (gate 1) | Implemented (engine 8) |
| Tuesday and Saturday excluded from the Subha Muhurtham tag (marriage rule) | Implemented. The day view does not yet flag them with their permitted uses. |
| Hora: 12 equal parts of daytime and 12 of night, Chaldean order from the weekday lord | Implemented (engine 11): APK Hora card, web Today chips |
| Sraardha tithi tie rule (second day gets an exact-half tithi) | Implemented (engine 11) |
| Magha and Moola permitted for marriage; Adhomukha applies to construction only | Adhomukha filters not used by the app (no construction filters) |
| Nakshatram quality (Dhruva, Mridu, Adhomukha) | Not displayed |
| Lagna Shuddhi (8th house, benefics in Kendra and Trikona, Lagna lord strength) | Not implemented. Needs positions of all planets (Mars, Mercury and Venus are not computed yet). |
| Marana Yogam (weekday and Nakshatra pair) | Not implemented. Needs the classical lookup table. |
| Thara Balam and personal Chandrashtamam | Not implemented. Needs the birth star and birth Rasi as settings. |
| Darsha (Amavasai) day selection with Madhyahna | Not implemented |
| Mahalaya rules (Ghata Chaturdashi restriction and the designated days) | Not implemented |
| Partial-ghati (Tyajya) rules for malefic yogas | Not implemented. The app uses whole-day rejection. |
| Kari Naal table in the day view | Implemented (Karanam card) |

**Worked checks.** Sun $120^\circ$ with Moon $300^\circ$ gives $\Delta\lambda = 180^\circ$ (Pournami). Sun $100^\circ$ with Moon
$310^\circ$ gives $\Delta\lambda = 210^\circ$ and block 18, Krishna Tritiya, so the verdict is `PASS (conditional)` on Chandra
Balam and Lagna Shuddhi. The earlier Navami example ($\Delta\lambda = 105^\circ$) is Shukla Navami, not Krishna Ashtami.

## 14. Day-level rules (`DayRules.kt`, `rules.js`)

### 14.1 Soolam (Vaara Soolam)
- Direction for the day from the weekday of its civil date (sunrise to next sunrise):

| Weekday | Direction | Remedy (Tamil) | Remedy (English) |
|---|---|---|---|
| Sunday | West | நெய் (அ) வெற்றிலை | Ghee or betel leaf |
| Monday | East | பால் (அ) தயிர் | Milk or curd |
| Tuesday | North | வெல்லம் (அ) பால் | Jaggery or milk |
| Wednesday | North | பால் (அ) கொத்தமல்லி / எள் | Milk, coriander or sesame |
| Thursday | South | தயிர் (அ) சீரகம் | Curd or cumin |
| Friday | West | பார்லி (அ) நெய் / வெல்லம் | Barley, ghee or jaggery |
| Saturday | East | உளுந்து (அ) எள் / இஞ்சி | Urad dal, sesame or ginger |

- Monday, Saturday and Sunday agree with prokerala.com. Tuesday to Friday come from the Tamil almanac source.

### 14.2 Parigaram (weekday remedy)
- One remedy per weekday, shown on the Soolam card (English, from the Tamil source).

### 14.3 Abhijit Muhurtham
- Centred on solar noon: `noon = sunrise + daytime/2`, window `noon ± daytime/30` (validator Step 3B.6). About
  46–48 minutes in Chennai, and it varies with the day length elsewhere.

### 14.4 Kari Naal
- Solar days in each Tamil month when no Subakariyam is held. Flagged on the Karanam card.

| Tamil month | Days |
|---|---|
| Chithirai | 6, 15 |
| Vaikasi | 7, 16, 17 |
| Aani | 1, 6 |
| Aadi | 2, 10, 20 |
| Aavani | 2, 9, 28 |
| Purattasi | 16, 29 |
| Aippasi | 6, 20 |
| Karthigai | 1, 10, 17 |
| Margazhi | 6, 9, 11 |
| Thai | 1, 2, 3, 11, 17 |
| Maasi | 15, 16, 17 |
| Panguni | 6, 15, 19 |

---

### 14.5 Hora (planetary hours) — `DayRules.hora`, `rules.js` `hora`
- Inputs: weekday, sunrise, sunset and the next sunrise.
- Day horas: 12 equal parts of `(sunset − sunrise)`, starting at sunrise. Night horas: 12 equal parts of
  `(next sunrise − sunset)`, starting at sunset. The next sunrise is stored with each day, so the night length is exact.
- Lords: the first hora of a weekday belongs to that weekday's lord (Sunday Sun, Monday Moon, Tuesday Mars, Wednesday
  Mercury, Thursday Jupiter, Friday Venus, Saturday Saturn). The sequence Sun, Venus, Mercury, Moon, Saturn, Jupiter, Mars
  then repeats.
- Benefic horas: Jupiter, Venus, Mercury, Moon. Malefic: Sun, Mars, Saturn.
- Shown on the APK Daily screen (Hora card, all 24 in the detail panel) and as chips on the web Today tab.

## 15. Festivals and holidays

- **Computed festivals** (`TamilCalendarEngine.computeFestivals`): each rule has a Tamil and English title, a kind
  (festival, viratham, holiday, religious, school), a deity or religion, and a matcher on the date.
- **Civil and world-religion days** (`holidays.js`, `core/holidays`): US federal and Illinois holidays, Christian,
  Jewish and Islamic observances. Islamic dates depend on local moon sighting, so they can be a day off.
- **School calendars** (`app/src/main/assets/school_calendars.json`, the same file the APK ships): one event per day of
  each school break, for the district's region. Several names can cover the same break; the Festivals view shows one
  line per school closure per day.
- **Important dates**: the festival, tag and religion events of a day, with holidays first.

## 16. Settings that change the output
- **Place**: latitude, longitude and time zone, which change sunrise, sunset and every time-based value. Every calculation
  uses the selected place. Before a place is chosen the defaults are Naperville on the APK (`AppContainer`) and Chennai
  on the web (`store.js`). The two defaults differ and are an open decision (see `docs/BACKLOG.md`).
- **Timing convention**: `AUTO` picks `TRADITIONAL_6AM` for India and `SUNRISE_BASED` elsewhere.
- **Faiths and school districts**: filter which events are listed (`applying` in `calendar.js`).

---

## 17. Versions and caches

- `TamilCalendarEngine.VERSION` (currently 11) is part of the cached-day key. Bumping it recomputes cached days.
- The APK's Room database (`CalendarDatabase`, currently version 6) stores the computed day, including the next sunrise. Changing the stored fields
  bumps the database version, which wipes and rebuilds the cache.
- Web offline years are kept in Cache Storage under `ktc-packs-v4`. Saved years from before engine 11 are dropped (cache `ktc-packs-v4`) and should be saved again. Changing the day shape means bumping that name.

## 18. How it is verified

| Check | Command | What it covers |
|---|---|---|
| Unit tests | `./gradlew testDebugUnitTest` | Engine reference dates, lagna order, rules tables |
| Astronomy | `tools/verify_panchangam.py` (Swiss Ephemeris) | Sunrise, sunset, tithi, nakshatram, yogam ends, Tamil month, Lagnam changes, Sraardha tithi |
| Web parity | `./gradlew :web:referenceData` then `node web/tools/check_engine.mjs <place> <yyyy-MM>` | Every field of the JavaScript engine against Kotlin |

Known limits: sunrise and sunset use the standard −0.833° altitude, not a local almanac table. Tithi and star ends
are accurate to about a minute. Festival and Muhurtham rules follow the tradition as recorded in the code and the
sources listed in `docs/BACKLOG.md`, and they are not independently verified.
