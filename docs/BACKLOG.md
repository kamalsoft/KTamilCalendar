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

1. **Krishna Paksha Muhurtham:** done. Only Krishna Prathama to Panchami (tithi 1–5) is used. Open: what "select Saptami/Dashami" means.
2. **Hora:** done (engine 11). Remaining: none.
3. **Lagna Shuddhi:** 8th-house gate done (engine 12). Open: benefic placement in Kendra and Trikona, malefic placement, and Lagna lord strength. These need Mercury and Venus.
4. **Marana Yogam** (skill gate 3): needs the classical weekday and Nakshatra lookup table, which is not yet supplied.
5. **Thara Balam:** done (Settings: Birth details). **Personal Chandrashtamam:** done, with offset 5 (see item 21).
6. **Sraardha tie rule:** done (engine 11). The second day takes an exact-half tithi.
7. **Darsha and Mahalaya rules** (skill section C, points 4–5): not implemented. Darsha: Amavasai over Madhyahna on day 1. Mahalaya: Ghata Chaturdashi limited to unnatural deaths.
8. **Tuesday and Saturday** (skill section E): the Subha Muhurtham tag excludes them (marriage rule). The day view does not yet flag them with their permitted uses (Saturday for maintenance and legal defence; Tuesday for surgery and disputes).
9. **Nakshatram and yogam** are checked against Swiss Ephemeris for Naperville only. The revalidation sample was Chennai, and the audit should run for each place a user can select.
10. **Default location differs by platform.** The APK falls back to Naperville (`AppContainer`), and the web app defaults to Chennai (`store.js`). Every calculation uses the selected location, so the defaults only matter before a place is chosen. Decide whether both should use the same default.
11. **Revalidation sample:** the November 2026 Chennai audit used Chennai as a sample place only. Rerun it for the place the user has selected.
12. **Nakshatram quality and malefic yogas** (skill section D): defined, not used by the app.
13. **Yogam and Nakshatram quality in the day view:** decide whether to display them.
14. **Magha and Moola:** resolved by the audit. Permitted for marriage; Adhomukha applies only to construction and excavation filters, which the app does not have. Astronomer confirmation pending.
15. **Malefic yogas:** implemented as audited (1, 6, 9, 10, 13, 15, 17, 19, 27). Open: partial-ghati rules (Tyajya ghatis) for the yogas that are not full-day blacklists.
16. **Tuesday and Saturday day-view flags** (skill section E): permitted uses are not shown on the day view yet.
17. **Nakshatram quality** (skill section D): Dhruva, Mridu and Adhomukha are not displayed.
18. **Lagna Shuddhi** placement and Lagna lord strength: need Mercury and Venus positions (8th-house gate done).
19. **Marana Yogam** (skill gate 3): needs the classical weekday and Nakshatra lookup table.
20. **Thara Balam and personal Chandrashtamam** (skill gate 4): done, see item 5 and item 21.
21. **Chandrashtamam offset (decision):** the engine and the personal check use the Moon's Rasi plus 5. The validator's inclusive count, with the Moon as the 1st sign, gives plus 7. For 16 Nov 2026 (Moon in Magaram), plus 5 gives Mithunam and plus 7 gives Simmam. Confirm which is correct before changing the day-level card.
22. **Marana Yogam table:** needs a verifiable classical source. The pasted matrix is not used until it is checked.
23. **Darsha:** done (engine 13). **Tuesday and Saturday badge:** done on the web Today tab. Open: the APK day view.
24. **Lagna placement scoring** (Kendra and Trikona, Lagna lord strength): the pasted note assumes Mercury and Venus are in the pipeline. They are not yet. The scoring thresholds (combustion, debilitation) need a source before they are built.
25. **Chandrashtamam offset:** the pasted note keeps +5 and cites Aries to Scorpio as an example. Aries (0) to Scorpio (7) is +7, and its star example (Ashwini to Anuradha) is also wrong (the 8th star is Pushya). Still an open decision (item 21).
