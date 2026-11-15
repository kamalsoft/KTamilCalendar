// Port of the data layer's composition (CalendarRepositoryImpl + Mappers) and TamilDayData's derived values.
import { TamilCalendarEngine } from './engine.js';
import { localHolidayRegions, schoolDistricts } from './locations.js';
import { epochDay, parseIso, isoOf } from './civil.js';
import { usHolidays, worldReligionDays } from './holidays.js';
import { CONTENT } from './content.js';

const DAY_TAG_ORDER = CONTENT.enums.DayTag.map((t) => t[0]);

/**
 * Days from [from] to [to] (epoch days) for [place], with the rule-based holidays merged in, the way the
 * Android repository stores them. Each day gets `holiday` (a local day off) and tags with GOVT_HOLIDAY added.
 */
export function monthDays(place, from, to, { convention, schoolJson } = {}) {
  const regions = localHolidayRegions(place);
  const civil = [
    ...usHolidays(from, to, regions),
    ...worldReligionDays(from, to),
    ...schoolEvents(schoolJson, schoolDistricts(place), from, to),
  ];
  const byDate = new Map();
  for (const ev of civil) {
    if (!byDate.has(ev.date)) byDate.set(ev.date, []);
    byDate.get(ev.date).push(ev);
  }
  const engine = new TamilCalendarEngine(place, convention);
  return engine.compute(from, to).map((day) => {
    const events = [...day.events, ...(byDate.get(day.date) || [])].map((e) => ({
      ...e,
      isLocalHoliday: e.isHoliday && e.kind !== 'SCHOOL' && regions.has(e.region),
    }));
    const holiday = events.some((e) => e.isLocalHoliday);
    const tags = holiday && !day.tags.includes('GOVT_HOLIDAY') ? [...day.tags, 'GOVT_HOLIDAY'] : day.tags;
    return { ...day, events, holiday, tags };
  });
}

/**
 * Bundled school calendars (school_calendars.json): one event per day of each entry, for the districts in
 * [districtRegions]. Mirrors data/local/SchoolCalendars.kt.
 */
export function schoolEvents(json, districtRegions, from, to) {
  if (!json || !districtRegions.size) return [];
  const out = [];
  for (const district of json.districts || []) {
    if (!districtRegions.has(district.region)) continue;
    for (const day of district.days || []) {
      if (!day.from) continue;
      const start = parseIso(day.from);
      const end = day.to ? parseIso(day.to) : start;
      for (let d = start; d <= end; d++) {
        if (d < from || d > to) continue;
        out.push({
          date: isoOf(d), kind: 'SCHOOL', titleTa: day.ta || day.en || '', titleEn: day.en || day.ta || '',
          isHoliday: !!day.noSchool, source: 'BUNDLED', detail: null, region: district.region,
          religion: null, deity: null,
        });
      }
    }
  }
  return out;
}

/** Whether an event is shown under the user's settings (port of AppSettings.isVisible). */
export function isVisible(e, s) {
  if (e.religion && e.religion !== 'HINDU' && s.hiddenFaiths.includes(e.religion) && !e.isLocalHoliday) return false;
  switch (e.region) {
    case 'US_FEDERAL': case 'ILLINOIS': return s.usHolidays;
    case 'SCHOOL_D203': case 'SCHOOL_D204': return s.schoolCalendars;
    case 'TAMIL_NADU': return s.tamilNaduHolidays || e.kind !== 'GOVT_HOLIDAY';
    default: return true;
  }
}

/** The day with events filtered by the settings, and holiday/school tags recomputed (port of applying()). */
export function applying(day, s) {
  const kept = day.events.filter((e) => isVisible(e, s));
  if (kept.length === day.events.length) return day;
  let tags = day.tags.filter((t) => t !== 'GOVT_HOLIDAY' || kept.some((e) => e.isLocalHoliday));
  tags = tags.filter((t) => t !== 'SCHOOL_HOLIDAY' || kept.some((e) => e.kind === 'SCHOOL' && e.isHoliday));
  return { ...day, events: kept, tags, holiday: kept.some((e) => e.isLocalHoliday) };
}

/** The day's important dates, most important first (port of TamilDayData.importantDates). */
export function importantDates(day) {
  const listed = day.events.filter((e) => e.kind !== 'SCHOOL' && e.kind !== 'MUHURTHAM');
  const out = [];
  const entry = (titleTa, titleEn, extra = {}) => ({ title: { ta: titleTa, en: titleEn }, titleEn, ...extra });
  listed.filter((e) => e.isLocalHoliday).forEach((e) => out.push(entry(e.titleTa, e.titleEn, { holiday: true, religion: e.religion, deity: e.deity })));
  listed.filter((e) => !e.isLocalHoliday && e.religion === 'HINDU').forEach((e) => out.push(entry(e.titleTa, e.titleEn, { holiday: false, religion: 'HINDU', deity: e.deity })));
  const tagSet = new Set(day.tags);
  DAY_TAG_ORDER.filter((id) => tagSet.has(id)).forEach((id) => {
    const t = CONTENT.enums.DayTag.find((x) => x[0] === id);
    if (t[3] && id !== 'POURNAMI') out.push(entry(t[1], t[2], { holiday: false, tag: id, deity: t[3] }));
  });
  ['AMAVASAI', 'POURNAMI'].filter((id) => tagSet.has(id)).forEach((id) => {
    const t = CONTENT.enums.DayTag.find((x) => x[0] === id);
    out.push(entry(t[1], t[2], { holiday: false, tag: id, deity: t[3] }));
  });
  listed.filter((e) => !e.isLocalHoliday && e.religion && e.religion !== 'HINDU').forEach((e) => out.push(entry(e.titleTa, e.titleEn, { holiday: false, religion: e.religion })));
  listed.filter((e) => !e.isLocalHoliday && !e.religion).forEach((e) => out.push(entry(e.titleTa, e.titleEn, { holiday: false })));
  const seen = new Set();
  return out.filter((x) => (seen.has(x.titleEn) ? false : (seen.add(x.titleEn), true)));
}
