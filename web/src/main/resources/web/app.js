// Tamil calendar web app: the UI. All calculations run in the browser (engine/), so any place and date works.
import { PLACES, placeLabel, localHolidayRegions, timingConvention, usesImperialUnits } from './engine/locations.js';
import { ICON, TAG_ORDER, RELIGION_ORDER } from './engine/icons.js';
import { tithiNamesOf } from './engine/engine.js';
import { ayanamOfSun, ayanamFacts, ayanamYear, ayanamChanges, sunRasiOn, AYANAM_NAMES } from './engine/ayanam.js';
import { soolam, abhijit, karanaName, isVishti, isKariNaal, hora, tharaOf, tharaPasses, personalChandrashtamam, moonPhase, moonPhaseInfo } from './engine/rules.js';
import { monthDays, importantDates, applying } from './engine/calendar.js';
import { rasiPalan, skyPositions, grahasIn, gochara } from './engine/rasi.js';
import { CONTENT } from './engine/content.js';
import { epochDay, isoOf, parseIso, dayOfWeek, dayOfMonth, monthOf, yearOf, fromEpochDay, wallClock, zonedMs, hms, MS_DAY } from './engine/civil.js';
import { loadSettings, saveSettings } from './app/store.js';
import { fetchWeather, sceneOf, labelOf } from './app/weather.js';
import { slokaFor, speak, stopSpeaking, TempleMusic } from './app/sloka.js';
import { scheduleReminders, notifyUser, clearReminders } from './app/reminders.js';
import { APP_VERSION } from './app/version.js';

const $ = (id) => document.getElementById(id);
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);
const TABS = [
  ['today', 'இன்று', 'Today'], ['month', 'மாதம்', 'Month'], ['festivals', 'விழாக்கள்', 'Festivals'],
  ['important', 'முக்கிய நாட்கள்', 'Important'], ['muhurtham', 'சுப முகூர்த்தம்', 'Muhurtham'], ['rasi', 'ராசி', 'Rasi'], ['location', 'இடம்', 'Location'],
  ['learn', 'அறிவோம்', 'Learn'],
  ['theme', 'தீம்', 'Theme'], ['settings', 'அமைப்புகள்', 'Settings'], ['about', 'பற்றி', 'About'],
];
const ALWAYS_SHOWN = new Set(['today', 'location', 'theme', 'settings', 'about']);
const MONTHS_TA = ['ஜனவரி', 'பிப்ரவரி', 'மார்ச்', 'ஏப்ரல்', 'மே', 'ஜூன்', 'ஜூலை', 'ஆகஸ்ட்', 'செப்டம்பர்', 'அக்டோபர்', 'நவம்பர்', 'டிசம்பர்'];
const MONTHS_EN = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
const WEEKDAYS_TA_SHORT = ['ஞா', 'தி', 'செ', 'பு', 'வி', 'வெ', 'ச'];

const state = {
  settings: loadSettings(),
  tab: 'today',
  day: 0,            // selected epoch day
  flipDir: 0,        // +1 / -1 while the Month page turns, read once by viewMonth
  monthOf: null,     // [year, month] shown on the month tab
  place: null,       // engine place object
  placeKey: '',
  cache: new Map(),
  school: null,
  weather: null,
  weatherErr: '',
  music: null,
  rasi: null,
  rasiTab: 0,
  search: { query: '', results: [], error: '' },
  kindFilter: 'ALL',
  weatherKey: '',
  spokenKey: '',
  todayPanel: 'panchangam',
  mhFrom: null, mhTo: null,     // muhurtham date range
  cmpKey: null, cmpDay: null,   // compare: other place and day
  findQuery: '',                // festival search
  packDays: new Map(),   // epoch day -> day, from the saved year pack of the current place
  packKey: '',
  lastInput: Date.now(),
  ambientOn: false,
};

// ---------------------------------------------------------------- helpers
const lb = (ta, en) => (state.settings.tamilOnly ? ta : `${ta} · ${en}`);
const lbo = (o) => lb(o.ta, o.en);
const tzNow = () => wallClock(Date.now(), state.place.zoneId);
const todayEpoch = () => tzNow().day;
const timeOf = (hms) => {
  const [h, m] = hms.split(':').map(Number);
  if (state.settings.clock24h) return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  return `${h % 12 === 0 ? 12 : h % 12}:${String(m).padStart(2, '0')}${h < 12 ? ' AM' : ' PM'}`;
};
const secOf = (hms) => { const [h, m, s] = hms.split(':').map(Number); return h * 3600 + m * 60 + s; };
const endOf = (iso) => (iso && iso.includes('T') ? timeOf(iso.slice(11, 19)) : '');
const dateLong = (day) => new Date(day * MS_DAY).toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' });
const unitsImperial = () => (state.settings.units === 'AUTO' ? usesImperialUnits(state.place) : state.settings.units === 'IMPERIAL');
const conventionOf = () => (state.settings.convention === 'AUTO' ? timingConvention(state.place) : state.settings.convention);
const placeKeyOf = (s) => (s.place.custom ? `custom:${s.place.custom.latitude},${s.place.custom.longitude},${s.place.custom.zoneId}` : s.place.preset);
const resolvePlace = () => {
  const p = state.settings.place;
  if (p.custom) return p.custom;
  return PLACES.find((x) => x.key === p.preset) || PLACES[0];
};

/** Grid range for a month: Sunday before the 1st to Saturday after the last day. */
function gridRange(year, month) {
  const first = epochDay(year, month + 1, 1);
  const last = epochDay(year, month + 2, 0);
  return [first - dayOfWeek(first), last + (6 - dayOfWeek(last))];
}

/** Days for a range, computed once per place, convention and range. Settings filters are applied on read. */
function daysBetween(from, to) {
  const key = `${state.placeKey}|${conventionOf()}|${from}|${to}`;
  if (!state.cache.has(key) && state.packDays.size && [...Array(to - from + 1).keys()].every((i) => state.packDays.has(from + i))) {
    // The saved year pack already has these days: use it instead of computing.
    const days = [];
    for (let e = from; e <= to; e++) days.push(state.packDays.get(e));
    state.cache.set(key, days);
  }
  if (!state.cache.has(key)) {
    state.cache.set(key, monthDays(state.place, from, to, { convention: conventionOf(), schoolJson: state.school }));
    if (state.cache.size > 60) state.cache.delete(state.cache.keys().next().value);
  }
  return state.cache.get(key).map((d) => applying(d, state.settings));
}
function dayData(d) {
  const [from, to] = gridRange(yearOf(d), monthOf(d) - 1);
  return daysBetween(from, to).find((x) => x.epochDay === d);
}
/** Days from epoch [from] to [to] inclusive, in chunks of one grid (cached per chunk). */
function daysInRange(from, to) {
  const out = [];
  for (let a = from; a <= to; a += 42) out.push(...daysBetween(a, Math.min(to, a + 41)));
  return out;
}
function daysFrom(start, count) {
  const out = [];
  for (let from = start; from < start + count; from += 42) {
    out.push(...daysBetween(from, Math.min(start + count - 1, from + 41)));
  }
  return out;
}

// ---------------------------------------------------------------- views
function renderTabs() {
  const hidden = new Set(state.settings.hiddenSections);
  $('tabs').innerHTML = TABS.filter(([id]) => ALWAYS_SHOWN.has(id) || !hidden.has(id)).map(([id, ta, en]) =>
    `<button class="tab${state.tab === id ? ' on' : ''}" data-act="tab" data-tab="${id}">${esc(ta)}${state.settings.tamilOnly ? '' : `<small>${esc(en)}</small>`}</button>`).join('');
  $('place-chip').textContent = state.place.nameEn + (state.place.countryCode ? '' : '');
}

function render() {
  renderTabs();
  applyTheme();
  const view = { today: viewToday, month: viewMonth, festivals: viewFestivals, important: viewImportant, muhurtham: viewMuhurtham, rasi: viewRasi, learn: viewLearn, location: viewLocation, theme: viewTheme, settings: viewSettings, about: viewAbout }[state.tab];
  $('view').innerHTML = view();
  if (state.tab === 'today') {
    loadWeatherIfNeeded();
    maybeReadSloka();
  }
}

// ---- today
/** The window the time is in right now (or the next one), for the "Now" card. */
function nowStatus(d, nowSec) {
  const windows = [
    ...d.timings.nallaNeram.map((w) => ({ ...w, kind: 'good' })),
    { ...d.timings.rahuKalam, kind: 'bad' },
    { ...d.timings.yamagandam, kind: 'bad' },
    { ...d.timings.kuligai, kind: 'mixed' },
  ].map((w) => ({ ...w, a: secOf(w.start), b: secOf(w.end) }));
  const current = windows.find((w) => nowSec >= w.a && nowSec < w.b);
  const next = windows.filter((w) => w.a > nowSec).sort((x, y) => x.a - y.a)[0];
  return { current, next };
}
const nameOfWindow = (w) => (w.type === 'NALLA_NERAM' ? lb('நல்ல நேரம்', 'Nalla Neram') : lb(...CONTENT.enums.TimingType.find((x) => x[0] === w.type).slice(1, 3)));

function timelineHtml(d, nowSec) {
  const segs = [
    ...d.timings.nallaNeram.map((w) => ({ a: secOf(w.start), b: secOf(w.end), kind: 'good' })),
    ...[d.timings.rahuKalam, d.timings.yamagandam].map((w) => ({ a: secOf(w.start), b: secOf(w.end), kind: 'bad' })),
    { a: secOf(d.timings.kuligai.start), b: secOf(d.timings.kuligai.end), kind: 'mixed' },
  ].map((x) => `<i class="seg ${x.kind}" style="left:${(x.a / 864).toFixed(3)}%;width:${((x.b - x.a) / 864).toFixed(3)}%"></i>`).join('');
  const sunrise = secOf(d.sunrise), sunset = secOf(d.sunset);
  return `<div class="timeline" aria-label="${esc(lb('நாள் நேரம்', 'Day timeline'))}">
    <div class="track"><i class="daylight" style="left:${(sunrise / 864).toFixed(3)}%;width:${((sunset - sunrise) / 864).toFixed(3)}%"></i>${segs}
      ${nowSec !== null ? `<b class="now" style="left:${(nowSec / 864).toFixed(3)}%"></b>` : ''}</div>
    <div class="ticks"><span>12 AM</span><span>6</span><span>12</span><span>6</span><span>12 AM</span></div></div>`;
}

