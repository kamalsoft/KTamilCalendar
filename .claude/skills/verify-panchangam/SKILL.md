---
name: verify-panchangam
description: Check the panchangam engine's accuracy against the Swiss Ephemeris (tithi, nakshatram, yogam, Tamil date, sunrise/sunset). Use after any change to core/astro or when accuracy is questioned.
---

# Verify panchangam accuracy

```bash
python3 -m venv <scratchpad>/venv && <scratchpad>/venv/bin/pip install -q pyswisseph
./gradlew :app:testDebugUnitTest --tests '*PanchangamExportTest' -DexportPath=<scratchpad>/engine.csv
<scratchpad>/venv/bin/python -I tools/verify_panchangam.py <scratchpad>/engine.csv
```
Expect: 0 mismatches for tithi/nakshatram/yogam/Tamil month; end-time |Δ| ≤ ~1.5 min; sunrise/sunset ≤ ~1.3 min.
The script's LAT/LON/TZ are Naperville — keep them in sync with `GeoLocation.NAPERVILLE`.
For a spot check against a published source, Drik Panchang's day page for Naperville is `geoname-id=4903279`.
