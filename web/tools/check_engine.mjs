// Parity check: the JavaScript engine must reproduce the Kotlin engine's month files, field by field.
// Reference files come from `./gradlew :web:referenceData` (web/build/reference/data/<place>/<yyyy-MM>.json).
import { readFileSync } from 'node:fs';
import { TamilCalendarEngine } from '../src/main/resources/web/engine/engine.js';
import { PLACES, localHolidayRegions } from '../src/main/resources/web/engine/locations.js';
import { parseIso, dayOfWeek } from '../src/main/resources/web/engine/civil.js';
import { monthDays, importantDates } from '../src/main/resources/web/engine/calendar.js';

const [placeKey, month] = process.argv.slice(2);
const place = PLACES.find((p) => p.key === placeKey);
const ref = JSON.parse(readFileSync(`web/build/reference/data/${placeKey}/${month}.json`, 'utf8'));
const [y, m] = month.split('-').map(Number);
const first = parseIso(`${month}-01`);
const last = parseIso(`${y}-${String(m).padStart(2, '0')}-${new Date(Date.UTC(y, m, 0)).getUTCDate()}`);
const from = first - dayOfWeek(first); // Sunday before the 1st
const to = last + (6 - dayOfWeek(last)); // Saturday after the last day

const schoolJson = JSON.parse(readFileSync('app/src/main/assets/school_calendars.json', 'utf8'));
const days = monthDays(place, from, to, { schoolJson });
const problems = [];
const check = (date, what, got, want) => {
  if (JSON.stringify(got) !== JSON.stringify(want)) problems.push(`${date} ${what}: js=${JSON.stringify(got)} kt=${JSON.stringify(want)}`);
};
const hm = (s) => s.slice(0, 5);
for (const r of ref.days) {
  const d = days.find((x) => x.date === r.date);
  if (!d) { problems.push(`${r.date} missing`); continue; }
  check(r.date, 'tithi.en', d.panchangam.tithi.nameEn, r.tithi.name.en);
  check(r.date, 'tithi.endsAt', d.panchangam.tithi.endsAt && d.panchangam.tithi.endsAt.slice(0, 16), r.tithi.endsAt && r.tithi.endsAt.slice(0, 16));
  check(r.date, 'nakshatram', d.panchangam.nakshatram.nameEn, r.nakshatram.name.en);
  check(r.date, 'nakshatram.endsAt', d.panchangam.nakshatram.endsAt && d.panchangam.nakshatram.endsAt.slice(0, 16), r.nakshatram.endsAt && r.nakshatram.endsAt.slice(0, 16));
  check(r.date, 'yogam', d.panchangam.yogam.nameEn, r.yogam.name.en);
  check(r.date, 'yogam.endsAt', d.panchangam.yogam.endsAt && d.panchangam.yogam.endsAt.slice(0, 16), r.yogam.endsAt && r.yogam.endsAt.slice(0, 16));
  check(r.date, 'tamilDay', d.tamilDate.day, r.tamilDate.day);
  check(r.date, 'tamilMonth', d.tamilDate.monthIndex, ['Chithirai','Vaikasi','Aani','Aadi','Aavani','Purattasi','Aippasi','Karthigai','Margazhi','Thai','Maasi','Panguni'].indexOf(r.tamilDate.month.en));
  check(r.date, 'sunrise', hm(d.sunrise), hm(r.sunrise));
  check(r.date, 'sunset', hm(d.sunset), hm(r.sunset));
  check(r.date, 'moonRasi', d.panchangam.moonRasi, ['Mesham','Rishabam','Mithunam','Kadagam','Simmam','Kanni','Thulam','Viruchigam','Dhanusu','Magaram','Kumbam','Meenam'].indexOf(r.moonRasi.en));
  check(r.date, 'timings', d.timings.nallaNeram.concat([d.timings.rahuKalam, d.timings.yamagandam, d.timings.kuligai]).map((t) => t.start.slice(0, 5) + '-' + t.end.slice(0, 5)).sort(),
    r.timings.filter((t) => t.type.en !== 'Gowri Nalla Neram').map((t) => hm(t.start) + '-' + hm(t.end)).sort());
  check(r.date, 'gowriNallaNeram', d.timings.gowriNallaNeram.map((t) => t.start.slice(0, 5) + '-' + t.end.slice(0, 5)),
    r.timings.filter((t) => t.type.en === 'Gowri Nalla Neram').map((t) => hm(t.start) + '-' + hm(t.end)));
  check(r.date, 'chandrashtamam.nakshatras', d.chandrashtamam.nakshatras.length, r.chandrashtamam.nakshatras.split(', ').filter(Boolean).length);
  check(r.date, 'holiday', d.holiday, r.holiday);
  check(r.date, 'karanam', d.panchangam.karanam, r.karanam);
  check(r.date, 'sraardhaTithi', d.panchangam.sraardhaTithi ?? -1, r.sraardhaTithi);
  check(r.date, 'lagnam.rasi', d.panchangam.lagnam.map((s) => s.rasi), r.lagnam.map((s) => s.rasi));
  check(r.date, 'lagnam.start', d.panchangam.lagnam.map((s) => s.start.slice(0, 16)), r.lagnam.map((s) => s.start.slice(0, 16)));
  const js = importantDates(d).map((x) => x.titleEn);
  const kt = r.importantDates.map((x) => x.title.en);
  if (JSON.stringify(js) !== JSON.stringify(kt)) problems.push(`${r.date} importantDates: js=${JSON.stringify(js)} kt=${JSON.stringify(kt)}`);
}
console.log(`${placeKey} ${month}: ${ref.days.length} days, ${problems.length} differences`);
for (const p of problems.slice(0, 40)) console.log('  ' + p);
