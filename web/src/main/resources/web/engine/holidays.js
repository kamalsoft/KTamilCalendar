// Port of core/holidays: US/Illinois rules, Christian, Jewish (Hebrew calendar) and Islamic (Hijri) dates.
import * as A from './astro.js';
import { epochDay, dayOfWeek, dayOfMonth, monthOf, yearOf, fromEpochDay, isoOf, MS_DAY } from './civil.js';
import { CONTENT } from './content.js';

const RD_EPOCH_DAY = 719163; // RD of 1970-01-01
const HEBREW_EPOCH = -1373427;
const ISLAMIC_EPOCH = 227015;
const MECCA = { lat: 21.4225, lon: 39.8262 };
const mod = (a, n) => ((a % n) + n) % n;
const fdiv = (a, n) => Math.floor(a / n);

/** Gregorian Easter (anonymous computus), as an epoch day. Same algorithm as the APK's engine. */
export function easter(year) {
  const a = year % 19, b = fdiv(year, 100), c = year % 100;
  const d = fdiv(b, 4), e = b % 4, f = fdiv(b + 8, 25), g = fdiv(b - f + 1, 3);
  const h = mod(19 * a + b - d - g + 15, 30);
  const i = fdiv(c, 4), k = c % 4;
  const l = mod(32 + 2 * e + 2 * i - h - k, 7);
  const m = fdiv(a + 11 * h + 22 * l, 451);
  const month = fdiv(h + l - 7 * m + 114, 31);
  const day = ((h + l - 7 * m + 114) % 31) + 1;
  return epochDay(year, month, day);
}

/** Orthodox Easter: Julian computus converted to the Gregorian calendar (+13 days for 1900–2099). */
function orthodoxEaster(year) {
  const a = year % 4, b = year % 7, c = year % 19;
  const d = mod(19 * c + 15, 30);
  const e = mod(2 * a + 4 * b - d + 34, 7);
  const month = fdiv(d + e + 114, 31);
  const day = ((d + e + 114) % 31) + 1;
  return epochDay(year, month, day) + 13;
}

const event = (day, ta, en, religion, detail = null) => ({
  date: isoOf(day), kind: 'RELIGIOUS', titleTa: ta, titleEn: en, isHoliday: false, source: 'COMPUTED',
  detail, region: null, religion, deity: null,
});

// ---------------------------------------------------------------- US / Illinois (rule based, any year)
/** First weekday [dow] (0 = Sunday) on or after the 1st of the month, plus n-1 weeks. */
function nth(year, month, dow, n) {
  const first = epochDay(year, month, 1);
  return first + mod(dow - dayOfWeek(first), 7) + 7 * (n - 1);
}
function last(year, month, dow) {
  const nextFirst = month === 12 ? epochDay(year + 1, 1, 1) : epochDay(year, month + 1, 1);
  const lastDay = nextFirst - 1;
  return lastDay - mod(dayOfWeek(lastDay) - dow, 7);
}
const thanksgiving = (y) => nth(y, 11, 4, 4);
const US_RULES = [
  ['US_FEDERAL', 'ஆங்கிலப் புத்தாண்டு', "New Year's Day", true, null, (y) => epochDay(y, 1, 1)],
  ['US_FEDERAL', 'மார்ட்டின் லூதர் கிங் ஜூனியர் தினம்', 'Martin Luther King Jr. Day', false, null, (y) => nth(y, 1, 1, 3)],
  ['US_FEDERAL', 'ஜனாதிபதிகள் தினம்', "Presidents' Day", false, null, (y) => nth(y, 2, 1, 3)],
  ['US_FEDERAL', 'நினைவு தினம்', 'Memorial Day', false, null, (y) => last(y, 5, 1)],
  ['US_FEDERAL', 'ஜூன்டீன்த்', 'Juneteenth', true, null, (y) => (y >= 2021 ? epochDay(y, 6, 19) : null)],
  ['US_FEDERAL', 'அமெரிக்க சுதந்திர தினம்', 'Independence Day (US)', true, null, (y) => epochDay(y, 7, 4)],
  ['US_FEDERAL', 'தொழிலாளர் தினம்', 'Labor Day', false, null, (y) => nth(y, 9, 1, 1)],
  ['US_FEDERAL', 'கொலம்பஸ் / பூர்வகுடி மக்கள் தினம்', "Columbus / Indigenous Peoples' Day", false, null, (y) => nth(y, 10, 1, 2)],
  ['US_FEDERAL', 'படைவீரர் தினம்', 'Veterans Day', true, null, (y) => epochDay(y, 11, 11)],
  ['US_FEDERAL', 'நன்றி தெரிவிக்கும் நாள்', 'Thanksgiving', false, null, thanksgiving],
  ['US_FEDERAL', 'கிறிஸ்துமஸ்', 'Christmas', true, 'CHRISTIAN', (y) => epochDay(y, 12, 25)],
  ['ILLINOIS', 'லிங்கன் பிறந்த நாள்', "Lincoln's Birthday", true, null, (y) => epochDay(y, 2, 12)],
  ['ILLINOIS', 'நன்றி நாளுக்கு அடுத்த நாள்', 'Day after Thanksgiving', false, null, (y) => thanksgiving(y) + 1],
  ['ILLINOIS', 'பொதுத் தேர்தல் நாள்', 'General Election Day', false, null, (y) => (y % 2 === 0 ? nth(y, 11, 1, 1) + 1 : null)],
];