function viewToday() {
  const d = dayData(state.day);
  if (!d) return '<p class="status">No data for this day.</p>';
  const isToday = d.epochDay === todayEpoch();
  const nowSec = isToday ? wallClock(Date.now(), state.place.zoneId).sec : null;
  const p = d.panchangam;
  const nav = `<div class="bar"><button class="nav" data-act="day" data-d="-1" aria-label="Previous day">‹</button>
    <h1>${esc(dateLong(d.epochDay))}</h1><button class="nav" data-act="day" data-d="1" aria-label="Next day">›</button></div>
    ${isToday ? '' : `<button class="today" data-act="day-today">${lb('இன்று', 'Today')}</button>`}`;
  // Soft warning (validator section E), not a rejection: Tuesday is ruled by Mars, Saturday by Saturn.
  const weekdayFlag = d.weekdayIndex === 2
    ? `<li class="chip caution" title="Ruled by Mars: ideal for physical, engineering or litigation work; exercise caution for peaceful ventures.">${esc(lb('செவ்வாய் · Mars: எச்சரிக்கை', 'Mars day: caution'))}</li>`
    : d.weekdayIndex === 6
      ? `<li class="chip caution" title="Ruled by Saturn: favoured for foundational, mechanical or structural tasks; avoid hasty beginnings.">${esc(lb('சனி · Saturn: எச்சரிக்கை', 'Saturn day: caution'))}</li>`
      : '';
  const chips = weekdayFlag + importantDates(d).map((x) => `<li class="chip${x.holiday ? ' holiday' : ''}">${esc(lbo(x.title))}</li>`).join('');
  const hero = `<section class="hero">
    <p class="hero-week">${esc(lb(...CONTENT.names.weekdays[d.weekdayIndex]))}</p>
    <h2>${esc(lb(CONTENT.names.months[d.tamilDate.monthIndex][0] + ' ' + d.tamilDate.day, CONTENT.names.months[d.tamilDate.monthIndex][1] + ' ' + d.tamilDate.day))}</h2>
    <p class="hero-year">${esc(lb(CONTENT.names.years[d.tamilDate.yearCycleIndex][0] + ' வருடம்', CONTENT.names.years[d.tamilDate.yearCycleIndex][1] + ' year'))}</p>
    ${d.holiday || chips ? `<ul class="chips">${d.holiday ? `<li class="chip holiday">${esc(lb('விடுமுறை', 'Holiday'))}</li>` : ''}${chips}</ul>` : ''}
  </section>`;
  let nowCard = '';
  if (isToday) {
    const { current, next } = nowStatus(d, nowSec);
    const line = current
      ? `${esc(nameOfWindow(current))} · ${esc(lb('முடிவு', 'ends'))} ${timeOf(current.end)}`
      : next ? `${esc(lb('அடுத்தது', 'Next'))}: ${esc(nameOfWindow(next))} ${timeOf(next.start)}` : esc(lb('இன்று நேரங்கள் முடிந்தன', 'No more windows today'));
    nowCard = `<section class="now-card ${current ? current.kind : 'none'}">
      <p class="now-label">${esc(lb('இப்போது', 'Now'))}</p><p class="now-line">${line}</p></section>`;
  }
  // The rising sign at sunrise, and when the next sign rises.
  const lagna = d.panchangam.lagnam;
  const lagnaValue = lagna.length ? `${esc(lb(...CONTENT.names.rasis[lagna[0].rasi]))} <small>→ ${endOf(lagna[1]?.start)}</small>` : '—';
  // Day rules (engine/rules.js, docs/BACKLOG.md): Soolam, Karanam, Sraardham and Abhijit.
  const sool = soolam(d.weekdayIndex);
  const [karanaTa, karanaEn] = karanaName(p.karanam);
  const sraardha = p.sraardhaTithi;
  const secOf = (t) => { const [h, m, s] = t.split(':').map(Number); return h * 3600 + m * 60 + s; };
  const [abhijitFrom, abhijitTo] = abhijit(secOf(d.sunrise), secOf(d.sunset));
  // Personal rules need the birth star and Rasi from Settings (Thara Balam, and your Chandrashtamam).
  const { janmaStar, janmaRasi } = state.settings;
  const tharaT = janmaStar >= 0 ? tharaOf(p.nakshatram.index, janmaStar) : null;
  const personalTiles = [
    tharaT === null ? '' : `<div class="tile"><span class="ico">✶</span><dt>${esc(lb('தாரா பலன்', 'Thara Balam'))}</dt><dd>${esc(lb(...CONTENT.names.nakshatras[p.nakshatram.index]))} <small>· ${tharaT} ${tharaPasses(tharaT) ? '✓ ' + esc(lb('நல்லது', 'good')) : '✗ ' + esc(lb('தவிர்க்க', 'avoid'))}</small></dd></div>`,
    janmaRasi < 0 ? '' : `<div class="tile"><span class="ico">☾</span><dt>${esc(lb('உங்கள் சந்திராஷ்டமம்', 'Your Chandrashtamam'))}</dt><dd>${personalChandrashtamam(p.moonRasi, janmaRasi) ? esc(lb('ஆம் · Yes', 'Yes')) : esc(lb('இல்லை · No', 'No'))}</dd></div>`,
  ].join('');
  const facts = [
    ['🌅', lb('உதயம்', 'Sunrise'), timeOf(d.sunrise)],
    ['🌇', lb('அஸ்தமனம்', 'Sunset'), timeOf(d.sunset)],
    ['🌙', lb('திதி', 'Tithi'), `${esc(lb(p.tithi.nameTa, p.tithi.nameEn))} <small>→ ${endOf(p.tithi.endsAt)}</small>`],
    ['✦', lb('நட்சத்திரம்', 'Nakshatram'), `${esc(lb(p.nakshatram.nameTa, p.nakshatram.nameEn))} <small>→ ${endOf(p.nakshatram.endsAt)}</small>`],
    ['☉', lb('யோகம்', 'Yogam'), `${esc(lb(p.yogam.nameTa, p.yogam.nameEn))} <small>→ ${endOf(p.yogam.endsAt)}</small>`],
    ['☾', lb('சந்திர ராசி', 'Moon sign'), esc(lb(...CONTENT.names.rasis[p.moonRasi]))],
    ['⬆', lb('லக்னம்', 'Lagnam'), lagnaValue],
    ['⚑', lb('சூலம்', 'Soolam'), `${esc(lb(...sool.direction))} <small>${esc(lb(sool.remedyTa, sool.remedyEn))}</small>`],
    ['☯', lb('கரணம்', 'Karanam'), `${esc(lb(karanaTa, karanaEn))}${isVishti(p.karanam) ? ' <small>· Vishti</small>' : ''}${isKariNaal(d.tamilDate.monthIndex, d.tamilDate.day) ? ' <small>· கரி நாள் · Kari Naal</small>' : ''}`],
    ['✺', lb('சிராத்தம்', 'Sraardham'), sraardha == null ? '—' : esc(lb(...tithiNamesOf(sraardha)))],
    ['✧', lb('அபிஜித்', 'Abhijit'), `${timeOf(hms(abhijitFrom))} – ${timeOf(hms(abhijitTo))}`],
  ];
  const tile = ([icon, label, value], cls = '') => `<div class="tile ${cls}"><span class="ico">${icon}</span><dt>${esc(label)}</dt><dd>${value}</dd></div>`;
  // Tier 2: the main panchangam values. Tier 4: the rule checks (Lagnam, Soolam, Karanam, Sraardham, Abhijit).
  const phase = moonPhase(p.tithi.index);
  const phaseTile = ['🌙', lb('சந்திர நிலை', 'Moon phase'), `${phase[0]} ${esc(lb(phase[1], phase[2]))}`];
  const af = ayanamFacts(d.epochDay, state.place);
  const ayanamTile = ['☀', lb('அயனம்', 'Ayanam'), `${esc(lb(...AYANAM_NAMES[af.kind]))} <small>${esc(lb('நாள்', 'day'))} ${af.daysIn + 1}</small>`];
  const panchangamTiles = [facts[2], facts[3], facts[4], facts[5], phaseTile, ayanamTile, facts[0], facts[1]].map((f) => tile(f, 'primary')).join('');
  const ruleTiles = [facts[6], facts[7], facts[8], facts[9], facts[10]].map((f) => tile(f)).join('');
  const row = (t) => {
      const cls = t.type === 'NALLA_NERAM' || t.type === 'GOWRI_NALLA_NERAM' ? 'good' : t.type === 'KULIGAI' ? 'mixed' : 'bad';
      const label = lb(CONTENT.enums.TimingType.find((x) => x[0] === t.type)[1], CONTENT.enums.TimingType.find((x) => x[0] === t.type)[2]);
      const gowri = t.gowri !== null ? ` · ${lb(CONTENT.enums.Gowri[t.gowri][1], CONTENT.enums.Gowri[t.gowri][2])}` : '';
      return `<li class="${cls}"><span>${esc(label + gowri)}</span><time>${timeOf(t.start)} – ${timeOf(t.end)}</time></li>`;
  };
  // Good windows first (Nalla Neram and Gowri), then the windows to avoid. Every card is visible; nothing is collapsed.
  const byStart = (a, b) => secOf(a.start) - secOf(b.start);
  // Nalla Neram is a good Gowri window with Rahu Kalam and Yamagandam cut out, so each Nalla window sits inside
  // one or more Gowri windows. One card per time: the Nalla window, with the Gowri names beside it. A Gowri
  // window with no Nalla window inside it keeps its own card.
  const overlaps = (a, b) => secOf(a.start) < secOf(b.end) && secOf(b.start) < secOf(a.end);
  const gowriName = (g) => lb(CONTENT.enums.Gowri[g][1], CONTENT.enums.Gowri[g][2]);
  const gowriGood = d.timings.gowriNallaNeram;
  const goodList = [
    ...d.timings.nallaNeram.map((n) => ({ start: n.start, end: n.end, title: lb('நல்ல நேரம்', 'Nalla Neram'),
      sub: gowriGood.filter((g) => overlaps(g, n)).map((g) => gowriName(g.gowri)).join(' · ') })),
    ...gowriGood.filter((g) => !d.timings.nallaNeram.some((n) => overlaps(g, n))).map((g) => ({ start: g.start, end: g.end,
      title: lb('கௌரி நல்ல நேரம்', 'Gowri Nalla Neram'), sub: gowriName(g.gowri) })),
  ].sort(byStart);
  const avoidList = [d.timings.rahuKalam, d.timings.yamagandam, d.timings.kuligai].sort(byStart);
  const good = goodList.map((c) => `<li class="good"><span>${esc(c.title)}${c.sub ? ` · ${esc(c.sub)}` : ''}</span><time>${timeOf(c.start)} – ${timeOf(c.end)}</time></li>`).join('');
  const avoid = avoidList.map(row).join('');
  const chandra = d.chandrashtamam;
  const weatherCard = state.settings.weather ? (isToday ? weatherHtml() : forecastHtml(d.epochDay)) : '';
  // Three panels, one shown at a time, so the page stays short.
  const panels = [['panchangam', 'பஞ்சாங்கம்', 'Panchangam'], ['sloka', 'ஸ்லோகம்', 'Sloka'], ['weather', 'வானிலை', 'Weather']];
  const tabRow = `<div class="panel-tabs" role="tablist">${panels.map(([id, ta, en]) =>
    `<button class="pill${state.todayPanel === id ? ' on' : ''}" role="tab" aria-selected="${state.todayPanel === id}" data-act="today-panel" data-p="${id}">${esc(ta)}${state.settings.tamilOnly ? '' : `<small>${esc(en)}</small>`}</button>`).join('')}</div>`;
  let body;
  if (state.todayPanel === 'sloka') body = slokaCard(slokaFor(d));
  else if (state.todayPanel === 'weather') body = state.settings.weather ? weatherCard : `<p class="small">${esc(lb('வானிலை அமைப்புகளில் முடக்கப்பட்டுள்ளது', 'Weather is turned off in Settings'))}</p>`;
  else body = `${nowCard}
      <section class="tier tier-1">
        <h2 class="tier-title">${esc(lb('முக்கிய நேரங்கள்', 'Key timings'))}</h2>
        <section class="detail timings-board">
          <h3>${esc(lb('நேரங்கள்', 'Timings'))}</h3>
          <p class="grp">${esc(lb('நல்ல நேரம்', 'Good windows'))} · ${goodList.length}</p>
          <ul class="timings cards">${good}</ul>
          <p class="grp">${esc(lb('தவிர்க்க', 'Avoid'))} · ${avoidList.length}</p>
          <ul class="timings cards">${avoid}</ul>
        </section>
      </section>
      <section class="tier tier-2">
        <h2 class="tier-title">${esc(lb('பஞ்சாங்கம்', 'Panchangam'))}</h2>
        <div class="tiles primary-tiles">${panchangamTiles}</div>
      </section>
      <section class="tier tier-3">
        <h2 class="tier-title">${esc(lb('நாள் அட்டவணை', 'Day schedule'))}</h2>
        <section class="detail"><h3>${esc(lb('நாள் நேரம்', 'Day timeline'))}</h3>${timelineHtml(d, nowSec)}</section>
        <div class="today-split">
          <section class="detail">
            <h3>${esc(lb('லக்னம்', 'Lagnam'))}</h3>
            <ul class="sched sched-1">${lagna.map((s) => `<li class="sched-row"><span>${esc(lb(...CONTENT.names.rasis[s.rasi]))}</span><time>${timeOf(s.start.slice(11, 19))}</time></li>`).join('')}</ul>
          </section>
          <section class="detail">
            <h3>${esc(lb('ஹோரை', 'Hora'))}</h3>
            <ul class="sched sched-2">${hora(d.weekdayIndex, secOf(d.sunrise), secOf(d.sunset), secOf(d.nextSunrise)).map((h) => `<li class="sched-row${h.benefic ? ' good' : ''}"><span>${esc(lb(...h.lord))}</span><time>${timeOf(hms(h.start))}</time></li>`).join('')}</ul>
          </section>
        </div>
      </section>
      <section class="tier tier-4">
        <h2 class="tier-title">${esc(lb('விதிகள்', 'Rules'))}</h2>
        <div class="tiles">${ruleTiles}${personalTiles}</div>
      </section>
      <p class="warn chandra">${esc(lb('சந்திராஷ்டமம்', 'Chandrashtamam'))} — ${esc(lb(...CONTENT.names.rasis[chandra.rasi]))}
        <br><small>${esc(chandra.nakshatras.map((k) => lb(...CONTENT.names.nakshatras[k])).join(', '))}</small></p>
      <p class="small accuracy">${esc(lb('இந்தத் தகவல் கணக்கீடு மட்டுமே. திருமணம், சுப காரியம், சிராத்தம் போன்றவற்றுக்கு கோவில் அர்ச்சகரிடம் நேரத்தை உறுதிப்படுத்தவும்.', 'Calculated for information only. Before a marriage, ceremony, Sraardham or any muhurtham, confirm the date and time with a temple priest.'))}</p>
      ${reminderNote()}`;
  return `${nav}
    <div class="today-grid">
      ${hero}
      ${tabRow}
      ${body}
    </div>`;
}
const pakshaName = (p) => (p === 'SHUKLA' ? ['வளர்பிறை', 'Shukla Paksha'] : ['தேய்பிறை', 'Krishna Paksha']);

function slokaCard(s) {
  const sp = state.settings;
  return `<section class="sloka">
    <h3>${esc(lb('ஸ்லோகம்', 'Sloka of the day'))}</h3>
    <p class="sloka-title">${esc(lb(s.titleTa, s.titleEn))}</p>
    <p class="sloka-text">${esc(s.textTa).replace(/\\n|\n/g, '<br>')}</p>
    <p class="sloka-meaning">${esc(s.meaningEn)}</p>
    <div class="row">
      <button class="pill" data-act="sloka-read">🔊 ${esc(lb('படி', 'Read'))}</button>
      <button class="pill${state.music ? ' on' : ''}" data-act="sloka-music">♪ ${esc(lb('இசை', 'Music'))} ${state.music ? 'on' : 'off'}</button>
    </div>
    <p class="small">${esc(s.source)}</p>
    <p class="small">${esc(lb('மேலும் படிக்க', 'Read more'))}: <a href="${RELATED_URL}" target="_blank" rel="noopener">thiruppugazh-ui.vercel.app ↗</a></p>
  </section>`;
}

/** The forecast for a day within the next 7 (used when the selected day is not today). */
function forecastHtml(epoch) {
  const w = state.weather;
  if (!state.settings.weather) return '';
  if (!w) return `<section class="weather"><p class="small">${esc(lb('முன்னறிவிப்பு ஏற்றப்படுகிறது…', 'Loading forecast…'))}</p></section>`;
  const day = w.daily.find((x) => x.date === isoOf(epoch));
  if (!day) return `<section class="weather"><p class="small">${esc(lb('இந்த நாளுக்கு முன்னறிவிப்பு இல்லை (7 நாட்கள் மட்டும்)', 'No forecast for this day (7 days only)'))}</p></section>`;
  return `<section class="weather scene-${sceneOf(day.code, true)}">
    <h3>Forecast · ${esc(dateShort(day.date))}</h3>
    <div class="wx-now"><b>${Math.round(day.max)}${w.unit} / ${Math.round(day.min)}${w.unit}</b><span>${esc(labelOf(day.code))}</span></div></section>`;
}

