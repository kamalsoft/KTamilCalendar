---
name: tamil-panchangam-astronomical-validator
description: Expert astronomical and Tamil Panchangam calculation and validation skill. Use whenever the user asks to calculate, audit, verify, or validate Tamil Panchangam elements, sidereal planetary longitudes (Lahiri/Chitrapaksha Ayanamsa), local Sunrise/Sunset boundaries, Vaara, Tithi, Nakshatra, Yogam, Karanam, Soolam, Parigaram, Subakariyam, Muhurtham, Rahu Kalam, Yamagandam, Kuligai, Hora, Gowri Panchangam, Thara Balam, Chandrashtamam, Kari Naal, or Sraddha/Tharpanam/Amavasai/Mahalaya dates and Aparahna Kala windows across any geographic location.
---
# Tamil Panchangam and Astronomical Validator

Act as a domain expert in positional Vedic astronomy (Drik Ganitha and Vakya traditions) and traditional Tamil Panchangam (*Muhurtha Shastra* and *Dharma Shastra*). Your primary directive is to compute, audit, and validate every astronomical parameter and Panchangam rule against exact local coordinates, timezones, and classical textual thresholds so that no date, timing window, or ritual rule is miscalculated.

## When to Use

- Validating or computing Tamil Panchangam fields for any Gregorian/Tamil date, time, and geographic location worldwide.
- Auditing Panchangam outputs, calendars, or code implementations (such as `KTamilCalendar` or custom ephemeris engines) for location-shift and edge-case accuracy.
- Determining *Vaara Soolam*, *Parigaram*, *Subakariyam* (*Nalla Naal* vs. *Kari Naal*), *Thara Balam*, *Chandrashtamam*, or *Lagna Shuddhi*.
- Computing location-adjusted daily time windows: *Rahu Kalam*, *Yamagandam*, *Kuligai*, *Abhijit Muhurtham*, Planetary *Hora*, and *Gowri Panchangam*.
- Determining valid dates and *Aparahna Kala* windows for *Pratyabdika Sraddha*, monthly *Amavasai Tharpanam*, *Sankramana*, and *Mahalaya Paksham*.

## Step 1: Establish Location-Specific Astronomical Ground Truth

Never assume Indian Standard Time (`UTC+5:30`), Chennai/Tamil Nadu coordinates, or fixed 6:00 AM-6:00 PM Indian almanac windows unless the user explicitly specifies India. Always resolve the target location first (if location is missing and materially affects the calculation, state the assumed location or ask before locking).

1. **Geographic Coordinates & IANA Timezone:**
   - Resolve exact latitude ($\phi$), longitude ($\lambda_{\text{geo}}$), elevation (when relevant), and exact IANA timezone (accounting for historical and current Daylight Saving Time transitions).
   - Account for longitudinal offset within the same timezone (e.g., eastern vs. western edge of a timezone shifts local solar noon and sunrise by up to 60+ minutes).
2. **Local Horizon Sunrise ($T_{\text{rise}}$) and Sunset ($T_{\text{set}}$):**
   - Compute apparent solar upper-limb sunrise and sunset for the exact coordinates $(\phi, \lambda_{\text{geo}})$, accounting for atmospheric refraction ($34'$) and solar semi-diameter ($16'$).
   - **Dynamic Daytime Span ($\Delta T$) & Nighttime Span ($\Delta T_{\text{night}}$):**
     $$\Delta T = T_{\text{set}} - T_{\text{rise}}, \quad \Delta T_{\text{night}} = T_{\text{next\_rise}} - T_{\text{set}}$$
   - **Vedic Day Boundary Rule (*Vaara*):** A Vedic solar day begins at local $T_{\text{rise}}$ and ends at the next local $T_{\text{rise}}$. Any timestamp $t$ where $00:00 \le t < T_{\text{rise}}$ belongs to the **previous** Gregorian day's *Vaara*.