/** US federal and Illinois days off between [from] and [to] (epoch days), weekend-observed days included. */
export function usHolidays(from, to, regions) {
  const out = [];
  for (let y = yearOf(from) - 1; y <= yearOf(to) + 1; y++) {
    for (const [region, ta, en, shifts, religion, dateIn] of US_RULES) {
      if (!regions.has(region)) continue;
      const date = dateIn(y);
      if (date === null || date === undefined) continue;
      const add = (d, observed) => {
        if (d < from || d > to) return;
        out.push({
          date: isoOf(d), kind: 'GOVT_HOLIDAY',
          titleTa: observed ? `${ta} (விடுமுறை)` : ta, titleEn: observed ? `${en} (observed)` : en,
          isHoliday: true, source: 'COMPUTED', detail: null, region, religion, deity: null,
        });
      };
      add(date, false);
      if (shifts) {
        const w = dayOfWeek(date);
        if (w === 6) add(date - 1, true);
        else if (w === 0) add(date + 1, true);
      }
    }
  }
  return out.sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : 0));
}

// ---------------------------------------------------------------- Christian, Jewish, Islamic
export function christianDays(from, to) {
  const out = [];
  for (let y = yearOf(from); y <= yearOf(to); y++) {
    const e = easter(y);
    const c = 'CHRISTIAN';
    const xmas = epochDay(y, 12, 25);
    let advent = xmas - 28;
    while (dayOfWeek(advent) !== 0) advent++;
    out.push(
      event(epochDay(y, 1, 6), 'திருக்காட்சி விழா', 'Epiphany', c),
      event(e - 46, 'சாம்பல் புதன் (தவக்காலம் ஆரம்பம்)', 'Ash Wednesday (Lent begins)', c),
      event(epochDay(y, 3, 25), 'மங்கள வார்த்தை', 'Annunciation', c),
      event(e - 7, 'குருத்தோலை ஞாயிறு', 'Palm Sunday', c),
      event(e - 3, 'பெரிய வியாழன்', 'Maundy Thursday', c),
      event(e - 2, 'புனித வெள்ளி', 'Good Friday', c),
      event(e, 'ஈஸ்டர்', 'Easter Sunday', c),
      event(orthodoxEaster(y), 'ஆர்த்தடாக்ஸ் ஈஸ்டர்', 'Orthodox Easter', c),
      event(e + 39, 'விண்ணேற்றம்', 'Ascension Day', c),
      event(e + 49, 'பெந்தகோஸ்து', 'Pentecost', c),
      event(epochDay(y, 8, 15), 'மரியாவின் விண்ணேற்பு', 'Assumption of Mary', c),
      event(epochDay(y, 11, 1), 'அனைத்துப் புனிதர் தினம்', "All Saints' Day", c),
      event(epochDay(y, 11, 2), 'அனைத்து ஆன்மாக்கள் தினம்', "All Souls' Day", c),
      event(advent, 'திருவருகைக் காலம் ஆரம்பம்', 'First Sunday of Advent', c),
      event(epochDay(y, 12, 24), 'கிறிஸ்துமஸ் ஈவ்', 'Christmas Eve', c),
      event(xmas, 'கிறிஸ்துமஸ்', 'Christmas', c),
    );
  }
  return out.filter((ev) => ev.date >= isoOf(from) && ev.date <= isoOf(to));
}