function weatherHtml() {
  if (state.weatherErr) return `<section class="weather"><p class="small">${esc(state.weatherErr)}</p></section>`;
  const w = state.weather;
  if (!w) return '<section class="weather"><p class="small">Loading weather…</p></section>';
  const scene = sceneOf(w.current.code, w.current.isDay);
  const anim = state.settings.animations && state.settings.weatherAnimations;
  const season = [[12, 1, 2, 'winter'], [3, 4, 5, 'spring'], [9, 10, 11, 'autumn']].find((s) => s.slice(0, 3).includes(monthOf(todayEpoch())));
  const particles = anim && season ? `<div class="particles ${season[3]}">${Array.from({ length: 12 }, (_, i) => `<i style="left:${(i * 83) % 100}%;animation-delay:${(i * 0.7) % 6}s"></i>`).join('')}</div>` : '';
  const hours = w.hourly.map((h) => `<li><span>${timeOf(h.time.slice(11, 16) + ':00')}</span><b>${Math.round(h.temp)}${w.unit}</b><small>${h.rain}%</small></li>`).join('');
  const days = w.daily.map((d) => `<li><span>${dayOfWeekLabel(parseIso(d.date))}</span><b>${Math.round(d.max)}° / ${Math.round(d.min)}°</b><small>${esc(labelOf(d.code))}</small></li>`).join('');
  return `<section class="weather scene-${scene}${anim ? ' animated' : ''}">${particles}
    <div class="wx-now"><b>${Math.round(w.current.temp)}${w.unit}</b><span>${esc(labelOf(w.current.code))}</span>
    <small>Humidity ${w.current.humidity}% · Wind ${Math.round(w.current.wind)} ${w.windUnit}</small></div>
    <h3>Next 12 hours</h3><ul class="wx-list">${hours}</ul>
    <h3>7 days</h3><ul class="wx-list days">${days}</ul></section>`;
}
const dayOfWeekLabel = (day) => CONTENT.names.weekdays[dayOfWeek(day)][1].slice(0, 3);

function reminderNote() {
  const s = state.settings;
  if (!s.rahuReminder && !s.nallaReminder) return '';
  const granted = 'Notification' in window && Notification.permission === 'granted';
  return `<p class="small">${esc(lb('நினைவூட்டல்கள் இந்தப் பக்கம் திறந்திருக்கும்போது மட்டும்.', 'Reminders work while this page is open.'))}
    ${granted ? '' : ` <button class="pill" data-act="notify-allow">${esc(lb('அறிவிப்புகளை அனுமதி', 'Allow notifications'))}</button>`}</p>`;
}

// ---- month
/** The symbols in a month cell, as on the APK: the day's observances, then one per other faith; at most three. */
function dayIcons(d) {
  const faiths = importantDates(d).map((x) => x.religion).filter((r) => r && r !== 'HINDU');
  const keys = [...TAG_ORDER.filter((t) => t !== 'GOVT_HOLIDAY' && d.tags.includes(t)), ...RELIGION_ORDER.filter((r) => faiths.includes(r))];
  return keys.slice(0, 3).map((k) => `<span class="ico-i" role="img" aria-label="${esc(ICON[k].en)}">${ICON[k].svg}</span>`).join('');
}

function viewMonth() {
  const [y, m] = state.monthOf || [yearOf(todayEpoch()), monthOf(todayEpoch()) - 1];
  // Only as many weeks as the month needs (usually 5), as the APK month grid does.
  const first = epochDay(y, m + 1, 1);
  const weeks = Math.ceil((dayOfWeek(first) + epochDay(y, m + 2, 0) - first + 1) / 7);
  const from = first - dayOfWeek(first);
  const to = from + weeks * 7 - 1;
  const days = new Map(daysBetween(from, to).map((d) => [d.epochDay, d]));
  const weekdays = CONTENT.names.weekdays.map((w, i) => `<div class="${i === 0 ? 'sun' : ''}">${WEEKDAYS_TA_SHORT[i]}<small>${w[1].slice(0, 3)}</small></div>`).join('');
  const tamilMonths = [...new Set([...days.values()].filter((d) => monthOf(d.epochDay) === m + 1)
    .map((d) => `${CONTENT.names.months[d.tamilDate.monthIndex][0]} (${CONTENT.names.months[d.tamilDate.monthIndex][1]})`))].join(' – ');
  const today = todayEpoch();
  const cells = [];
  for (let e = from; e <= to; e++) {
    const d = days.get(e);
    const cls = ['cell'];
    if (monthOf(e) !== m + 1) cls.push('other');
    if (dayOfWeek(e) === 0) cls.push('sun');
    if (d && d.holiday) cls.push('holiday');
    if (e === today) cls.push('today');
    if (e === state.day) cls.push('selected');
    // The day's important dates as titles in the cell, most important first; extras shown as "+N", as on the APK.
    const events = d ? importantDates(d) : [];
    const maxDates = weeks <= 5 ? 2 : 1;
    const more = events.length - maxDates;
    const titles = events.slice(0, maxDates).map((x, i) => {
      const text = i === maxDates - 1 && more > 0 ? `${x.title.ta} +${more}` : x.title.ta;
      const kind = x.holiday ? ' alert' : x.religion && x.religion !== 'HINDU' ? ' muted' : '';
      return `<span class="ev${kind}">${esc(text)}</span>`;
    }).join('');
    const tamil = d && d.tamilDate.day === 1 ? `<span class="tm">${esc(CONTENT.names.months[d.tamilDate.monthIndex][0])}</span>` : d ? `<span class="t">${d.tamilDate.day}</span>` : '';
    const icons = d ? dayIcons(d) : '';
    const moon = d ? `<span class="moon" title="${esc(moonPhase(d.panchangam.tithi.index)[2])}">${moonPhase(d.panchangam.tithi.index)[0]}</span>` : '';
    cells.push(`<button class="${cls.join(' ')}" data-act="select-day" data-d="${e}"><span class="top"><span class="g">${dayOfMonth(e)}</span><span class="ico-row">${moon}${icons}</span>${tamil}</span><span class="evs">${titles}</span></button>`);
  }
  const sel = dayData(state.day) || dayData(today);
  // The flip view is the same month on a wall-calendar page; the arrows turn it.
  const page = state.settings.monthView === 'page';
  const flip = state.flipDir ? ` flip-${state.flipDir > 0 ? 'next' : 'prev'}` : '';
  state.flipDir = 0;
  return `<div class="month-wrap${page ? ' page' : ''}${flip}"><div class="bar"><button class="nav" data-act="month" data-m="-1" aria-label="Previous month">‹</button>
    <h1>${esc(MONTHS_TA[m])} ${y} · ${MONTHS_EN[m]}</h1><button class="nav" data-act="month" data-m="1" aria-label="Next month">›</button></div>
    <p class="month-sub">${esc(tamilMonths)}</p>
    <button class="today" data-act="month-today">${lb('இன்று', 'Today')}</button>
    <div class="row no-print"><button class="pill" data-act="month-view" aria-pressed="${page}">${page ? esc(lb('கட்டம்', 'Grid')) : esc(lb('தாள்', 'Page'))}</button><button class="pill" data-act="print-month">🖨 ${esc(lb('அச்சிடு', 'Print month'))}</button></div>
    <div class="weekdays">${weekdays}</div><div class="grid">${cells.join('')}</div>
    ${printTable(y, m, from, to)}
    <div class="detail small-detail">${sel ? `<h2>${esc(dateLong(sel.epochDay))}</h2><p class="ta-date">${esc(lb(CONTENT.names.months[sel.tamilDate.monthIndex][0] + ' ' + sel.tamilDate.day, CONTENT.names.months[sel.tamilDate.monthIndex][1] + ' ' + sel.tamilDate.day))}</p>
      ${moonPanel(sel)}
      <p class="small">${esc(lb('சூலம்', 'Soolam'))}: ${esc(lb(...soolam(sel.weekdayIndex).direction))} · ${esc(lb('சிராத்தம்', 'Sraardham'))}: ${sel.panchangam.sraardhaTithi == null ? '—' : esc(lb(...tithiNamesOf(sel.panchangam.sraardhaTithi)))}</p>
      <ul class="chips">${importantDates(sel).map((x) => `<li class="chip${x.holiday ? ' holiday' : ''}">${esc(lbo(x.title))}</li>`).join('') || `<li class="small">${esc(lb('சிறப்பு நாள் இல்லை', 'No observance'))}</li>`}</ul>
      <p class="small">${esc(lb('கரணம்', 'Karanam'))}: ${esc(lb(...karanaName(sel.panchangam.karanam)))}${isKariNaal(sel.tamilDate.monthIndex, sel.tamilDate.day) ? ` · ${esc(lb('கரி நாள்', 'Kari Naal'))}` : ''}</p>
      <button class="pill" data-act="open-today">${esc(lb('நாள் விவரம்', 'Day view'))} ›</button>` : ''}</div></div>`;
}

/** The month as a printable table (shown only in print). */
function printTable(y, m, from, to) {
  const rows = daysBetween(from, to).filter((d) => monthOf(d.epochDay) === m + 1).map((d) => {
    const notes = importantDates(d).map((x) => lbo(x.title)).join('; ');
    return `<tr><td>${esc(dateShort(isoOf(d.epochDay)))}</td><td>${esc(lb(...CONTENT.names.weekdays[d.weekdayIndex]))}</td>
      <td>${esc(lb(CONTENT.names.months[d.tamilDate.monthIndex][0] + ' ' + d.tamilDate.day, CONTENT.names.months[d.tamilDate.monthIndex][1] + ' ' + d.tamilDate.day))}</td>
      <td>${esc(lb(d.panchangam.tithi.nameTa, d.panchangam.tithi.nameEn))}</td><td>${esc(lb(d.panchangam.nakshatram.nameTa, d.panchangam.nakshatram.nameEn))}</td><td>${esc(notes)}</td></tr>`;
  }).join('');
  return `<section class="print-only"><h2>${esc(MONTHS_EN[m])} ${y} · ${esc(MONTHS_TA[m])}</h2>
    <p>${esc(state.place.nameEn)} · Tamizh Panchangam</p>
    <table><thead><tr><th>Date</th><th>Day</th><th>Tamil date</th><th>Tithi</th><th>Nakshatram</th><th>Notes</th></tr></thead><tbody>${rows}</tbody></table></section>`;
}

// ---- festivals & holidays (next 120 days)
/** Next occurrences (within a year) of festivals or observances whose name matches the query. */
function findResults(q) {
  const needle = q.trim().toLowerCase();
  if (needle.length < 2) return '';
  const today = todayEpoch();
  const seen = new Set();
  const hits = [];
  for (const d of daysInRange(today, today + 365)) {
    for (const e of d.events) {
      if (e.kind === 'MUHURTHAM') continue;
      if (!(e.titleEn.toLowerCase().includes(needle) || e.titleTa.includes(q.trim()))) continue;
      if (seen.has(e.date + e.titleEn)) continue;
      seen.add(e.date + e.titleEn);
      hits.push({ epoch: d.epochDay, e });
    }
    if (hits.length >= 8) break;
  }
  if (!hits.length) return `<p class="small">${esc(lb('இந்த ஆண்டில் இல்லை', 'No occurrence in the next year'))}</p>`;
  return `<ul class="events">${hits.map(({ epoch, e }) => `<li class="event"><time>${esc(dateShort(e.date))}</time>
      <span><button class="result" data-act="pick-day" data-d="${epoch}"><b>${esc(lb(e.titleTa, e.titleEn))}</b></button></span></li>`).join('')}</ul>`;
}