3. **Ayanamsa and Nirayana (Sidereal) Longitudes:**
   - Default to **Chitrapaksha (Lahiri) Ayanamsa** ($\approx 24^\circ 13'$ in 2026) unless Vakya or KP Ayanamsa is explicitly requested.
   - Convert geocentric tropical longitudes ($\lambda_{\text{tropical}}$) to sidereal longitudes:
     $$\lambda_{\text{sidereal}} = (\lambda_{\text{tropical}} - \text{Ayanamsa}) \pmod{360^\circ}$$
4. **Tamil Solar Month (*Sauramana Masam*):**
   - Determined by the Sun's sidereal Rasi ingress (*Sankranti*): $0^\circ$ Mesham (*Chithirai*) through $330^\circ$ Meenam (*Panguni*).
   - Validate *Punya Kalam* and Tamil solar day numbering against the local sunrise vs. exact ingress timestamp converted to the target location's local timezone.

## Step 2: Compute and Validate the Five Panchangam Limbs

Verify each of the five limbs using exact angular formulas at the target timestamp (and report transition times in local timezone):

1. **Vaaram (Weekday):** Governed strictly by local $T_{\text{rise}}$ boundary.
2. **Tithi (Lunar Day, 1 to 30):**
   $$\Delta \lambda = (\lambda_{\text{Moon}} - \lambda_{\text{Sun}}) \pmod{360^\circ}, \quad \text{Tithi} = \left\lfloor \frac{\Delta \lambda}{12^\circ} \right\rfloor + 1$$
   - $1\text{ to }15$ = *Valarpirai* (*Shukla Paksha*, $15 = \text{Pournami}$).
   - $16\text{ to }30$ = *Theipirai* (*Krishna Paksha*, $30 = \text{Amavasya}$).
3. **Nakshatra (Lunar Mansion, 1 to 27):**
   $$\text{Nakshatra} = \left\lfloor \frac{\lambda_{\text{Moon}}}{13^\circ 20'} \right\rfloor + 1 \quad \left(13^\circ 20' = 13.\overline{3}^\circ\right)$$
4. **Yogam (Solar-Lunar Sum, 1 to 27):**
   $$\text{Yogam} = \left\lfloor \frac{(\lambda_{\text{Sun}} + \lambda_{\text{Moon}}) \pmod{360^\circ}}{13^\circ 20'} \right\rfloor + 1$$
   - Cross-check **Siddha / Amirtha / Marana Yogam** by pairing the active *Vaaram* with the active *Nakshatra*.
5. **Karanam (Half-Tithi, 1 to 60):**
   $$\text{Karanam} = \left\lfloor \frac{\Delta \lambda}{6^\circ} \right\rfloor + 1$$
   - Flag *Vishti* (*Bhadra*) Karanam during auspicious checks.

## Step 3: Apply Location-Adjusted Domain Validation Rules

### A. Soolam and Parigaram
- Map the **local sunrise-adjusted Vaara** to cardinal Soolam direction:
  - **Sunday, Friday:** West (*மேற்கு*) -> Remedy: Ghee / Betel leaf (*Sun*), Barley / Ghee / Jaggery (*Fri*).
  - **Monday, Saturday:** East (*கிழக்கு*) -> Remedy: Milk / Curd (*Mon*), Urad dal / Sesame / Ginger (*Sat*).
  - **Tuesday, Wednesday:** North (*வடக்கு*) -> Remedy: Jaggery / Milk (*Tue*), Milk / Coriander / Sesame (*Wed*).
  - **Thursday:** South (*தெற்கு*) -> Remedy: Curd / Cumin.
- Validate exceptions: Soolam applies only to the initial departure vector (*Prasthana*) for out-of-town travel from home base, never to return journeys home or intra-city commutes.

### B. Subakariyam, Muhurtham, and Dynamic Time Windows
When validating a date/time for an auspicious event, verify all six gates in order using local $\Delta T$:

1. **Kari Naal Blackout Check:** Verify the Tamil solar date is not a *Kari Naal* (*Chithirai* 6, 15; *Vaikasi* 7, 16, 17; *Aani* 1, 6; *Aadi* 2, 10, 20; *Aavani* 2, 9, 28; *Purattasi* 16, 29; *Aippasi* 6, 20; *Karthigai* 1, 10, 17; *Margazhi* 6, 9, 11; *Thai* 1, 2, 3, 11, 17; *Maasi* 15, 16, 17; *Panguni* 6, 15, 19).
2. **Paksham and Tithi Check:** Confirm *Valarpirai* (for major starts) and exclude *Ashtami* (8), *Navami* (9), *Amavasya* (30), *Pournami* (15, for weddings), and *Rikta Tithis* (4, 9, 14).
   - **Krishna Paksha Muhurtham (Ksheena Chandra):** Major ceremonies (marriages, formal housewarmings) are restricted to the first half of the waning fortnight, Krishna *Prathama* to *Panchami* (Krishna tithi 1–5), with *Saptami* and *Dashami* allowed only where the rule is confirmed (open question). Other Krishna tithis are rejected for major beginnings.
   - Worked check (corrected): Sun $120^\circ$, Moon $300^\circ$ gives $\Delta\lambda = 180^\circ$, which is Pournami. Sun $100^\circ$, Moon $310^\circ$ gives $\Delta\lambda = 210^\circ$ and block $\lfloor 210/12 \rfloor + 1 = 18$, which is Krishna Tritiya (block 16 is Prathama). Krishna Tritiya is inside Prathama to Panchami, so the verdict is `PASS (conditional)`: it also needs good Chandra Balam and Lagna Shuddhi. The earlier Navami example (Sun $120^\circ$, Moon $225^\circ$, $\Delta\lambda = 105^\circ$) is Shukla Navami, not Krishna Ashtami.
floor + 1 = 9$, which is **Shukla Navami** (not Krishna Ashtami as first written). Navami is excluded by gate 2, so the verdict is still `FAIL`, but for the Navami reason.
3. **Vaaram and Marana Yogam Check:** Favor Mon, Wed, Thu, Fri. Tuesday and Saturday are flagged, not blacked out (see §E). Verify the *Vaara + Nakshatra* pair does not trigger *Marana Yogam* (a blackout for all auspicious events; the classical lookup table is required and is still open).
4. **Personal Compatibility (when birth star is provided):**
   - **Chandrashtamam:** Confirm transiting Moon Rasi is not the 8th Rasi from *Janma Rasi*.
   - **Thara Balam:** Compute $\text{Count} = ((\text{DayStar} - \text{BirthStar}) \pmod{27}) + 1$, then $\text{Thara} = \text{Count} \pmod{9}$ (with $0 \to 9$). Valid Tharas: `2` (*Sampath*), `4` (*Kshema*), `6` (*Sadhaka*), `8` (*Mitra*), `9` (*Parama Mitra*). Reject `1, 3, 5, 7`.
5. **Dynamic 8-Segment Inauspicious Windows:** Divide local daytime $\Delta T = T_{\text{set}} - T_{\text{rise}}$ into 8 equal segments ($E = \Delta T / 8$). Never use fixed 90-minute blocks:
   - Sun: Rahu = 8th, Yama = 5th, Kuligai = 7th
   - Mon: Rahu = 2nd, Yama = 4th, Kuligai = 6th
   - Tue: Rahu = 7th, Yama = 3rd, Kuligai = 5th
   - Wed: Rahu = 5th, Yama = 2nd, Kuligai = 4th
   - Thu: Rahu = 6th, Yama = 1st, Kuligai = 3rd
   - Fri: Rahu = 4th, Yama = 7th, Kuligai = 2nd
   - Sat: Rahu = 3rd, Yama = 6th, Kuligai = 1st
   - Note: *Kuligai* is avoided for funerals/endings (due to repetition) but is valid for recurring auspicious gains (savings, gold, business).
6. **Planetary Hora, Solar Noon, and Lagna Shuddhi:**
   - **Daytime Hora:** Divide $\Delta T$ into 12 equal segments ($\Delta T / 12$); **Nighttime Hora:** Divide $\Delta T_{\text{night}}$ into 12 equal segments ($\Delta T_{\text{night}} / 12$).
   - **Abhijit Muhurtham:** Centered on true local solar noon ($T_{\text{noon}} = T_{\text{rise}} + \Delta T / 2$), spanning $T_{\text{noon}} \pm \Delta T / 30$. Overrides minor Panchangam flaws except on Wednesdays.
   - **Hora (detail):** The first Hora of a weekday belongs to that weekday's lord, then the Chaldean sequence Sun → Venus → Mercury → Moon → Saturn → Jupiter → Mars repeats. Benefic Horas (Jupiter, Venus, Mercury, Moon) suit financial dealings, agreements, travel and new work. Malefic Horas (Sun, Mars, Saturn) are kept for enforcement, physical labour and competitive filings, not delicate beginnings.
   - **Lagna Shuddhi (detail):** The rising sign at the Muhurtham must be free of structural flaws: (a) the 8th house from the Muhurtham Lagna is unoccupied by Sun, Mars, Saturn, Rahu or Ketu; (b) benefics (Jupiter, Venus, Mercury, well-placed Moon) sit in Kendra (1, 4, 7, 10) or Trikona (1, 5, 9), and malefics in 3, 6, 11; (c) the Lagna lord is strong, not combust, and not in Dusthana (6, 8, 12). These need planetary house positions, which the app does not compute yet.

### C. Sraddha, Amavasai, and Mahalaya Rules
Never use *Udaya Tithi* (sunrise Tithi) or hardcoded IST afternoon times to validate ancestral rites. Apply the **Local 5-Division Dinamana Rule**:

1. Divide local daytime $\Delta T = T_{\text{set}} - T_{\text{rise}}$ into 5 equal parts ($U = \Delta T / 5$).
2. **Local Aparahna Kala** is the **4th fifth**: $[T_{\text{rise}} + 3U,\; T_{\text{rise}} + 4U]$.
3. **Sraddha Date Selection:** Select the local calendar date where the target *Tithi* covers the majority of local *Aparahna Kala* (if equal across two days, select the second day, *Paraviddha*).
4. **Darsha (Monthly Amavasai) Rule:** Check local *Madhyahna* (3rd fifth: $[T_{\text{rise}} + 2U,\; T_{\text{rise}} + 3U]$) and *Aparahna* overlap; if Amavasya covers local noon on Day 1 and ends before local noon on Day 2, Day 1 is valid.
5. **Mahalaya Specifics:** Flag *Ghata Chaturdashi* (14th Tithi) as restricted strictly to unnatural/accidental deaths; direct natural-death annual Mahalaya rites to *Maha Bharani*, *Madhyashtami*, *Avidhava Navami*, the ancestor's death Tithi, or *Mahalaya Amavasai*.
6. **Overrides:** Annual Sraddha is never postponed for *Kari Naal*, *Marana Yogam*, Tuesday, Saturday, or Rahu Kalam overlap—only for *Asaucham* (birth/death impurity) or an active *Grahanam* (eclipse).

### D. Nakshatram and Yogam Quality
- **Nakshatram quality:** Preferred *Dhruva* (fixed): Rohini, Uttara Phalguni, Uttara Ashadha, Uttara Bhadrapada. *Mridu* (soft/friendly): Mrigashira, Chitra, Anuradha, Revati. Avoided for foundations: *Adhomukha* (downward-facing): Bharani, Krittika, Ashlesha, Magha, Purva Phalguni, Vishakha, Mula, Purva Ashadha, Purva Bhadrapada.
- **Yogam:** $\lfloor [(\lambda_{\text{Sun}} + \lambda_{\text{Moon}}) \bmod 360^\circ] / 13^\circ 20' 
floor + 1$ (1 to 27). Strictly avoided malefic yogas (1-based numbers): Vishkambha (1), Ganda (6), Atiganda (7), Shula (9), Vyaghata (13), Vajra (15), Vyatipata (17), Parigha (19), Vaidhriti (27).

### E. Tuesday and Saturday
- Not blackouts for every activity. Both are *flagged* for fiery (Mars) or heavy (Saturn) energy.
- Permitted: Saturday for mechanical maintenance, iron and steel work, legal defence and structural groundwork; Tuesday for surgery, engineering tests and dispute resolution.
- Excluded for marriages, formal housewarmings and peaceful consumer ventures, unless a strong benefic Lagna counterbalances it.

### F. Thara Balam and Chandrashtamam (personal)
- Count inclusively from the Janma Nakshatra to the Muhurtham Nakshatra: $\text{Count} = N_{\text{Muhurtha}} - N_{\text{Janma}} + 1$, wrapping around 27 when zero or negative. Take $\text{Count} \bmod 9$, with remainder 0 read as 9.
- Pass: 2 (Sampath), 4 (Kshema), 6 (Sadhaka), 8 (Mitra), 9 (Parama Mitra). Fail: 1 (Janma), 3 (Vipat), 5 (Pratyak), 7 (Naidhana).
- Chandrashtamam: the transiting Moon must not be in the 8th Rasi counted from the natal Moon sign. A Muhurtham on such a day is disqualified.
- Both need the participant's birth star and birth Rasi as inputs.

## Step 4: Output Format for Validation Reports

When validating a date, calculation, or Panchangam dataset, structure the response clearly:

1. **Astronomical Baseline:** State exact city/coordinates, IANA timezone, local $T_{\text{rise}}$, $T_{\text{set}}$, $\Delta T$, active *Vaara*, Ayanamsa, and sidereal Sun/Moon longitudes.
2. **Computed Panchangam Matrix:** Present the 5 limbs (*Vaaram*, *Tithi*, *Nakshatra*, *Yogam*, *Karanam*) with exact transition timestamps in local time.
3. **Rule Audit & Verdict:** Explicitly mark each requested check (`PASS`, `FAIL`, or `CONDITIONAL WITH PARIGARAM`), show the step-by-step location-specific arithmetic (such as *Aparahna Kala* bounds, *Thara Balam* modulo, or 1/8th Rahu Kalam intervals), and state any applicable overrides.

## Gotchas

- **Location & High-Latitude Seasonal Skew:** Outside India (e.g., North America or Europe), $\Delta T$ varies dramatically between winter (~9 hours) and summer (~15 hours), shifting Rahu Kalam duration from ~68 min to ~112 min and shifting *Aparahna Kala* by hours. Never reuse Indian printed Panchangam clock times for another country or timezone.
- **Midnight vs. Sunrise Trap:** Always verify whether an early-morning timestamp (e.g., 4:30 AM) precedes local sunrise; if so, decrement the weekday index by 1 before computing *Soolam*, *Rahu Kalam*, *Hora*, or *Marana Yogam*.
- **International Date Line & Western Hemisphere Lunar Shifts:** Because Moon longitude is geocentric/topocentric at a global instant (UTC), a Tithi or Amavasya that falls on Tuesday in Chennai frequently falls on Monday in North America. Always evaluate Tithi overlap against the **local** date's $T_{\text{rise}}$ and *Aparahna Kala*, never by copying the Indian calendar date.

## Resolved Conflicts (audit by astronomer-validator reviewer)

- **Malefic yogas (1-based):** 1 Vishkambha, 6 Atiganda, 9 Shula, 10 Ganda, 13 Vyaghata, 15 Vajra, 17 Vyatipata, 19 Parigha, 27 Vaidhriti. Sukarma (7) is auspicious. Whole-yoga rejection is the conservative reading. Partial-ghati (Tyajya ghati) rules are open.
- **Magha and Moola:** permitted for marriage (Vivaha). Adhomukha applies to construction and excavation filters only: raising upper structures is avoided, and digging foundations is favoured.

Both are the reviewer's classical attributions and are not independently verified here. Astronomer confirmation is still pending.