// Hebrew calendar (Reingold & Dershowitz, "Calendrical Calculations"), on R.D. numbers.
const hebrewLeap = (y) => mod(7 * y + 1, 19) < 7;
function hebrewElapsedDays(y) {
  const months = fdiv(235 * y - 234, 19);
  const parts = 12084 + 13753 * months;
  const day = 29 * months + fdiv(parts, 25920);
  return mod(3 * (day + 1), 7) < 3 ? day + 1 : day;
}
function hebrewNewYear(y) {
  const ny0 = hebrewElapsedDays(y - 1), ny1 = hebrewElapsedDays(y), ny2 = hebrewElapsedDays(y + 1);
  const correction = ny2 - ny1 === 356 ? 2 : ny1 - ny0 === 382 ? 1 : 0;
  return HEBREW_EPOCH + ny1 + correction;
}
const hebrewYearDays = (y) => hebrewNewYear(y + 1) - hebrewNewYear(y);
function hebrewMonthDays(y, m) {
  const yd = hebrewYearDays(y);
  if ([2, 4, 6, 10, 13].includes(m)) return 29;
  if (m === 12 && !hebrewLeap(y)) return 29;
  if (m === 8 && mod(yd, 10) !== 5) return 29;
  if (m === 9 && mod(yd, 10) === 3) return 29;
  return 30;
}
function hebrewToRd(y, m, d) {
  const lastMonth = hebrewLeap(y) ? 13 : 12;
  let days = hebrewNewYear(y) + d - 1;
  if (m < 7) {
    for (let mm = 7; mm <= lastMonth; mm++) days += hebrewMonthDays(y, mm);
    for (let mm = 1; mm < m; mm++) days += hebrewMonthDays(y, mm);
  } else {
    for (let mm = 7; mm < m; mm++) days += hebrewMonthDays(y, mm);
  }
  return days;
}

export function jewishDays(from, to) {
  const out = [];
  const note = 'சூரியன் மறைவிலிருந்து முந்தைய மாலை ஆரம்பம் · Begins at sundown the evening before';
  const h = (y, m, d) => hebrewToRd(y, m, d) - RD_EPOCH_DAY;
  for (let g = yearOf(from); g <= yearOf(to); g++) {
    const spring = g + 3760, autumn = spring + 1;
    const j = 'JEWISH';
    out.push(
      event(h(spring, hebrewLeap(spring) ? 13 : 12, 14), 'பூரிம்', 'Purim', j, note),
      event(h(spring, 1, 15), 'பாஸ்கா (பெசாக்)', 'Passover (Pesach)', j, note),
      event(h(spring, 3, 6), 'ஷாவுவோத்', 'Shavuot', j, note),
      event(h(autumn, 7, 1), 'ரோஷ் ஹஷானா (யூதப் புத்தாண்டு)', 'Rosh Hashanah', j, note),
      event(h(autumn, 7, 10), 'யோம் கிப்பூர்', 'Yom Kippur', j, note),
      event(h(autumn, 7, 15), 'சுக்கோத்', 'Sukkot', j, note),
      event(h(autumn, 9, 25), 'ஹனுக்கா ஆரம்பம்', 'Hanukkah begins', j, note),
    );
  }
  return out.filter((ev) => ev.date >= isoOf(from) && ev.date <= isoOf(to));
}