function viewFestivals() {
  const start = todayEpoch();
  const kinds = ['ALL', 'FESTIVAL', 'VIRATHAM', 'GOVT_HOLIDAY', 'RELIGIOUS', 'SCHOOL'];
  const filters = kinds.map((k) => `<button class="pill${state.kindFilter === k ? ' on' : ''}" data-act="kind" data-k="${k}">${esc(k === 'ALL' ? lb('அனைத்தும்', 'All') : kindLabel(k))}</button>`).join('');
  // One entry per observance and date (the same one can come from two sources), then consecutive days
  // of the same observance (school breaks, multi-day festivals) merge into one range, as in the APK.
  // Keyed by the Tamil title: school calendars name the same break differently per district (Winter Break / Winter
  // Vacation), and the Tamil names agree. A school closure on a government holiday is already listed as the holiday.
  const seen = new Set();
  const entries = [];
  for (const d of daysFrom(start, 120)) {
    const govt = d.events.some((x) => x.kind === 'GOVT_HOLIDAY');
    for (const e of d.events) {
      if (e.kind === 'MUHURTHAM') continue;
      if (state.kindFilter !== 'ALL' && e.kind !== state.kindFilter) continue;
      if (state.kindFilter === 'ALL' && e.kind === 'SCHOOL' && govt) continue;
      // One school closure per day, whatever the district calls it (Winter Break and Winter Vacation are one).
      const key = e.kind === 'SCHOOL' ? `${e.date}|SCHOOL` : e.date + e.titleTa;
      if (seen.has(key)) continue;
      seen.add(key);
      entries.push(e);
    }
  }
  const runs = [];
  for (const e of entries) {
    const last = runs[runs.length - 1];
    if (last && last.titleEn === e.titleEn && last.kind === e.kind && last.end === dayBefore(e.date)) last.end = e.date;
    else runs.push({ ...e, end: e.date });
  }
  const cardFor = (r) => {
    const religion = r.religion && r.religion !== 'HINDU' ? ` · ${lb(...CONTENT.enums.Religion.find((x) => x[0] === r.religion).slice(1, 3))}` : '';
    return `<article class="fcard ${r.isLocalHoliday ? 'hol' : kindCls(r.kind)}">${dateBadge(parseIso(r.date), r.end !== r.date ? parseIso(r.end) : null)}
      <div class="fcard-body"><b>${esc(lb(r.titleTa, r.titleEn))}</b><small>${esc(kindLabel(r.kind))}${esc(religion)}</small></div></article>`;
  };
  const body = groupByMonth(runs, (r) => parseIso(r.date)).map((g) => monthSection(g, cardFor)).join('');
  return `<h1 class="page">${esc(lb('விழாக்கள் & விடுமுறைகள்', 'Festivals & holidays'))}</h1>
    <div class="search"><input id="find" type="search" placeholder="${esc(lb('விழாவின் பெயர் தேடுக', 'Find a festival, e.g. Pongal'))}" value="${esc(state.findQuery)}" autocomplete="off"></div>
    <div id="find-results">${findResults(state.findQuery)}</div>
    <div class="row wrap">${filters}</div>
    <div class="stat-row"><div><b>${runs.length}</b><small>${esc(lb('நிகழ்வுகள்', 'entries'))}</small></div></div>
    ${body || `<p class="small">${esc(lb('இல்லை', 'Nothing in this period'))}</p>`}`;
}
const kindLabel = (k) => ({ FESTIVAL: ['விழா', 'Festival'], VIRATHAM: ['விரதம்', 'Viratham'], GOVT_HOLIDAY: ['விடுமுறை', 'Holiday'], RELIGIOUS: ['மதம்', 'Religious'], SCHOOL: ['பள்ளி', 'School'] })[k].join(' · ');
const dayBefore = (iso) => isoOf(parseIso(iso) - 1);
const dateShort = (iso) => new Date(`${iso}T00:00:00Z`).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short', timeZone: 'UTC' });
// Card helpers for the Festivals, Important and Muhurtham pages.
const weekdayShortOf = (epoch) => CONTENT.names.weekdays[dayOfWeek(epoch)][1].slice(0, 3);
const dateBadge = (epoch, end = null) => `<div class="date-badge"><b>${dayOfMonth(epoch)}${end ? `–${dayOfMonth(end)}` : ''}</b><small>${esc(weekdayShortOf(epoch))}</small></div>`;
const monthTitle = (epoch) => `${esc(MONTHS_TA[monthOf(epoch) - 1])} ${yearOf(epoch)} · ${MONTHS_EN[monthOf(epoch) - 1]}`;
/** Rows (in date order) grouped into consecutive runs of the same month. */
const groupByMonth = (rows, epochOf) => {
  const groups = [];
  for (const r of rows) {
    const e = epochOf(r);
    const key = `${yearOf(e)}-${monthOf(e)}`;
    const g = groups[groups.length - 1];
    if (g && g.key === key) g.items.push(r);
    else groups.push({ key, epoch: e, items: [r] });
  }
  return groups;
};
const monthSection = (g, render) => `<section class="month-group"><h2 class="month-h">${monthTitle(g.epoch)}<small>${g.items.length}</small></h2><div class="cards-grid">${g.items.map(render).join('')}</div></section>`;
const kindCls = (k) => ({ FESTIVAL: 'fest', VIRATHAM: 'fest', GOVT_HOLIDAY: 'hol', RELIGIOUS: 'rel', SCHOOL: 'sch' })[k] || '';

// ---- important dates (next 60 days)
function viewImportant() {
  const start = todayEpoch();
  const items = daysFrom(start, 60).map((d) => ({ d, list: importantDates(d) })).filter((x) => x.list.length);
  const total = items.reduce((n, x) => n + x.list.length, 0);
  const groups = groupByMonth(items, (x) => x.d.epochDay);
  const card = ({ d, list }) => `<article class="fcard${list.some((x) => x.holiday) ? ' hol' : ' fest'}">${dateBadge(d.epochDay)}
    <div class="fcard-body">${list.map((x) => `<b class="${x.holiday ? 'alert-text' : ''}">${esc(lbo(x.title))}</b>`).join('')}</div></article>`;
  return `<h1 class="page">${esc(lb('முக்கிய நாட்கள்', 'Important dates'))}</h1>
    <p class="page-sub">${esc(lb('அடுத்த 60 நாட்கள் · பண்டிகைகள், விரதங்கள், விடுமுறைகள்', 'Next 60 days · festivals, observances and holidays'))}</p>
    <div class="stat-row"><div><b>${items.length}</b><small>${esc(lb('நாட்கள்', 'days'))}</small></div><div><b>${total}</b><small>${esc(lb('நிகழ்வுகள்', 'items'))}</small></div></div>
    ${groups.map((g) => monthSection(g, card)).join('') || `<p class="small">${esc(lb('இல்லை', 'Nothing in this period'))}</p>`}`;
}

// ---- subha muhurtham: days in a chosen range (default: next 120 days), with the best window
function viewMuhurtham() {
  const today = todayEpoch();
  const from = state.mhFrom ?? today;
  const to = Math.min(state.mhTo ?? today + 120, from + 366);
  const items = [];
  for (const d of daysInRange(from, to)) {
    const mh = d.events.find((e) => e.kind === 'MUHURTHAM');
    if (mh) items.push({ d, mh });
  }
  const card = ({ d, mh }) => {
    const best = d.timings.nallaNeram.find((w) => secOf(w.start) < 12 * 3600) || d.timings.nallaNeram[0];
    return `<article class="fcard mh">${dateBadge(d.epochDay)}
      <div class="fcard-body"><b>${esc(lb(mh.titleTa, mh.titleEn))}</b>
        <small>${esc(lb(...CONTENT.names.nakshatras[d.panchangam.nakshatram.index]))} · ${esc(lb(d.panchangam.tithi.nameTa, d.panchangam.tithi.nameEn))}</small>
        ${best ? `<span class="best-window">${esc(lb('சிறந்த நேரம்', 'Best window'))} ${timeOf(best.start)} – ${timeOf(best.end)}</span>` : ''}</div></article>`;
  };
  const groups = groupByMonth(items, (x) => x.d.epochDay);
  return `<h1 class="page">${esc(lb('சுப முகூர்த்த நாட்கள்', 'Subha Muhurtham'))}</h1>
    <p class="page-sub">${esc(lb('ஆடி, புரட்டாசி, மார்கழி மாதங்கள் தவிர்க்கப்படும்; செவ்வாய், சனி இல்லை.', 'Aadi, Purattasi and Margazhi are skipped; no Tuesdays or Saturdays.'))}</p>
    <section class="group">
      <div class="row wrap">
        <label class="field">${esc(lb('இருந்து', 'From'))} <input type="date" data-mh="from" value="${isoOf(from)}"></label>
        <label class="field">${esc(lb('வரை', 'To'))} <input type="date" data-mh="to" value="${isoOf(to)}"></label>
      </div>
    </section>
    <div class="stat-row"><div><b>${items.length}</b><small>${esc(lb('முகூர்த்த நாட்கள்', 'muhurtham days'))}</small></div></div>
    ${groups.map((g) => monthSection(g, card)).join('') || `<p class="small">${esc(lb('இந்த காலத்தில் முகூர்த்த நாட்கள் இல்லை', 'No muhurtham days in this period'))}</p>`}`;
}

// ---- rasi palan, chart and gochara
/** Tile position of each rasi on the 4×4 chart (row, column), as in the APK's RasiPalanScreen. */
const RASI_POS = { 11: [0, 0], 0: [0, 1], 1: [0, 2], 2: [0, 3], 10: [1, 0], 3: [1, 3], 9: [2, 0], 4: [2, 3], 8: [3, 0], 7: [3, 1], 6: [3, 2], 5: [3, 3] };
/** Full planet names for the chart (Tamil, English). */
const GRAHA_NAME = { sun: ['சூரியன்', 'Sun'], moon: ['சந்திரன்', 'Moon'], jupiter: ['குரு', 'Jupiter'], saturn: ['சனி', 'Saturn'], rahu: ['ராகு', 'Rahu'], ketu: ['கேது', 'Ketu'] };

function viewRasi() {
  const d = dayData(state.day) || dayData(todayEpoch());
  const palan = rasiPalan(d.panchangam.moonRasi);
  const byRasi = new Map(palan.map((p) => [p.rasi, p]));
  const sky = skyPositions(d.epochDay, state.place.zoneId);
  const nav = `<div class="bar"><button class="nav" data-act="day" data-d="-1" aria-label="Previous day">‹</button>
    <h1>${esc(lb('ராசி பலன்', 'Rasi Palan'))} · ${esc(dateLong(d.epochDay))}</h1><button class="nav" data-act="day" data-d="1" aria-label="Next day">›</button></div>
    <p class="small center">${esc(lb('ஒரு ராசியைத் தட்டுக', 'Tap a rasi for today, month and year'))}</p>`;
  const moonRasi = d.panchangam.moonRasi;
  const ashtama = palan.find((p) => p.outlook === 'CHANDRASHTAMAM');
  const rasiName = (r) => esc(lb(...CONTENT.names.rasis[r]));
  const tiles = Object.entries(RASI_POS).map(([r, [row, col]]) => {
    const rasi = Number(r);
    const p = byRasi.get(rasi);
    const planets = Object.keys(GRAHA_NAME).filter((g) => sky[g] === rasi);
    return `<button class="rasi-tile o-${p.outlook.toLowerCase()}${rasi === moonRasi ? ' is-moon' : ''}" style="grid-row:${row + 1};grid-column:${col + 1}" data-act="rasi-open" data-r="${r}">
      <span class="rt-name">${esc(CONTENT.names.rasis[rasi][0])}</span>
      ${state.settings.tamilOnly ? '' : `<small>${esc(CONTENT.names.rasis[rasi][1])}</small>`}
      <span class="rt-outlook">${esc(outlookWords(p.outlook)[0])}</span>
      <span class="rt-planets">${planets.map((g) => `<em>${esc(lb(...GRAHA_NAME[g]))}</em>`).join('')}</span></button>`;
  }).join('');
  // The centre panel: today's positions as labelled rows.
  const row = (label, value, cls = '') => `<div class="${cls}"><dt>${esc(label)}</dt><dd>${value}</dd></div>`;
  const centre = `<section class="rasi-centre" style="grid-row:2 / span 2;grid-column:2 / span 2">
    <h3>${esc(lb('ராசி சக்கரம்', 'Rasi chart'))}</h3>
    <small>${esc(lb('இன்றைய கிரக நிலை', "Today's planetary positions"))}</small>
    <dl class="rc-rows">
      ${row(lb('சந்திரன்', 'Moon'), rasiName(moonRasi))}
      ${row(lb('குரு', 'Jupiter'), rasiName(sky.jupiter))}
      ${row(lb('சனி', 'Saturn'), rasiName(sky.saturn))}
      ${row(lb('ராகு', 'Rahu'), rasiName(sky.rahu))}
      ${row(lb('கேது', 'Ketu'), rasiName(sky.ketu))}
      ${ashtama ? row(lb('சந்திராஷ்டமம்', 'Chandrashtamam'), rasiName(ashtama.rasi), 'rc-alert') : ''}
    </dl>
  </section>`;
  return `${nav}<div class="rasi-grid">${tiles}${centre}</div>${state.rasi !== null ? rasiModal(d, byRasi.get(state.rasi), sky) : ''}`;
}

/** The expanded rasi (APK's RasiDetail): Today, Month and Year tabs with read-aloud. */
function rasiModal(d, p, sky) {
  const g = gochara(p.rasi, sky);
  const tabs = [
    { ta: 'இன்று', en: 'Today', text: [p.messageTa, p.messageEn], outlook: p.outlook, note: '' },
    { ta: 'மாதம்', en: 'Month', text: [g.month.ta, g.month.en], outlook: g.month.outlook, note: 'சந்திர ராசியிலிருந்து கோசாரம் · Rule-based gochara from current planetary transits' },
    { ta: 'ஆண்டு', en: 'Year', text: [g.year.ta, g.year.en], outlook: g.year.outlook, note: 'சந்திர ராசியிலிருந்து கோசாரம் · Rule-based gochara from current planetary transits' },
  ][state.rasiTab];
  const tabButtons = [0, 1, 2].map((t) => {
    const label = lb(...[['இன்று', 'Today'], ['மாதம்', 'Month'], ['ஆண்டு', 'Year']][t]);
    return `<button class="pill${state.rasiTab === t ? ' on' : ''}" data-act="rasi-tab" data-t="${t}">${esc(label)}</button>`;
  }).join('');
  return `<div class="rasi-modal" role="dialog" aria-modal="true" aria-label="${esc(lb(...CONTENT.names.rasis[p.rasi]))}">
    <div class="rasi-card">
      <div class="bar"><h2>${esc(lb(...CONTENT.names.rasis[p.rasi]))}</h2>
        <span class="chip ${tabs.outlook.toLowerCase()}">${esc(lb(...outlookWords(tabs.outlook)))}</span>
        <button class="nav" data-act="rasi-close" aria-label="Close">✕</button></div>
      <div class="row wrap">${tabButtons}</div>
      <p class="pre">${esc(lb(...tabs.text))}</p>
      ${tabs.note ? `<p class="small">${esc(tabs.note)}</p>` : ''}
      <button class="pill" data-act="rasi-read">🔊 ${esc(lb('வாசி', 'Read aloud'))}</button>
    </div></div>`;
}
const outlookWords = (o) => ({ GOOD: ['நன்மை', 'Good'], NEUTRAL: ['சமம்', 'Neutral'], CAUTION: ['கவனம்', 'Caution'], CHANDRASHTAMAM: ['சந்திராஷ்டமம்', 'Chandrashtamam'] })[o];

