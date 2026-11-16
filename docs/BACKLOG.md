# Priority backlog

## Done (uncommitted in this round)

1. **Sraardha Thithi**: the tithi that is the same at 13:30 and 16:00 local, Amavasai excluded. Source: Vande Guru Paramparaam post. Verified against Swiss Ephemeris for 365 days (0 mismatches).
2. **Soolam**: the weekday table, with remedies from the Tamil almanac source. Matches the prokerala page for Monday, Saturday and Sunday.
3. **Parigaram**: weekday remedy and Abhijit window, in the Soolam card.
4. **Subakariyam (day level)**: Kari Naal days, and the Karanam (with Vishti flag). Shown on Today and in the APK day screen.
5. **Naal**: Karanam, Kari Naal and the Sraardha tithi, shown on Today and in the APK day screen.
6. **Duplicate school days (web Festivals)**: one school closure per day, whatever the district calls it. The APK Festivals screen is not changed yet.

## Open, in priority order

7. **Month grid reference**. The FlipHTML5 flipbook could not be read. Needs a screenshot of one grid page to match. Next in line after the items above.

## Follow-ups (not blocking)

- **Sraardha window.** The post's fixed 1:30–4:00 pm is used, as approved. The later Sraardha source instead computes Aparaana Kaalam as the 4th fifth of the day (from sunrise and sunset). The source's own example (sunrise 6:55 am, sunset 4:25 pm) gives 12:37–2:31 pm. Switch the window if you want that rule.
- **Sraardha coverage.** A tithi covers the 13:30–16:00 window on about 87% of days, so the card shows a tithi on most days. Rules for Mahalaya and Amavasai are not implemented.
- **Subakariyam steps 5–6.** Thara Balam and Chandrashtamam need the user's birth star, and Lagna Shuddhi needs a window search. Both need a settings input before they can be built.
- **Karanam Tamil spellings** and the **Kari Naal table** come from the pasted sources and should be checked against a printed Panchangam.
- **APK Festivals** school dedupe, to match the web.

## Applied from the astronomer-validator skill

- Good windows never overlap an avoid window. Rahu Kalam, Yamagandam and Kuligai are cut out of Nalla Neram and Gowri.
- Avoid windows are the eight equal parts of the local daytime at every place. The India 6 AM block is removed, so the
  timing-convention setting no longer changes windows. The setting should be removed from the settings screen.
- Sraardha tithi uses Aparahna Kala (the 4th fifth of the daytime) with a majority rule, replacing the fixed 13:30–16:00 window.
- Abhijit is solar noon ± daytime/30.
- Muhurtham stars and best tithis follow the validator's lists.

## Still open for the astronomer

- Krishna Paksha Muhurtham tithis: not in the validator's sources.
- Thara Balam, the personal Chandrashtamam check, Marana Yogam and Lagna Shuddhi: not implemented.
- Gowri windows: the day-part cycle is not covered by the validator's sources.

## Revalidation against the validator skill (open)

1. **Krishna Paksha Muhurtham (decision needed).** The app tags Krishna tithi days as Subha Muhurtham (10 Chennai and 9 Naperville days in 2026). The skill says Valarpirai for major starts. Decide: restrict to Valarpirai, or keep and label as minor.
2. **Hora** (skill gate 6): daytime and nighttime Hora are not computed.
3. **Lagna Shuddhi** (skill gate 6): the 8th house from the Muhurtham Lagna is not checked.
4. **Marana Yogam** (skill gate 3): the Vaara and Nakshatra pair is not checked.
5. **Thara Balam and personal Chandrashtamam** (skill gate 4): need the user's birth star as a setting.
6. **Sraardha tie rule** (edge case): when a tithi covers exactly half of Aparahna Kala on two consecutive days, the skill picks the second day. The app gives no tithi on either day.
7. **Darsha and Mahalaya rules** (skill section C, points 4–5): not implemented.
8. **Tuesday and Saturday** (skill gate 3): the app excludes them outright; the skill says flag them.
9. **Nakshatram and yogam** are checked against Swiss Ephemeris for Naperville only. The revalidation sample was Chennai, and the audit should run for each place a user can select.
10. **Default location differs by platform.** The APK falls back to Naperville (`AppContainer`), and the web app defaults to Chennai (`store.js`). Every calculation uses the selected location, so the defaults only matter before a place is chosen. Decide whether both should use the same default.
11. **Revalidation sample:** the November 2026 Chennai audit used Chennai as a sample place only. Rerun it for the place the user has selected.
