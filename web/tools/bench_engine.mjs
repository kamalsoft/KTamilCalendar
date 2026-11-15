// Timing of the in-browser calculations, for the screens that compute most days at once.
import { PLACES } from '../src/main/resources/web/engine/locations.js';
import { monthDays } from '../src/main/resources/web/engine/calendar.js';
import { parseIso } from '../src/main/resources/web/engine/civil.js';
import { readFileSync } from 'node:fs';
const school = JSON.parse(readFileSync('app/src/main/assets/school_calendars.json', 'utf8'));
const place = PLACES[0];
const start = parseIso('2026-11-01');
for (const [label, n] of [['one month (42 days)', 42], ['120 days (festivals tab)', 120]]) {
  const t0 = performance.now();
  for (let from = start; from < start + n; from += 42) monthDays(place, from, Math.min(start + n - 1, from + 41), { schoolJson: school });
  console.log(`${label}: ${Math.round(performance.now() - t0)} ms`);
}