// ---- location
function viewLocation() {
  const s = state.settings;
  const presets = PLACES.map((p) => `<button class="place-card${!s.place.custom && s.place.preset === p.key ? ' on' : ''}" data-act="place-preset" data-key="${p.key}"><b>${esc(lb(p.nameTa, p.nameEn))}</b><small>${esc(p.zoneId)}</small></button>`).join('');
  const recent = s.recentPlaces.map((p, i) => `<button class="pill" data-act="place-recent" data-i="${i}">${esc(p.nameEn)}</button>`).join('');
  const pinned = s.pinnedPlaces.map((p) => `<li class="event pin-row"><span><b>${esc(p.label)}</b><br><small>${esc(lb('ஆஃப்லைன் ஆண்டுகள்', 'Offline years'))}: ${packYears.map((y) => `<button class="pill small-pill" data-act="pack-save" data-year="${y}" data-id="${esc(p.id)}">${y}</button>`).join(' ')}</small></span>
      <span class="pin-actions"><button class="pill" data-act="pin-open" data-id="${esc(p.id)}">${esc(lb('திற', 'Open'))}</button><button class="pill" data-act="pin-remove" data-id="${esc(p.id)}">✕</button></span></li>`).join('');
  const pinHere = s.pinnedPlaces.some((p) => p.id === placeKeyOf(s)) ? '' : `<button class="pill" data-act="pin-current">📌 ${esc(lb('இந்த இடத்தைப் பின் செய்', 'Pin this place'))}</button>`;
  const results = state.search.results.map((r, i) => `<li><button class="result" data-act="place-result" data-i="${i}"><b>${esc(r.name)}</b><small>${esc([r.admin1, r.country].filter(Boolean).join(', '))}</small></button></li>`).join('');
  return `<h1 class="page">${esc(lb('இடம்', 'Location'))}</h1>
    <p class="page-sub">${esc(lb('எல்லாக் கணக்குகளும் இங்கே தேர்ந்தெடுக்கும் இடத்திற்கு.', 'Every calculation uses the place you choose here.'))}</p>
    <div class="current-place"><span>${esc(lb('தற்போதைய இடம்', 'Current place'))}</span><b>${esc(placeLabel(state.place))}</b><small>${esc(state.place.zoneId)}</small></div>
    <h3 class="sub-h">${esc(lb('இடங்கள்', 'Places'))}</h3>
    <div class="place-grid">${presets}</div>
    <div class="row"><button class="pill" data-act="place-auto">📍 ${esc(lb('என் இடம்', 'Use my location'))}</button></div>
    <form id="search-form" class="search"><input id="search" type="search" placeholder="${esc(lb('நகரம் தேடுக', 'Search a city'))}" value="${esc(state.search.query)}" autocomplete="off"><button class="pill" type="submit">${esc(lb('தேடு', 'Search'))}</button></form>
    ${state.search.busy ? `<p class="small">${esc(lb('தேடுகிறது…', 'Searching…'))}</p>` : ''}
    ${state.search.error ? `<p class="small error">${esc(state.search.error)}</p>` : ''}
    <ul class="results">${results}</ul>
    ${recent ? `<h3>${esc(lb('சமீபத்தியவை', 'Recent'))}</h3><div class="row wrap">${recent}</div>` : ''}
    ${compareHtml()}
    <section class="group"><h3>${esc(lb('பின் செய்யப்பட்ட இடங்கள் & ஆஃப்லைன்', 'Pinned places & offline years'))}</h3>
      <p class="small">${esc(lb('ஒரு ஆண்டை சேமித்தால் இணைய இணைப்பு இல்லாமலும் வேகமாகத் திறக்கும்.', 'Save a year to open it quickly, and see the last forecast, without a connection.'))}</p>
      ${pinHere}
      <ul class="events">${pinned || `<li class="small">${esc(lb('இன்னும் இல்லை', 'Nothing pinned yet'))}</li>`}</ul>
      <label class="toggle"><input type="checkbox" data-setting="autoLocate" ${s.autoLocate ? 'checked' : ''}> ${esc(lb('தொடங்கும்போது என் இடம்', 'Use my location when the app opens'))}</label>
    </section>`;
}

/** Compare the same day in the open place and another place: times are each place's local time. */
function compareHtml() {
  const here = state.placeKey;
  const day = state.cmpDay ?? todayEpoch();
  const other = PLACES.find((p) => p.key === state.cmpKey) || PLACES.find((p) => p.key !== state.settings.place.preset) || PLACES[0];
  const a = daysBetween(day, day)[0];
  const b = monthDays(other, day, day, { convention: timingConvention(other), schoolJson: state.school })[0];
  const rahu = (d) => `${timeOf(d.timings.rahuKalam.start)} – ${timeOf(d.timings.rahuKalam.end)}`;
  const rows = [
    [lb('உதயம்', 'Sunrise'), timeOf(a.sunrise), timeOf(b.sunrise)],
    [lb('அஸ்தமனம்', 'Sunset'), timeOf(a.sunset), timeOf(b.sunset)],
    [lb('திதி', 'Tithi'), lb(a.panchangam.tithi.nameTa, a.panchangam.tithi.nameEn), lb(b.panchangam.tithi.nameTa, b.panchangam.tithi.nameEn)],
    [lb('நட்சத்திரம்', 'Nakshatram'), lb(a.panchangam.nakshatram.nameTa, a.panchangam.nakshatram.nameEn), lb(b.panchangam.nakshatram.nameTa, b.panchangam.nakshatram.nameEn)],
    [lb('ராகு காலம்', 'Rahu Kalam'), rahu(a), rahu(b)],
  ];
  const opts = PLACES.filter((p) => p.key !== here).map((p) => `<option value="${p.key}"${p.key === other.key ? ' selected' : ''}>${esc(lb(p.nameTa, p.nameEn))}</option>`).join('');
  return `<section class="group compare">
    <h3>${esc(lb('ஒப்பீடு', 'Compare places'))}</h3>
    <div class="row wrap">
      <label class="field">${esc(lb('இன்னொரு இடம்', 'Other place'))} <select data-cmp="place">${opts}</select></label>
      <label class="field">${esc(lb('நாள்', 'Day'))} <input type="date" data-cmp="day" value="${isoOf(day)}"></label>
    </div>
    <div class="cmp-grid">
      <span></span><b>${esc(placeLabelShort(state.place))}</b><b>${esc(placeLabelShort(other))}</b>
      ${rows.map(([l, x, y]) => `<span class="cmp-l">${esc(l)}</span><span>${esc(x)}</span><span>${esc(y)}</span>`).join('')}
    </div>
    <p class="small">${esc(lb('ஒவ்வொரு இடத்தின் உள்ளூர் நேரம்.', "Each column is that place's local time."))}</p>
  </section>`;
}
const placeLabelShort = (p) => p.nameEn;

// ---- theme and skin
const SKINS = [
  ['CLASSIC', 'பாரம்பரியம்', 'Classic', 'Solid cards with soft shadows.'],
  ['GLASS', 'கண்ணாடி', 'Glass', 'Frosted, translucent cards over the background.'],
  ['PAPER', 'காகிதம்', 'Paper', 'Flat cards with a crisp outline, like a printed almanac.'],
  ['SOFT', 'மென்மை', 'Soft', 'Rounder cards with a gentle lift and a hint of the accent colour.'],
  ['EDGE', 'ஓரம்', 'Edge', 'Flat cards with a coloured edge on the left.'],
];
function viewTheme() {
  const cur = state.settings.theme;
  const cards = CONTENT.themes.map((t) => `<button class="theme-card${cur === t.id ? ' on' : ''}" data-act="theme" data-id="${t.id}"
      style="background:linear-gradient(165deg, ${t.colors.background}, ${t.colors.backgroundMid} 55%, ${t.colors.backgroundDeep});color:${t.colors.text}">
    <span class="swatches"><span style="background:${t.colors.accent}"></span><span style="background:${t.colors.alert}"></span><span style="background:${t.colors.amber}"></span></span>
    <b>${esc(lb(t.ta, t.en))}</b></button>`).join('');
  const skins = SKINS.map(([id, ta, en, desc]) => `<button class="skin-card${state.settings.skin === id ? ' on' : ''}" data-act="skin" data-id="${id}">
    <span class="skin-sample skin-${id.toLowerCase()}"><i></i><i></i></span>
    <b>${esc(lb(ta, en))}</b><small>${esc(desc)}</small></button>`).join('');
  return `<h1 class="page">${esc(lb('தீம் & தோற்றம்', 'Theme & skin'))}</h1>
    <p class="page-sub">${esc(lb('வண்ணங்களும் அட்டைகளின் தோற்றமும். தேர்வு நினைவில் வைக்கப்படும்.', 'Colours and card style. Your choice is remembered.'))}</p>
    <section class="group"><h3>${esc(lb('வண்ணத் தீம்', 'Colour theme'))}</h3><div class="theme-grid">${cards}</div></section>
    <section class="group"><h3>${esc(lb('தோற்றம் (skin)', 'Skin · card style'))}</h3><div class="skin-grid">${skins}</div></section>`;
}

// ---- settings
function viewSettings() {
  const s = state.settings;
  const check = (key, label) => `<label class="toggle"><input type="checkbox" data-setting="${key}" ${s[key] ? 'checked' : ''}> ${esc(label)}</label>`;
  const select = (key, label, opts) => `<label class="field">${esc(label)} <select data-setting="${key}">${opts.map(([v, t]) => `<option value="${v}"${String(s[key]) === v ? ' selected' : ''}>${esc(t)}</option>`).join('')}</select></label>`;
  const faiths = CONTENT.enums.Religion.filter((r) => r[0] !== 'HINDU').map((r) => `<label class="toggle"><input type="checkbox" data-faith="${r[0]}" ${s.hiddenFaiths.includes(r[0]) ? '' : 'checked'}> ${esc(lb(r[1], r[2]))}</label>`).join('');
  const tabs = TABS.filter(([id]) => !ALWAYS_SHOWN.has(id)).map(([id, ta, en]) => `<label class="toggle"><input type="checkbox" data-section="${id}" ${s.hiddenSections.includes(id) ? '' : 'checked'}> ${esc(lb(ta, en))}</label>`).join('');
  const m = s.music;
  const musicSel = (k, label, opts) => `<label class="field">${esc(label)} <select data-music="${k}">${opts.map(([v, t]) => `<option value="${v}"${m[k] === v ? ' selected' : ''}>${esc(t)}</option>`).join('')}</select></label>`;
  return `<h1 class="page">${esc(lb('அமைப்புகள்', 'Settings'))}</h1>
    <p class="page-sub">${esc(lb('மாற்றங்கள் உடனே நடைமுறைக்கு வந்து சேமிக்கப்படும்.', 'Changes apply straight away and are saved on this device.'))}</p>
    <div class="settings-grid">
    <section class="group"><h3>${esc(lb('காட்சி', 'Display'))}</h3>
      ${check('weather', lb('வானிலை', 'Weather'))}${check('animations', lb('அனிமேஷன்', 'Animations'))}${check('weatherAnimations', lb('வானிலை அனிமேஷன்', 'Weather animations'))}
      ${check('clock24h', lb('24 மணி நேரம்', '24-hour clock'))}${check('tamilOnly', lb('தமிழ் மட்டும்', 'Tamil only'))}
      ${select('units', lb('அளவுகள்', 'Units'), [['AUTO', lb('தானியங்கி', 'Auto')], ['METRIC', '°C · km/h'], ['IMPERIAL', '°F · mph']])}
    </section>
    <section class="group"><h3>${esc(lb('நாட்காட்டி', 'Calendar'))}</h3>
      <h3>${esc(lb('பிறப்பு விவரம்', 'Birth details'))}</h3>
      <p class="small">${esc(lb('தாரா பலன் மற்றும் உங்கள் சந்திராஷ்டமத்திற்கு. இந்த சாதனத்தில் மட்டும் சேமிக்கப்படும்.', 'For Thara Balam and your Chandrashtamam on Today. Stored on this device only.'))}</p>
      ${select('janmaStar', lb('பிறந்த நட்சத்திரம்', 'Birth star'), [['-1', '—']].concat(CONTENT.names.nakshatras.map((n, i) => [String(i), lb(...n)])))}
      ${select('janmaRasi', lb('பிறந்த ராசி', 'Birth Rasi'), [['-1', '—']].concat(CONTENT.names.rasis.map((r, i) => [String(i), lb(...r)])))}
      ${select('convention', lb('ராகு காலம் கணக்கு', 'Rahu Kalam convention'), [['AUTO', lb('தானியங்கி', 'Auto')], ['TRADITIONAL_6AM', lb('காலை 6 – மாலை 6', 'Fixed 6 AM – 6 PM')], ['SUNRISE_BASED', lb('சூரிய உதயம்', 'Local sunrise–sunset')]])}
      ${check('usHolidays', lb('அமெரிக்க / இல்லினாய் விடுமுறைகள்', 'US & Illinois holidays'))}
      ${check('schoolCalendars', lb('பள்ளி நாட்காட்டி', 'School calendars'))}
      ${check('tamilNaduHolidays', lb('தமிழ்நாடு அரசு விடுமுறைகள்', 'Tamil Nadu govt. holidays'))}
      <p class="small">${esc(lb('மதங்கள் (இந்து தவிர்த்து)', 'Faiths (other than Hindu)'))}</p>${faiths}
    </section>
    <section class="group"><h3>${esc(lb('பார்க்கும் பகுதிகள்', 'Sections shown'))}</h3>${tabs}</section>
    <section class="group"><h3>${esc(lb('ஆம்பியன்ட் & ஸ்லோகம்', 'Ambient & sloka'))}</h3>
      ${check('ambient', lb('ஓய்வு நேர கடிகாரம்', 'Ambient clock when idle'))}
      ${select('ambientMinutes', lb('எத்தனை நிமிடங்களுக்குப் பின்', 'After how many minutes'), [[1, '1'], [2, '2'], [5, '5'], [10, '10']])}
      ${check('slokaMusic', lb('ஸ்லோகத்துடன் இசை', 'Music with sloka'))}
      ${check('slokaVoice', lb('ஸ்லோகத்தை வாசிக்கவும்', 'Read the sloka aloud when Today opens'))}
      ${musicSel('raga', lb('ராகம்', 'Raga'), [['MOHANAM', 'Mohanam'], ['HINDOLAM', 'Hindolam'], ['REVATHI', 'Revathi']])}
      ${musicSel('tempo', lb('வேகம்', 'Tempo'), [['SLOW', lb('மெதுவாக', 'Slow')], ['MEDIUM', lb('நடுத்தரம்', 'Medium')]])}
      ${musicSel('mix', lb('தம்புரா / புல்லாங்குழல்', 'Tanpura / flute'), [['TANPURA', lb('தம்புரா அதிகம்', 'More tanpura')], ['BALANCED', lb('சமநிலை', 'Balanced')], ['FLUTE', lb('புல்லாங்குழல் அதிகம்', 'More flute')]])}
      ${musicSel('bell', lb('மணி', 'Bell'), [['OFF', lb('இல்லை', 'Off')], ['SOFT', lb('மெல்லிய', 'Soft')], ['NORMAL', lb('சாதாரண', 'Normal')], ['PROMINENT', lb('தெளிவான', 'Prominent')]])}
      ${musicSel('pitch', lb('சுருதி', 'Pitch'), [['C', 'C'], ['D', 'D'], ['E', 'E'], ['F', 'F'], ['G', 'G']])}
    </section>
    <section class="group"><h3>${esc(lb('நினைவூட்டல்கள்', 'Reminders'))}</h3>
      ${check('rahuReminder', lb('ராகு காலத்திற்கு 10 நிமிடம் முன்', '10 minutes before Rahu Kalam'))}
      ${check('nallaReminder', lb('நல்ல நேரம் தொடங்கும்போது', 'When Nalla Neram begins'))}
      ${reminderNote()}
    </section>
    <section class="group"><h3>${esc(lb('தரவு', 'Data'))}</h3><button class="pill" data-act="reset-settings">${esc(lb('இயல்புநிலைக்கு மீட்டமை', 'Reset to defaults'))}</button></section>
    </div>`;
}