function tabularHijri(rd) {
  const year = fdiv(30 * (rd - ISLAMIC_EPOCH) + 10646, 10631);
  const firstOfYear = ISLAMIC_EPOCH - 1 + 354 * (year - 1) + fdiv(3 + 11 * year, 30) + 1;
  const month = Math.min(12, Math.max(1, fdiv(11 * (rd - firstOfYear) + 330, 325)));
  return { year, month };
}

/** Hijri month starts (epoch day, year, month) overlapping [from, to]. */
export function islamicMonthStarts(from, to) {
  const out = [];
  let jd = A.julianDay(Date.UTC(...ymd(from - 40)));
  const endJd = A.julianDay(Date.UTC(...ymd(to + 2)));
  while (jd < endJd) {
    const conj = A.findCrossing(jd, 35, 1.0, 0, A.elongation);
    if (conj === null) break;
    const utcDay = Math.floor(A.instantOf(conj) / MS_DAY);
    const meccaSunset = A.sunriseSunset(utcDay, MECCA.lat, MECCA.lon)[1];
    const first = utcDay + (conj < meccaSunset ? 1 : 2);
    const { year, month } = tabularHijri(first + 14 + RD_EPOCH_DAY);
    out.push({ day: first, year, month });
    jd = conj + 1;
  }
  return out;
}
const ymd = (day) => {
  const { y, m, d } = fromEpochDay(day);
  return [y, m - 1, d];
};

export function islamicDays(from, to) {
  const i = 'ISLAM';
  const note = 'கணக்கீடு; பிறை பார்த்தலால் ஒரு நாள் மாறலாம் · Calculated; local moon sighting may differ by a day. Begins at sunset the evening before.';
  const out = [];
  for (const { day: first, year, month } of islamicMonthStarts(from, to)) {
    const d = (n) => first + n - 1;
    const add = (...evs) => out.push(...evs);
    switch (month) {
      case 1: add(event(d(1), `இஸ்லாமியப் புத்தாண்டு (${year} AH)`, `Islamic New Year (${year} AH)`, i, note), event(d(10), 'ஆஷூரா', 'Ashura', i, note)); break;
      case 3: add(event(d(12), 'மீலாது நபி', 'Mawlid an-Nabi', i, note)); break;
      case 7: add(event(d(27), 'இஸ்ரா மிஃராஜ்', "Isra and Mi'raj", i, note)); break;
      case 8: add(event(d(15), 'ஷபே பராஅத்', 'Shab-e-Barat', i, note)); break;
      case 9: add(event(d(1), 'ரமலான் ஆரம்பம்', 'Ramadan begins', i, note), event(d(27), 'லைலத்துல் கத்ர் (27-ஆம் இரவு)', 'Laylat al-Qadr (27th night)', i, note)); break;
      case 10: add(event(d(1), 'ரமலான் பண்டிகை (ஈதுல் பித்ர்)', 'Eid al-Fitr', i, note)); break;
      case 12: add(event(d(9), 'அரஃபா நாள்', 'Day of Arafah', i, note), event(d(10), 'பக்ரீத் (ஈதுல் அழ்ஹா)', 'Eid al-Adha', i, note)); break;
      default: break;
    }
  }
  return out.filter((ev) => ev.date >= isoOf(from) && ev.date <= isoOf(to));
}

/** Christian + Jewish + Islamic observances between [from] and [to]. */
export function worldReligionDays(from, to) {
  return [...christianDays(from, to), ...jewishDays(from, to), ...islamicDays(from, to)].sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : 0));
}