// ---- about
const RELATED_URL = 'https://thiruppugazh-ui.vercel.app/';

/** True on iPhone/iPad Safari, the only browser that can add this page to the home screen. */
function onIosSafari() {
  const ua = navigator.userAgent;
  return /iPhone|iPad|iPod/.test(ua) && /Safari/.test(ua) && !/CriOS|FxiOS|EdgiOS|OPiOS/.test(ua);
}
function installedAsApp() {
  return window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;
}

function viewAbout() {
  const status = installedAsApp()
    ? lb('✓ இந்த சாதனத்தில் ஏற்கனவே நிறுவப்பட்டுள்ளது', '✓ Already installed on this device')
    : onIosSafari()
      ? lb('✓ இப்போது Safari-ல் திறந்துள்ளீர்கள் — கீழே உள்ள படிகளைப் பின்பற்றவும்', "✓ You're in Safari — follow the steps below")
      : lb('⚠ iPhone நிறுவலுக்கு Safari-ல் திறக்கவும். Chrome போன்ற உலாவிகளில் நிறுவ முடியாது.', '⚠ To install on iPhone, open this page in Safari. Chrome and other browsers cannot install it.');
  return `<h1 class="page">${esc(lb('பற்றி', 'About'))}</h1>
    <section class="about-hero">
      <img src="logo.svg" alt="" width="56" height="56">
      <div><h2>${esc(lb('தமிழ் பஞ்சாங்கம்', 'Tamizh Panchangam'))}</h2>
      <p class="small">${esc(lb('பஞ்சாங்கம், ராசி பலன், முகூர்த்தம் — ஒரு இடத்துக்கான கணக்கீடு.', 'Panchangam, Rasi Palan and Muhurtham, calculated for your place.'))}</p>
      <small>Version ${esc(APP_VERSION)}</small></div>
    </section>
    <section class="detail install">
      <h3>${esc(lb('iPhone-ல் நிறுவுக', 'Install on iPhone'))}</h3>
      <p class="small">${esc(status)}</p>
      <ol class="steps">
        <li>${esc(lb('Safari-ல் இந்தப் பக்கத்தைத் திறக்கவும்.', 'Open this page in Safari.'))}</li>
        <li>${esc(lb('கீழே உள்ள Share (□↑) பொத்தானைத் தட்டவும்.', 'Tap the Share button (□ with an arrow) at the bottom.'))}</li>
        <li>${esc(lb('கீழே உருட்டி "Add to Home Screen" தட்டவும்.', 'Scroll down and tap "Add to Home Screen".'))}</li>
        <li>${esc(lb('"Add" தட்டவும். முகப்புத் திரையில் முழுத் திரை செயலியாகத் திறக்கும்.', 'Tap "Add". It opens full screen from your home screen.'))}</li>
      </ol>
    </section>
    <section class="detail">
      <h3>${esc(lb('தொடர்புடைய தளம் · Related', 'Related site'))}</h3>
      <p>${esc(lb('திருப்புகழ் பாடல்களை இங்கே படிக்கலாம்.', 'Read Thiruppugazh hymns and their meanings here.'))}</p>
      <p><a class="pill on" href="${RELATED_URL}" target="_blank" rel="noopener">Thiruppugazh · thiruppugazh-ui.vercel.app ↗</a></p>
    </section>
    <section class="detail">
      <p class="small">Version ${esc(APP_VERSION)}</p>
    </section>
    <section class="detail">
      <p>${esc(lb('இந்த நாட்காட்டியின் கணக்குகள் இந்த சாதனத்திலேயே நடக்கின்றன. சூரியன், சந்திரன் நிலைகள் Meeus நுட்பங்களிலிருந்து; லஹிரி அயனாம்சம்.', 'Every calculation runs on this device from Sun and Moon positions (Meeus algorithms, Lahiri ayanamsa). The engine matches the Android TV app.'))}</p>
      <p class="small">${esc(lb('விடுமுறை நாட்கள் அரசுப் பட்டியல்களைப் பொறுத்தது; இஸ்லாமிய நாட்கள் பிறை பார்த்தலால் ஒரு நாள் மாறலாம்.', 'Holidays follow official lists where known. Islamic dates are calculated and local moon sighting may differ by a day.'))}</p>
      <p class="small">${esc(lb('ஸ்லோகங்கள் பொது உரிமையுள்ள பாரம்பரிய நூல்கள்.', 'All sloka texts are public-domain classics.'))}</p>
      <button class="pill" data-act="disclaimer">${esc(lb('மறுப்பு அறிவிப்பு', 'Show disclaimer'))}</button>
    </section>`;
}

// ---------------------------------------------------------------- offline year packs
// A pack is one year of days for one place and timing convention, kept in Cache Storage. The app reads it
// instead of recomputing; without a pack everything is still computed on the device.
// v5: packs saved before the Darsha flag existed are dropped; save the offline years again.
const PACK_CACHE = 'ktc-packs-v5';
const packUrl = (placeId, convention, year) => `pack/${encodeURIComponent(placeId)}/${convention}/${year}.json`;
const packYears = [2026, 2027, 2028];

async function loadPackForCurrent() {
  const id = state.placeKey, conv = conventionOf();
  const token = `${id}|${conv}`;
  state.packKey = token;
  state.packDays = new Map();
  if (!('caches' in window)) return;
  try {
    const cache = await caches.open(PACK_CACHE);
    for (const year of packYears) {
      const hit = await cache.match(packUrl(id, conv, year));
      if (!hit) continue;
      const days = await hit.json();
      if (state.packKey !== token) return; // place changed while loading
      for (const d of days) state.packDays.set(d.epochDay, d);
    }
    state.cache.clear();
    rerender();
  } catch (e) { /* no pack: the app computes */ }
}

/** Saves one year for a pinned place (it does not have to be the open one). */
async function savePack(pin, year, button) {
  const place = pin.place.custom ? pin.place.custom : PLACES.find((x) => x.key === pin.place.preset);
  const conv = state.settings.convention === 'AUTO' ? timingConvention(place) : state.settings.convention;
  const status = (t) => { if (button) button.textContent = t; };
  status(lb('கணக்கிடுகிறது…', 'Calculating…'));
  await new Promise((r) => setTimeout(r, 30)); // let the label paint
  const days = monthDays(place, epochDay(year, 1, 1), epochDay(year, 12, 31), { convention: conv, schoolJson: state.school });
  const cache = await caches.open(PACK_CACHE);
  await cache.put(packUrl(pin.id, conv, year), new Response(JSON.stringify(days), { headers: { 'Content-Type': 'application/json' } }));
  if (pin.id === state.placeKey) await loadPackForCurrent();
  status(lb(`${year} சேமிக்கப்பட்டது ✓`, `${year} saved ✓`));
}

async function packStatus(placeId) {
  if (!('caches' in window)) return {};
  const cache = await caches.open(PACK_CACHE);
  const out = {};
  for (const year of packYears) out[year] = !!(await cache.match(packUrl(placeId, conventionOf(), year)));
  return out;
}

function pinCurrent() {
  const id = placeKeyOf(state.settings);
  if (state.settings.pinnedPlaces.some((p) => p.id === id)) return;
  state.settings.pinnedPlaces = [...state.settings.pinnedPlaces, { id, label: state.place.nameEn, place: state.settings.place }].slice(0, 5);
  saveSettings(state.settings);
  rerender();
}

function unpin(id) {
  state.settings.pinnedPlaces = state.settings.pinnedPlaces.filter((p) => p.id !== id);
  saveSettings(state.settings);
  rerender();
}

// ---------------------------------------------------------------- weather & music & reminders
/** Loads weather once per place and units. Re-rendering happens only when a result arrives, so it can't loop. */
function loadWeatherIfNeeded() {
  if (!state.settings.weather) return;
  const key = `${state.placeKey}|${unitsImperial()}`;
  if (state.weatherKey === key) return;
  state.weatherKey = key;
  state.weather = null;
  state.weatherErr = '';
  const p = state.place;
  const done = () => { if (state.weatherKey === key && state.tab === 'today') rerender(); };
  const lastKey = `ktc.wx.${key}`;
  fetchWeather(p.latitude, p.longitude, unitsImperial())
    .then((w) => {
      try { localStorage.setItem(lastKey, JSON.stringify({ at: Date.now(), w })); } catch (e) { /* not kept */ }
      if (state.weatherKey === key) state.weather = w;
      done();
    })
    .catch(() => {
      // Offline: show the last forecast saved for this place, marked with its age.
      let saved = null;
      try { saved = JSON.parse(localStorage.getItem(lastKey) || 'null'); } catch (e) { saved = null; }
      if (state.weatherKey === key && saved) {
        state.weather = saved.w;
        state.weatherErr = lb(`கடைசியாகப் பெற்றது ${new Date(saved.at).toLocaleString('en-GB', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}`, `Last forecast from ${new Date(saved.at).toLocaleString('en-GB', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}`);
      } else if (state.weatherKey === key) state.weatherErr = lb('வானிலை கிடைக்கவில்லை', 'Weather unavailable (offline?)');
      done();
    });
}

/** With "read aloud" on, the day's verse is read once when Today opens (not again on every re-render). */
function maybeReadSloka() {
  if (!state.settings.slokaVoice) return;
  const key = `${state.day}|${state.placeKey}`;
  if (state.spokenKey === key) return;
  state.spokenKey = key;
  speak(slokaFor(dayData(state.day)));
}

function toggleMusic() {
  if (state.music) {
    state.music.stop();
    state.music = null;
  } else {
    state.music = new TempleMusic(state.settings.music);
    state.music.start();
  }
  rerender();
}

function scheduleToday() {
  const d = dayData(todayEpoch());
  const s = state.settings;
  if (!d || (!s.rahuReminder && !s.nallaReminder)) { clearReminders(); return; }
  scheduleReminders(d.epochDay, state.place.zoneId, d.timings, s, (p) => notifyUser(p, banner));
}

let bannerTimer = null;
function banner(text) {
  const el = $('banner');
  el.textContent = text;
  el.hidden = false;
  clearTimeout(bannerTimer);
  bannerTimer = setTimeout(() => { el.hidden = true; }, 8000);
}

// ---------------------------------------------------------------- ambient clock
function showAmbient(on) {
  state.ambientOn = on;
  $('ambient').hidden = !on;
  if (on) ambientTick();
  keepScreenAwake(on);
}

/** Keeps the screen on while the ambient clock is showing (Wake Lock API; ignored where unsupported). */
async function keepScreenAwake(on) {
  if (!('wakeLock' in navigator)) return;
  try {
    if (on && !state.wakeLock) state.wakeLock = await navigator.wakeLock.request('screen');
    else if (!on && state.wakeLock) { await state.wakeLock.release(); state.wakeLock = null; }
  } catch (e) { /* refused, e.g. low battery or hidden tab */ }
}
function ambientTick() {
  if (!state.ambientOn) return;
  const d = dayData(todayEpoch());
  const now = new Date();
  const nowSec = wallClock(now.getTime(), state.place.zoneId).sec;
  const clock = timeOf(`${String(Math.floor(nowSec / 3600)).padStart(2, '0')}:${String(Math.floor((nowSec % 3600) / 60)).padStart(2, '0')}:00`);
  // The Nalla Neram that is running now, or the next one today (or say tomorrow when the day's are over).
  const nallaNow = d.timings.nallaNeram.find((w) => nowSec >= secOf(w.start) && nowSec < secOf(w.end));
  const nallaNext = d.timings.nallaNeram.find((w) => secOf(w.start) > nowSec);
  const rahu = d.timings.rahuKalam;
  const hour = Math.floor(nowSec / 3600);
  const phase = hour >= 6 && hour < 17 ? 'day' : hour >= 17 && hour < 19 ? 'dusk' : 'night';
  const lamps = Array.from({ length: 10 }, (_, i) => `<i class="lamp" style="left:${(i * 37 + 8) % 96}%;top:${(i * 53 + 20) % 80}%;animation-delay:${(i * 1.3) % 7}s"></i>`).join('');
  const stars = phase === 'night' ? Array.from({ length: 28 }, (_, i) => `<i class="star" style="left:${(i * 29) % 100}%;top:${(i * 43) % 70}%;animation-delay:${(i * 0.7) % 4}s"></i>`).join('') : '';
  $('ambient').className = `ambient ${phase}`;
  $('ambient').innerHTML = `<div class="sky">${stars}${lamps}</div><div class="amb-inner">
    <div class="amb-clock">${clock}</div>
    <div class="amb-date">${esc(dateLong(d.epochDay))}</div>
    <div class="amb-line">${esc(lb(CONTENT.names.months[d.tamilDate.monthIndex][0] + ' ' + d.tamilDate.day, CONTENT.names.months[d.tamilDate.monthIndex][1] + ' ' + d.tamilDate.day))}</div>
    <div class="amb-line">${esc(lb('திதி', 'Tithi'))}: ${esc(lb(d.panchangam.tithi.nameTa, d.panchangam.tithi.nameEn))}</div>
    <div class="amb-line">${nallaNow
      ? `${esc(lb('நல்ல நேரம்', 'Nalla Neram'))} · ${esc(lb('முடிவு', 'ends'))} ${timeOf(nallaNow.end)}`
      : `${esc(lb('அடுத்த நல்ல நேரம்', 'Next Nalla Neram'))}: ${nallaNext ? timeOf(nallaNext.start) : esc(lb('நாளை', 'tomorrow'))}`}</div>
    <div class="amb-line">${esc(lb('ராகு காலம்', 'Rahu Kalam'))}: ${timeOf(rahu.start)} – ${timeOf(rahu.end)}</div>
    ${state.weather ? `<div class="amb-line amb-weather">${Math.round(state.weather.current.temp)}${state.weather.unit} · ${esc(labelOf(state.weather.current.code))}</div>` : ''}
    <div class="amb-tap">${esc(lb('தொடர தொடவும்', 'Tap to return'))}</div></div>`;
}

// ---------------------------------------------------------------- actions
function rerender() {
  const y = $('view');
  const scroll = y ? window.scrollY : 0;
  render();
  window.scrollTo(0, scroll);
}

function applySettingChange(key, value) {
  state.settings[key] = value;
  saveSettings(state.settings);
  if (key === 'rahuReminder' || key === 'nallaReminder') scheduleToday();
  rerender();
}

function setPlace(p, remember = true) {
  if (p.custom) {
    state.settings.place = { custom: p.custom };
    if (remember) {
      const recent = state.settings.recentPlaces.filter((r) => !(r.latitude === p.custom.latitude && r.longitude === p.custom.longitude));
      state.settings.recentPlaces = [p.custom, ...recent].slice(0, 5);
    }
  } else {
    state.settings.place = { preset: p.key };
  }
  saveSettings(state.settings);
  setEngineFromSettings();
  state.rasi = null;
  scheduleToday();
  rerender();
}

function setEngineFromSettings() {
  state.place = resolvePlace();
  state.placeKey = placeKeyOf(state.settings);
  state.day = todayEpoch();
  state.monthOf = null;
  $('place-chip').textContent = state.place.nameEn;
  state.cache.clear();
  loadPackForCurrent();
}

function autoLocate() {
  if (!('geolocation' in navigator)) { state.search.error = lb('இருப்பிடம் கிடைக்கவில்லை', 'Location is not available here'); return rerender(); }
  navigator.geolocation.getCurrentPosition(async (pos) => {
    const { latitude, longitude } = pos.coords;
    const zoneId = Intl.DateTimeFormat().resolvedOptions().timeZone;
    let name = 'Current location', country = '', countryCode = '', admin1 = '';
    try {
      const r = await fetch(`https://api-bdc.net/data/reverse-geocode-client?latitude=${latitude}&longitude=${longitude}&localityLanguage=en`);
      const j = await r.json();
      name = j.city || j.locality || name; country = j.countryName || ''; countryCode = j.countryCode || ''; admin1 = j.principalSubdivision || '';
    } catch (e) { /* keep the default name */ }
    setPlace({ custom: { nameTa: name, nameEn: name, latitude, longitude, zoneId, countryCode, admin1, countryName: country } });
  }, () => { state.search.error = lb('இருப்பிட அனுமதி தேவை', 'Location permission was refused'); rerender(); });
}

async function searchPlaces(raw) {
  const q = raw.trim();
  const token = ++searchToken; // a slower earlier search must not overwrite a newer one
  state.search = { query: q, results: [], error: '', busy: !!q };
  if (!q) return rerender();
  rerender();
  try {
    const found = await geocode(q);
    if (token !== searchToken) return;
    state.search.results = found.map((x) => ({ name: x.name, latitude: x.latitude, longitude: x.longitude, zoneId: x.timezone, countryCode: x.country_code, admin1: x.admin1 || '', country: x.country || '', countryName: x.country || '' }));
    if (!state.search.results.length) {
      state.search.error = /[\u0B80-\u0BFF]/.test(q)
        ? lb('தமிழில் தேட முடியாது. ஆங்கிலத்தில் நகரின் பெயரை எழுதவும் (எ.கா. Chennai)', 'Search by the English name, e.g. Chennai')
        : lb('எதுவும் கிடைக்கவில்லை', 'No matching places');
    }
  } catch (e) {
    if (token !== searchToken) return;
    state.search.error = lb('தேட முடியவில்லை. இணைய இணைப்பை சரிபார்க்கவும்', 'Search failed. Check your internet connection and try again.');
  }
  state.search.busy = false;
  rerender();
}
let searchToken = 0;

/** Place search. Tamil names are often spelt differently in English (Thiruvallur / Tiruvallur), so if the
 *  first spelling finds nothing, the common variants are tried in turn. */
async function geocode(q) {
  const variants = [...new Set([q, q.replace(/^thi/i, 'Ti').replace(/th/gi, 't'), q.replace(/^thi/i, 'Ti')])];
  for (const v of variants) {
    const r = await fetch(`https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(v)}&count=8&language=en&format=json`);
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    const j = await r.json();
    if (j.results && j.results.length) return j.results;
  }
  return [];
}

function onClick(e) {
  if (e.target.classList && e.target.classList.contains('rasi-modal')) return closeRasi();
  const el = e.target.closest('[data-act]');
  if (!el) return;
  const a = el.dataset.act;
  state.lastInput = Date.now();
  if (a === 'tab') { state.tab = el.dataset.tab; rerender(); }
  else if (a === 'today-panel') { state.todayPanel = el.dataset.p; rerender(); }
  else if (a === 'day') { state.day += Number(el.dataset.d); rerender(); }
  else if (a === 'day-today') { state.day = todayEpoch(); rerender(); }
  else if (a === 'open-today') { state.day = Number(el.closest('[data-d]')?.dataset.d ?? state.day); state.tab = 'today'; rerender(); }
  else if (a === 'pick-day') { state.day = Number(el.dataset.d); state.tab = 'today'; state.rasi = null; rerender(); }
  // A month cell selects its day and stays on the Month tab; the details show in the section below the grid.
  else if (a === 'select-day') { state.day = Number(el.dataset.d); rerender(); }
  else if (a === 'month-view') { state.settings.monthView = state.settings.monthView === 'page' ? 'grid' : 'page'; saveSettings(state.settings); rerender(); }
  else if (a === 'month') { const [y, m] = state.monthOf || [yearOf(state.day), monthOf(state.day) - 1]; const n = new Date(Date.UTC(y, m + Number(el.dataset.m), 1)); state.monthOf = [n.getUTCFullYear(), n.getUTCMonth()]; state.flipDir = Number(el.dataset.m); rerender(); }
  else if (a === 'month-today') { const t = todayEpoch(); state.monthOf = [yearOf(t), monthOf(t) - 1]; state.day = t; rerender(); }
  else if (a === 'kind') { state.kindFilter = el.dataset.k; rerender(); }
  else if (a === 'print-month') { window.print(); }
  else if (a === 'rasi-open') { state.rasi = Number(el.dataset.r); state.rasiTab = 0; rerender(); }
  else if (a === 'rasi-tab') { state.rasiTab = Number(el.dataset.t); rerender(); }
  else if (a === 'rasi-close') { closeRasi(); }
  else if (a === 'rasi-read') { toggleRasiSpeech(); }
  else if (a === 'sloka-read') { const d = dayData(state.day); speak(slokaFor(d)); }
  else if (a === 'sloka-music') toggleMusic();
  else if (a === 'place-preset') setPlace(PLACES.find((p) => p.key === el.dataset.key));
  else if (a === 'place-recent') setPlace({ custom: state.settings.recentPlaces[Number(el.dataset.i)] });
  else if (a === 'place-result') { const r = state.search.results[Number(el.dataset.i)]; setPlace({ custom: { nameTa: r.name, nameEn: r.name, latitude: r.latitude, longitude: r.longitude, zoneId: r.zoneId, countryCode: r.countryCode, admin1: r.admin1, countryName: r.countryName } }); }
  else if (a === 'place-auto') autoLocate();
  else if (a === 'pin-current') pinCurrent();
  else if (a === 'pin-remove') unpin(el.dataset.id);
  else if (a === 'pin-open') { const pin = state.settings.pinnedPlaces.find((p) => p.id === el.dataset.id); if (pin) setPlace(pin.place.custom ? { custom: pin.place.custom } : PLACES.find((x) => x.key === pin.place.preset), false); }
  else if (a === 'pack-save') { const pin = state.settings.pinnedPlaces.find((p) => p.id === el.dataset.id); if (pin) savePack(pin, Number(el.dataset.year), el); }
  else if (a === 'theme') { applySettingChange('theme', el.dataset.id); }
  else if (a === 'skin') { applySettingChange('skin', el.dataset.id); }
  else if (a === 'notify-allow') { if ('Notification' in window) Notification.requestPermission().then(() => rerender()); }
  else if (a === 'disclaimer') { state.settings.disclaimerSeen = false; saveSettings(state.settings); showDisclaimer(); }
  else if (a === 'reset-settings') { if (confirm(lb('அமைப்புகளை மீட்டமைக்கவா?', 'Reset all settings?'))) { localStorage.removeItem('ktc.settings.v2'); location.reload(); } }
}

function closeRasi() {
  stopSpeaking();
  state.rasi = null;
  rerender();
}

/** Reads the open rasi's current tab aloud, or stops if it is already speaking. */
function toggleRasiSpeech() {
  if (!('speechSynthesis' in window)) return;
  if (window.speechSynthesis.speaking) { stopSpeaking(); return rerender(); }
  const d = dayData(state.day) || dayData(todayEpoch());
  const p = rasiPalan(d.panchangam.moonRasi)[state.rasi];
  const g = gochara(state.rasi, skyPositions(d.epochDay, state.place.zoneId));
  const text = [[p.messageTa], [g.month.ta], [g.year.ta]][state.rasiTab][0];
  speakText(text);
}

function speakText(text) {
  if (!('speechSynthesis' in window)) return;
  window.speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(text);
  u.lang = 'ta-IN';
  window.speechSynthesis.speak(u);
}

function onChange(e) {
  const el = e.target;
  state.lastInput = Date.now();
  if (el.dataset.mh) {
    if (el.value) state[el.dataset.mh === 'from' ? 'mhFrom' : 'mhTo'] = parseIso(el.value);
    return rerender();
  }
  if (el.dataset.cmp === 'place') { state.cmpKey = el.value; return rerender(); }
  if (el.dataset.cmp === 'day') { if (el.value) state.cmpDay = parseIso(el.value); return rerender(); }
  if (el.dataset.setting) {
    const key = el.dataset.setting;
    const value = el.type === 'checkbox' ? el.checked : (['ambientMinutes', 'janmaStar', 'janmaRasi'].includes(key) ? Number(el.value) : el.value);
    applySettingChange(key, value);
  } else if (el.dataset.section) {
    const hidden = new Set(state.settings.hiddenSections);
    el.checked ? hidden.delete(el.dataset.section) : hidden.add(el.dataset.section);
    applySettingChange('hiddenSections', [...hidden]);
  } else if (el.dataset.faith) {
    const hidden = new Set(state.settings.hiddenFaiths);
    el.checked ? hidden.delete(el.dataset.faith) : hidden.add(el.dataset.faith);
    applySettingChange('hiddenFaiths', [...hidden]);
  } else if (el.dataset.music) {
    applySettingChange('music', { ...state.settings.music, [el.dataset.music]: el.value });
    if (state.music) { state.music.stop(); state.music = new TempleMusic(state.settings.music); state.music.start(); }
  }
}

function showDisclaimer() {
  $('disclaimer').hidden = false;
}

function applyTheme() {
  const t = CONTENT.themes.find((x) => x.id === state.settings.theme) || CONTENT.themes[0];
  const root = document.documentElement.style;
  document.documentElement.dataset.skin = state.settings.skin || 'CLASSIC';
  const map = { background: '--bg', backgroundDeep: '--bg-deep', backgroundMid: '--bg-mid', surface: '--surface', surfaceFocused: '--surface-hi', surfaceSelected: '--surface-sel', accent: '--accent', accentSoft: '--accent-soft', alert: '--alert', alertSoft: '--alert-soft', amber: '--amber', text: '--text', textMuted: '--muted', textDim: '--dim' };
  for (const [k, v] of Object.entries(map)) root.setProperty(v, t.colors[k]);
  root.setProperty('--bg-grad', `linear-gradient(165deg, ${t.colors.background} 0%, ${t.colors.backgroundMid} 55%, ${t.colors.backgroundDeep} 100%)`);
  document.documentElement.dataset.theme = t.isLight ? 'light' : 'dark';
  document.querySelector('meta[name="theme-color"]').setAttribute('content', t.colors.background);
}

// ---------------------------------------------------------------- boot
async function boot() {
  setEngineFromSettings();
  const requested = new URLSearchParams(location.search).get('tab');
  if (TABS.some(([id]) => id === requested)) state.tab = requested;
  state.school = await fetch('engine/school_calendars.json').then((r) => (r.ok ? r.json() : null)).catch(() => null);
  $('view').addEventListener('click', onClick);
  $('tabs').addEventListener('click', onClick);
  $('view').addEventListener('change', onChange);
  $('view').addEventListener('submit', (e) => {
    if (e.target.id !== 'search-form') return;
    e.preventDefault();
    searchPlaces($('search').value);
  });
  $('view').addEventListener('input', (e) => {
    if (e.target.id === 'search') state.search.query = e.target.value;
    if (e.target.id === 'find') { state.findQuery = e.target.value; const box = $('find-results'); if (box) box.innerHTML = findResults(state.findQuery); }
  });
  $('ambient').addEventListener('click', () => showAmbient(false));
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && state.rasi !== null) closeRasi(); });
  $('disclaimer-ok').addEventListener('click', () => { state.settings.disclaimerSeen = true; saveSettings(state.settings); $('disclaimer').hidden = true; });
  $('place-chip').addEventListener('click', () => { state.tab = 'location'; rerender(); });
  for (const ev of ['pointerdown', 'keydown', 'touchstart']) document.addEventListener(ev, () => { state.lastInput = Date.now(); }, { passive: true });
  setInterval(() => {
    const s = state.settings;
    if (!state.ambientOn && s.ambient && Date.now() - state.lastInput >= s.ambientMinutes * 60000) showAmbient(true);
    if (state.ambientOn) ambientTick();
  }, 1000);
  if (!state.settings.disclaimerSeen) showDisclaimer();
  rerender();
  if (state.settings.autoLocate && navigator.permissions) {
    navigator.permissions.query({ name: 'geolocation' }).then((r) => { if (r.state === 'granted') autoLocate(); }).catch(() => {});
  }
  scheduleToday();
  setInterval(scheduleToday, 60 * 60 * 1000);
  // Update prompt: a new version waits until the user taps Reload, then the page reloads onto it.
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('sw.js').then((reg) => {
    if (reg.waiting && navigator.serviceWorker.controller) showUpdate(reg.waiting);
    reg.addEventListener('updatefound', () => {
      const worker = reg.installing;
      worker.addEventListener('statechange', () => {
        if (worker.state === 'installed' && navigator.serviceWorker.controller) showUpdate(worker);
      });
    });
  }).catch(() => { /* offline support is optional */ });
  let reloading = false;
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (!reloading) { reloading = true; location.reload(); }
  });
}
function showUpdate(worker) {
  const bar = $('update');
  bar.hidden = false;
  $('update-go').onclick = () => worker.postMessage('SKIP_WAITING');
}
}

boot();

// ---- moon and ayanam: the day's moon panel (Month) and the Learn page
/** The selected day's moon: a large phase emoji, how much of the face is lit, and what it looks like. */
function moonPanel(d) {
  const mi = moonPhaseInfo(d.panchangam.tithi.index);
  const af = ayanamFacts(d.epochDay, state.place);
  const pct = Math.round(mi.lit * 100);
  return `<div class="moon-panel">
    <div class="moon-big" role="img" aria-label="${esc(mi.en)}">${mi.emoji}</div>
    <div class="moon-text">
      <b>${esc(lb(mi.ta, mi.en))}</b>
      <div class="lit-bar" aria-hidden="true"><i style="width:${pct}%"></i></div>
      <small>${pct}% ${esc(lb('ஒளி', 'lit'))} · ${esc(lb(mi.waxing ? 'வளர்பிறை' : 'தேய்பிறை', mi.waxing ? 'waxing' : 'waning'))}</small>
      <p>${esc(lb(mi.expTa, mi.expEn))}</p>
    </div>
  </div>
  <p class="small">${esc(lb(...AYANAM_NAMES[af.kind]))} · ${esc(lb('நாள்', 'day'))} ${af.daysIn + 1}${af.nextDay !== null ? ` · ${esc(lb(...AYANAM_NAMES[af.nextKind]))}: ${esc(dateShort(isoOf(af.nextDay)))}` : ''}</p>`;
}

/** The Learn page: moon phases, Uttarayanam and Dakshinayanam, the year map, and plain-words glossary. */
function viewLearn() {
  const today = state.day || todayEpoch();
  const zone = state.place.zoneId;
  const year = yearOf(today);
  const days = ayanamYear(year, zone);
  // Exact crossings, with the day each one counts from (after sunset, the next sunrise).
  const exact = ayanamChanges(year, zone, state.place.latitude, state.place.longitude);
  const now = ayanamFacts(today, state.place);
  const d = dayData(today) || dayData(todayEpoch());
  const todayPhase = d ? moonPhaseInfo(d.panchangam.tithi.index).en : '';
  // Eight phases in order, one representative tithi each (every 3.75 tithis).
  const phaseCards = Array.from({ length: 8 }, (_, i) => {
    const mi = moonPhaseInfo(i * 3.75);
    return `<article class="learn-phase${mi.en === todayPhase ? ' now' : ''}" style="--delay:${i * 0.35}s">
      <div class="moon-anim" aria-hidden="true">${mi.emoji}</div>
      <b>${esc(lb(mi.ta, mi.en))}</b><small>${esc(lb(mi.expTa, mi.expEn))}</small></article>`;
  }).join('');
  const monthMaps = Array.from({ length: 12 }, (_, m) => {
    const cells = days.filter(([e]) => monthOf(e) === m + 1).map(([e, k]) => {
      const cur = e === today ? ' today' : '';
      return `<i class="ay-cell ${k === 'UTTARAYANAM' ? 'ay-u' : 'ay-d'}${cur}" title="${esc(dateShort(isoOf(e)))}"></i>`;
    }).join('');
    return `<div class="ay-month"><b>${esc(lb(MONTHS_TA[m], MONTHS_EN[m]))}</b><div class="ay-days">${cells}</div></div>`;
  }).join('');
  const changeList = exact.map((c) => {
    const entering = c.kind === 'UTTARAYANAM' ? lb('மகர ராசி (270°)', 'Makara (270°)') : lb('கடக ராசி (90°)', 'Karka (90°)');
    return `<li class="change-card ${c.kind === 'UTTARAYANAM' ? 'ay-u-card' : 'ay-d-card'}">
      <b>${esc(lb(...AYANAM_NAMES[c.kind]))} · ${esc(lb('தொடக்கம்', 'begins'))}</b>
      <p>${esc(lb('சூரியன் நுழையும் நேரம்', 'The Sun enters'))} ${esc(entering)}: <b>${esc(dateShort(isoOf(c.day)))}, ${c.time}</b> ${esc(lb('(இடத்தின் நேரம்)', '(local time)'))}</p>
      <p class="small">${esc(lb('இந்த நாளிலிருந்து கணக்கு', 'Counts from'))}: <b>${esc(dateShort(isoOf(c.effectiveDay)))}</b>${c.afterSunset ? ` ${esc(lb('(சூரிய உதயம்)', '(sunrise, since the Sun entered after sunset)'))}` : ''}</p>
    </li>`;
  }).join('');
  const gloss = [
    ['🌙', lb('திதி', 'Tithi'), lb('சந்திரனும் சூரியனும் ஒரு கோணத்தில் பிரிவதை வைத்து அளக்கும் நாள். 12 பாகை = ஒரு திதி.', 'The lunar day: the Moon pulls away from the Sun by 12 degrees for each tithi.')],
    ['⭐', lb('நட்சத்திரம்', 'Nakshatram'), lb('சந்திரன் இருக்கும் 27 பகுதிகளில் ஒன்று (ஒவ்வொன்றும் 13° 20′).', 'One of 27 star segments the Moon passes through each day.')],
    ['☀', lb('அயனம்', 'Ayanam'), lb('சூரியன் வடக்கு நோக்கியோ தெற்கு நோக்கியோ நகரும் ஆறு மாதக் காலம்.', 'A six-month half of the Sun\'s year: north-moving or south-moving.')],
    ['♈', lb('ராசி', 'Rasi'), lb('வானத்தை 12 சம பகுதிகளாகப் பிரிக்கும் ராசிகள் (மேஷம் முதல் மீனம் வரை).', 'One of the 12 equal signs of the sky, from Mesham to Meenam.')],
    ['🔁', lb('சங்கராந்தி', 'Sankranti'), lb('சூரியன் ஒரு ராசியிலிருந்து அடுத்த ராசிக்குள் நுழையும் நாள்.', 'The day the Sun enters a new sign. Uttarayanam starts at Makara Sankranti.')],
    ['🌑', lb('அமாவாசை', 'Amavasai'), lb('சந்திரன் சூரியனுடன் ஒரே திசையில் இருக்கும் நாள். நிலா தெரியாது.', 'New Moon: the Moon is in line with the Sun, so we see no lit face.')],
  ].map(([icon, label, text]) => `<div class="gloss-item"><span>${icon}</span><div><b>${esc(label)}</b><p>${esc(text)}</p></div></div>`).join('');
  return `<h1 class="page">${esc(lb('அறிவோம் · நிலவு, சூரியன், அயனம்', 'Learn · Moon, Sun and Ayanam'))}</h1>
    <p class="page-sub">${esc(lb('நிலவும் சூரியனும் வானில் நகர்வதால் நாட்காட்டி எப்படி மாறுகிறது என்பதை எளிமையாகப் பாருங்கள்.', 'See, in simple steps, how the Moon and the Sun move across the sky and change the calendar.'))}</p>

    <section class="learn-sec">
      <h2>${esc(lb('நிலவின் நிலைகள்', 'The phases of the Moon'))}</h2>
      <p>${esc(lb('நிலவு தானாக ஒளிர்வதில்லை. சூரியன் ஒளியை வாங்கிப் பிரதிபலிக்கிறது. அது நம்மைச் சுற்றும்போது எவ்வளவு ஒளிப்பக்கம் நமக்குத் தெரிகிறது என்பதுதான் நிலவின் வடிவம்.', 'The Moon makes no light of its own. It reflects sunlight, and how much of the lit face we see changes as it circles the Earth.'))}</p>
      <div class="learn-phases">${phaseCards}</div>
    </section>

    <section class="learn-sec">
      <h2>${esc(lb('உத்தராயணம் & தட்சிணாயனம்', 'Uttarayanam and Dakshinayanam'))}</h2>
      <p>${esc(lb('சூரியன் ஆண்டு முழுவதும் ஒரு பாதையில் நகர்வது போல் தெரியும். அது வடக்கு நோக்கி நகரும் ஆறு மாதம் உத்தராயணம்; தெற்கு நோக்கி நகரும் ஆறு மாதம் தட்சிணாயனம்.', 'From Earth the Sun seems to travel a path over the year. The six months it moves north are Uttarayanam; the six months it moves south are Dakshinayanam.'))}</p>
      <div class="sun-year">
        <div class="sun-track" aria-hidden="true"><span class="sun-go">☀</span></div>
        <div class="sun-months">${Array.from({ length: 12 }, (_, m) => {
          const rasi = sunRasiOn(epochDay(year, m + 1, 15), zone);
          const k = ayanamOfSun(rasi * 30 + 15);
          const cur = m + 1 === monthOf(today) ? ' now' : '';
          return `<div class="sun-m ${k === 'UTTARAYANAM' ? 'sun-north' : 'sun-south'}${cur}">
            <b>${esc(lb(MONTHS_TA[m], MONTHS_EN[m]))}</b>
            <span class="sun-arrow" aria-hidden="true">${k === 'UTTARAYANAM' ? '↑' : '↓'}</span>
            <small>${esc(lb(...CONTENT.names.rasis[rasi]))}</small>
            <em>${esc(lb(...AYANAM_NAMES[k]))}</em></div>`;
        }).join('')}</div>
        <p class="small">${esc(lb('ஒவ்வொரு மாதத்தின் 15-ஆம் தேதியில் சூரியன் இருக்கும் ராசி. ↑ வடக்கு நோக்கி (உத்தராயணம்), ↓ தெற்கு நோக்கி (தட்சிணாயனம்).', 'The Sun\'s sign on the 15th of each month. ↑ northward (Uttarayanam), ↓ southward (Dakshinayanam).'))}</p>
      </div>
      <div class="ay-cards">
        <article class="ay-card ay-u-card"><h3>☀ ${esc(lb(...AYANAM_NAMES.UTTARAYANAM))}</h3>
          <p>${esc(lb('மகர சங்கராந்தியில் (சுமார் ஜனவரி 14) தொடங்கி, கடக சங்கராந்தி (சுமார் ஜூலை 16) வரை. பகல் நீளம் கூடும்.', 'Starts at Makara Sankranti (about 14 January) and lasts until Karka Sankranti (about 16 July). Days grow longer.'))}</p></article>
        <article class="ay-card ay-d-card"><h3>🌙 ${esc(lb(...AYANAM_NAMES.DAKSHINAYANAM))}</h3>
          <p>${esc(lb('கடக சங்கராந்தியில் (சுமார் ஜூலை 16) தொடங்கி, மகர சங்கராந்தி (சுமார் ஜனவரி 14) வரை. பகல் நீளம் குறையும்.', 'Starts at Karka Sankranti (about 16 July) and lasts until Makara Sankranti (about 14 January). Days grow shorter.'))}</p></article>
      </div>
      <p class="now-ayanam">${esc(lb(...AYANAM_NAMES[now.kind]))} · ${esc(lb('நாள்', 'day'))} ${now.daysIn + 1}${now.nextDay !== null ? ` · ${esc(lb(...AYANAM_NAMES[now.nextKind]))}: ${esc(dateShort(isoOf(now.nextDay)))}` : ''}</p>
      <h3 class="sub-h">${esc(lb(`${year} ஆண்டின் மாற்றங்கள்`, `Changes in ${year}`))}</h3>
      <ul class="change-list">${changeList}</ul>
    </section>

    <section class="learn-sec">
      <h2>${esc(lb('ஆண்டு வரைபடம்', 'Year map'))}</h2>
      <p class="small"><i class="ay-cell ay-u"></i> ${esc(lb('உத்தராயணம்', 'Uttarayanam'))} &nbsp; <i class="ay-cell ay-d"></i> ${esc(lb('தட்சிணாயனம்', 'Dakshinayanam'))} &nbsp; <i class="ay-cell ay-u today"></i> ${esc(lb('இன்று', 'Today'))}</p>
      <div class="year-map">${monthMaps}</div>
    </section>

    <section class="learn-sec">
      <h2>${esc(lb('சொற்கள்', 'Words to know'))}</h2>
      <div class="gloss-grid">${gloss}</div>
    </section>`;
}
