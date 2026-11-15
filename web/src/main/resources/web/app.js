// Tamil calendar web app: the UI. All calculations run in the browser (engine/), so any place and date works.
import { PLACES, placeLabel, localHolidayRegions, timingConvention, usesImperialUnits } from './engine/locations.js';
import { monthDays, importantDates, applying } from './engine/calendar.js';
import { rasiPalan, skyPositions, grahasIn, gochara } from './engine/rasi.js';
import { CONTENT } from './engine/content.js';
import { epochDay, isoOf, parseIso, dayOfWeek, dayOfMonth, monthOf, yearOf, fromEpochDay, wallClock, zonedMs, MS_DAY } from './engine/civil.js';
import { loadSettings, saveSettings } from './app/store.js';
import { fetchWeather, sceneOf, labelOf } from './app/weather.js';
import { slokaFor, speak, stopSpeaking, TempleMusic } from './app/sloka.js';
import { scheduleReminders, notifyUser, clearReminders } from './app/reminders.js';

const $ = (id) => document.getElementById(id);
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);
const TABS = [
  ['today', 'இன்று', 'Today'], ['month', 'மாதம்', 'Month'], ['festivals', 'விழாக்கள்', 'Festivals'],
  ['important', 'முக்கிய நாட்கள்', 'Important'], ['rasi', 'ராசி', 'Rasi'], ['location', 'இடம்', 'Location'],
  ['theme', 'தீம்', 'Theme'], ['settings', 'அமைப்புகள்', 'Settings'], ['about', 'பற்றி', 'About'],
];
const ALWAYS_SHOWN = new Set(['today', 'location', 'theme', 'settings', 'about']);
const MONTHS_TA = ['ஜனவரி', 'பிப்ரவரி', 'மார்ச்', 'ஏப்ரல்', 'மே', 'ஜூன்', 'ஜூலை', 'ஆகஸ்ட்', 'செப்டம்பர்', 'அக்டோபர்', 'நவம்பர்', 'டிசம்பர்'];
const MONTHS_EN = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
const WEEKDAYS_TA_SHORT = ['ஞா', 'தி', 'செ', 'பு', 'வி', 'வெ', 'ச'];
const RASI_CHART = [11, 0, 1, 2, 10, -1, -1, 3, 9, -1, -1, 4, 8, 7, 6, 5]; // South Indian layout, 4x4 (-1 = centre)

const state = {
  settings: loadSettings(),
  tab: 'today',
  day: 0,            // selected epoch day
  monthOf: null,     // [year, month] shown on the month tab
  place: null,       // engine place object
  placeKey: '',
  cache: new Map(),
  school: null,
  weather: null,
  weatherErr: '',
  music: null,
  rasi: null,
  search: { query: '', results: [], error: '' },
  kindFilter: 'ALL',
  weatherKey: '',
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
  const view = { today: viewToday, month: viewMonth, festivals: viewFestivals, important: viewImportant, rasi: viewRasi, location: viewLocation, theme: viewTheme, settings: viewSettings, about: viewAbout }[state.tab];
  $('view').innerHTML = view();
  $('view').classList.toggle('today-wide', state.tab === 'today');
  if (state.tab === 'today') loadWeatherIfNeeded();
}

// ---- today
function viewToday() {
  const d = dayData(state.day);
  if (!d) return '<p class="status">No data for this day.</p>';
  const p = d.panchangam;
  const nav = `<div class="bar"><button class="nav" data-act="day" data-d="-1" aria-label="Previous day">‹</button>
    <h1>${esc(dateLong(d.epochDay))}</h1><button class="nav" data-act="day" data-d="1" aria-label="Next day">›</button></div>
    <button class="today" data-act="day-today">${lb('இன்று', 'Today')}</button>`;
  const chips = importantDates(d).map((x) => `<li class="chip${x.holiday ? ' holiday' : ''}">${esc(lbo(x.title))}</li>`).join('');
  const timings = [...d.timings.nallaNeram, d.timings.rahuKalam, d.timings.yamagandam, d.timings.kuligai]
    .concat(d.timings.gowriNallaNeram).map((t) => {
      const cls = t.type === 'NALLA_NERAM' || t.type === 'GOWRI_NALLA_NERAM' ? 'good' : t.type === 'KULIGAI' ? 'mixed' : 'bad';
      const label = lb(CONTENT.enums.TimingType.find((x) => x[0] === t.type)[1], CONTENT.enums.TimingType.find((x) => x[0] === t.type)[2]);
      const gowri = t.gowri !== null ? ` · ${lb(CONTENT.enums.Gowri[t.gowri][1], CONTENT.enums.Gowri[t.gowri][2])}` : '';
      return `<li class="${cls}"><span>${esc(label + gowri)}</span><time>${timeOf(t.start)} – ${timeOf(t.end)}</time></li>`;
    }).join('');
  const chandra = d.chandrashtamam;
  const slo = slokaFor(d);
  const weatherCard = state.settings.weather ? weatherHtml() : '';
  return `${nav}
    <div class="detail">
      <div>
        <h2>${esc(lb(...CONTENT.names.weekdays[d.weekdayIndex]))}</h2>
        <p class="ta-date">${esc(lb(CONTENT.names.months[d.tamilDate.monthIndex][0] + ' ' + d.tamilDate.day, CONTENT.names.months[d.tamilDate.monthIndex][1] + ' ' + d.tamilDate.day))}
          · ${esc(lb(CONTENT.names.years[d.tamilDate.yearCycleIndex][0] + ' வருடம்', CONTENT.names.years[d.tamilDate.yearCycleIndex][1] + ' year'))}</p>
      </div>
      ${d.holiday || chips ? `<ul class="chips">${d.holiday ? `<li class="chip holiday">${esc(lb('விடுமுறை', 'Holiday'))}</li>` : ''}${chips}</ul>` : ''}
      <dl class="facts">
        <div><dt>${esc(lb('உதயம்', 'Sunrise'))}</dt><dd>${timeOf(d.sunrise)}</dd></div>
        <div><dt>${esc(lb('அஸ்தமனம்', 'Sunset'))}</dt><dd>${timeOf(d.sunset)}</dd></div>
        <div><dt>${esc(lb('திதி', 'Tithi'))}</dt><dd>${esc(lb(p.tithi.nameTa, p.tithi.nameEn))} <small>→ ${endOf(p.tithi.endsAt)}</small><br><small>${esc(lb(...pakshaName(p.paksha)))}</small></dd></div>
        <div><dt>${esc(lb('நட்சத்திரம்', 'Nakshatram'))}</dt><dd>${esc(lb(p.nakshatram.nameTa, p.nakshatram.nameEn))} <small>→ ${endOf(p.nakshatram.endsAt)}</small></dd></div>
        <div><dt>${esc(lb('யோகம்', 'Yogam'))}</dt><dd>${esc(lb(p.yogam.nameTa, p.yogam.nameEn))} <small>→ ${endOf(p.yogam.endsAt)}</small></dd></div>
        <div><dt>${esc(lb('சந்திர ராசி', 'Moon sign'))}</dt><dd>${esc(lb(...CONTENT.names.rasis[p.moonRasi]))}</dd></div>
      </dl>
      <section><h3>${esc(lb('நேரங்கள்', 'Timings'))}</h3><ul class="timings">${timings}</ul></section>
      <p class="warn">${esc(lb('சந்திராஷ்டமம்', 'Chandrashtamam'))} — ${esc(lb(...CONTENT.names.rasis[chandra.rasi]))}
        <br><small>${esc(chandra.nakshatras.map((k) => lb(...CONTENT.names.nakshatras[k])).join(', '))}</small></p>
      ${slokaCard(slo)}
      ${weatherCard}
      ${reminderNote()}
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
  </section>`;
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
    <small>${esc(lb('ஈரப்பதம்', 'Humidity'))} ${w.current.humidity}% · ${esc(lb('காற்று', 'Wind'))} ${Math.round(w.current.wind)} ${w.windUnit}</small></div>
    <h3>${esc(lb('அடுத்த 12 மணி நேரம்', 'Next 12 hours'))}</h3><ul class="wx-list">${hours}</ul>
    <h3>${esc(lb('7 நாட்கள்', '7 days'))}</h3><ul class="wx-list days">${days}</ul></section>`;
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
function viewMonth() {
  const [y, m] = state.monthOf || [yearOf(todayEpoch()), monthOf(todayEpoch()) - 1];
  const [from, to] = gridRange(y, m);
  const days = new Map(daysBetween(from, to).map((d) => [d.epochDay, d]));
  const weekdays = CONTENT.names.weekdays.map((w, i) => `<div>${WEEKDAYS_TA_SHORT[i]}<small>${w[1].slice(0, 3)}</small></div>`).join('');
  const today = todayEpoch();
  const cells = [];
  for (let e = from; e <= to; e++) {
    const d = days.get(e);
    const cls = ['cell'];
    if (monthOf(e) !== m + 1) cls.push('other');
    if (d && d.holiday) cls.push('holiday');
    if (e === today) cls.push('today');
    if (e === state.day) cls.push('selected');
    const marks = d ? (d.holiday ? '<i class="dot alert"></i>' : '') + (importantDates(d).length ? '<i class="dot"></i>' : '') : '';
    const tamil = d ? `<span class="t">${d.tamilDate.day}</span>` : '';
    const monthStart = d && d.tamilDate.day === 1 ? `<span class="m">${esc(CONTENT.names.months[d.tamilDate.monthIndex][0])}</span>` : '';
    cells.push(`<button class="${cls.join(' ')}" data-act="pick-day" data-d="${e}"><span class="g">${dayOfMonth(e)}</span>${tamil}${monthStart}<span class="marks">${marks}</span></button>`);
  }
  const sel = dayData(state.day) || dayData(today);
  return `<div class="bar"><button class="nav" data-act="month" data-m="-1" aria-label="Previous month">‹</button>
    <h1>${esc(MONTHS_TA[m])} · ${MONTHS_EN[m]} ${y}</h1><button class="nav" data-act="month" data-m="1" aria-label="Next month">›</button></div>
    <button class="today" data-act="month-today">${lb('இன்று', 'Today')}</button>
    <div class="weekdays">${weekdays}</div><div class="grid">${cells.join('')}</div>
    <div class="detail small-detail">${sel ? `<h2>${esc(dateLong(sel.epochDay))}</h2><p class="ta-date">${esc(lb(CONTENT.names.months[sel.tamilDate.monthIndex][0] + ' ' + sel.tamilDate.day, CONTENT.names.months[sel.tamilDate.monthIndex][1] + ' ' + sel.tamilDate.day))}</p>
      <ul class="chips">${importantDates(sel).map((x) => `<li class="chip${x.holiday ? ' holiday' : ''}">${esc(lbo(x.title))}</li>`).join('') || `<li class="small">${esc(lb('சிறப்பு நாள் இல்லை', 'No observance'))}</li>`}</ul>
      <button class="pill" data-act="open-today">${lb('விவரம்', 'Details')} ›</button>` : ''}</div>`;
}

// ---- festivals & holidays (next 120 days)
function viewFestivals() {
  const start = todayEpoch();
  const kinds = ['ALL', 'FESTIVAL', 'VIRATHAM', 'GOVT_HOLIDAY', 'RELIGIOUS', 'SCHOOL'];
  const filters = kinds.map((k) => `<button class="pill${state.kindFilter === k ? ' on' : ''}" data-act="kind" data-k="${k}">${esc(k === 'ALL' ? lb('அனைத்தும்', 'All') : kindLabel(k))}</button>`).join('');
  const rows = [];
  const seen = new Set(); // the same observance can come from two sources (e.g. Christmas): show it once
  for (const d of daysFrom(start, 120)) {
    for (const e of d.events) {
      if (e.kind === 'MUHURTHAM') continue;
      if (state.kindFilter !== 'ALL' && e.kind !== state.kindFilter) continue;
      if (seen.has(e.date + e.titleEn)) continue;
      seen.add(e.date + e.titleEn);
      rows.push(`<li class="event${e.isLocalHoliday ? ' holiday' : ''}"><time>${esc(dateShort(e.date))}</time><span>${esc(lb(e.titleTa, e.titleEn))}${e.religion && e.religion !== 'HINDU' ? ` <small>· ${esc(lb(...CONTENT.enums.Religion.find((x) => x[0] === e.religion).slice(1, 3)))}</small>` : ''}</span></li>`);
    }
  }
  return `<h1 class="page">${esc(lb('விழாக்கள் & விடுமுறைகள்', 'Festivals & holidays'))}</h1>
    <div class="row wrap">${filters}</div>
    <ul class="events">${rows.join('') || `<li class="small">${esc(lb('இல்லை', 'Nothing in this period'))}</li>`}</ul>`;
}
const kindLabel = (k) => ({ FESTIVAL: ['விழா', 'Festival'], VIRATHAM: ['விரதம்', 'Viratham'], GOVT_HOLIDAY: ['விடுமுறை', 'Holiday'], RELIGIOUS: ['மதம்', 'Religious'], SCHOOL: ['பள்ளி', 'School'] })[k].join(' · ');
const dateShort = (iso) => new Date(`${iso}T00:00:00Z`).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short', timeZone: 'UTC' });

// ---- important dates (next 60 days)
function viewImportant() {
  const start = todayEpoch();
  const items = daysFrom(start, 60).map((d) => ({ d, list: importantDates(d) })).filter((x) => x.list.length);
  return `<h1 class="page">${esc(lb('முக்கிய நாட்கள்', 'Important dates'))}</h1>
    <ul class="events">${items.map(({ d, list }) => `<li class="event"><time>${esc(dateShort(isoOf(d.epochDay)))}</time>
      <span>${list.map((x) => `<b class="${x.holiday ? 'alert-text' : ''}">${esc(lbo(x.title))}</b>`).join('<br>')}</span></li>`).join('') || `<li class="small">${esc(lb('இல்லை', 'Nothing in this period'))}</li>`}</ul>`;
}

// ---- rasi palan, chart and gochara
function viewRasi() {
  const d = dayData(todayEpoch());
  const palan = rasiPalan(d.panchangam.moonRasi);
  const sky = skyPositions(d.epochDay, state.place.zoneId);
  const cells = RASI_CHART.map((r) => {
    if (r < 0) return '<div class="chart-centre"></div>';
    const labels = grahasIn(sky, r).join(' ');
    return `<button class="chart-cell${state.rasi === r ? ' on' : ''}" data-act="rasi" data-r="${r}"><small>${esc(CONTENT.names.rasis[r][0])}</small><b>${esc(labels)}</b></button>`;
  }).join('');
  const list = palan.map((p) => `<li class="rasi-row ${p.outlook.toLowerCase()}"><span>${esc(lb(...CONTENT.names.rasis[p.rasi]))}</span><b>${esc(lb(...outlookWords(p.outlook)))}</b><small>${esc(lb(p.messageTa, p.messageEn))}</small></li>`).join('');
  let detail = '';
  if (state.rasi !== null) {
    const g = gochara(state.rasi, sky);
    detail = `<section class="detail"><h2>${esc(lb(...CONTENT.names.rasis[state.rasi]))}</h2>
      <h3>${esc(lb('இந்த மாதம்', 'This month'))} · <span class="${g.month.outlook.toLowerCase()}">${esc(lb(...outlookWords(g.month.outlook)))}</span></h3><p class="pre">${esc(lb(g.month.ta, g.month.en))}</p>
      <h3>${esc(lb('இந்த ஆண்டு', 'This year'))} · <span class="${g.year.outlook.toLowerCase()}">${esc(lb(...outlookWords(g.year.outlook)))}</span></h3><p class="pre">${esc(lb(g.year.ta, g.year.en))}</p>
      <button class="pill" data-act="rasi-read">🔊 ${esc(lb('படி', 'Read'))}</button></section>`;
  }
  return `<h1 class="page">${esc(lb('ராசி பலன்', 'Rasi Palan'))}</h1>
    <p class="small">${esc(lb('சந்திர ராசி', 'Moon sign'))}: ${esc(lb(...CONTENT.names.rasis[d.panchangam.moonRasi]))} · ${esc(dateLong(d.epochDay))}</p>
    <div class="chart">${cells}</div>
    ${detail}
    <h3>${esc(lb('ராசி பலன்', 'Rasi Palan today'))}</h3><ul class="rasi-list">${list}</ul>`;
}
const outlookWords = (o) => ({ GOOD: ['நன்மை', 'Good'], NEUTRAL: ['சமம்', 'Neutral'], CAUTION: ['கவனம்', 'Caution'], CHANDRASHTAMAM: ['சந்திராஷ்டமம்', 'Chandrashtamam'] })[o];

// ---- location
function viewLocation() {
  const s = state.settings;
  const presets = PLACES.map((p) => `<button class="pill${!s.place.custom && s.place.preset === p.key ? ' on' : ''}" data-act="place-preset" data-key="${p.key}">${esc(lb(p.nameTa, p.nameEn))}</button>`).join('');
  const recent = s.recentPlaces.map((p, i) => `<button class="pill" data-act="place-recent" data-i="${i}">${esc(p.nameEn)}</button>`).join('');
  const results = state.search.results.map((r, i) => `<li><button class="result" data-act="place-result" data-i="${i}"><b>${esc(r.name)}</b><small>${esc([r.admin1, r.country].filter(Boolean).join(', '))}</small></button></li>`).join('');
  return `<h1 class="page">${esc(lb('இடம்', 'Location'))}</h1>
    <p class="small">${esc(placeLabel(state.place))} · ${esc(state.place.zoneId)}</p>
    <div class="row wrap">${presets}</div>
    <div class="row"><button class="pill" data-act="place-auto">📍 ${esc(lb('என் இடம்', 'Use my location'))}</button></div>
    <form id="search-form" class="search"><input id="search" type="search" placeholder="${esc(lb('நகரம் தேடுக', 'Search a city'))}" value="${esc(state.search.query)}" autocomplete="off"><button class="pill" type="submit">${esc(lb('தேடு', 'Search'))}</button></form>
    ${state.search.error ? `<p class="small error">${esc(state.search.error)}</p>` : ''}
    <ul class="results">${results}</ul>
    ${recent ? `<h3>${esc(lb('சமீபத்தியவை', 'Recent'))}</h3><div class="row wrap">${recent}</div>` : ''}`;
}

// ---- theme
function viewTheme() {
  const cards = CONTENT.themes.map((t) => `<button class="theme-card${state.settings.theme === t.id ? ' on' : ''}" data-act="theme" data-id="${t.id}" style="background:${t.colors.background};color:${t.colors.text}">
    <span class="swatch" style="background:${t.colors.accent}"></span><span class="swatch" style="background:${t.colors.alert}"></span>
    <b style="color:${t.colors.text}">${esc(lb(t.ta, t.en))}</b></button>`).join('');
  return `<h1 class="page">${esc(lb('தீம்', 'Theme'))}</h1><div class="theme-grid">${cards}</div>`;
}

// ---- settings
function viewSettings() {
  const s = state.settings;
  const check = (key, label) => `<label class="toggle"><input type="checkbox" data-setting="${key}" ${s[key] ? 'checked' : ''}> ${esc(label)}</label>`;
  const select = (key, label, opts) => `<label class="field">${esc(label)} <select data-setting="${key}">${opts.map(([v, t]) => `<option value="${v}"${s[key] === v ? ' selected' : ''}>${esc(t)}</option>`).join('')}</select></label>`;
  const faiths = CONTENT.enums.Religion.filter((r) => r[0] !== 'HINDU').map((r) => `<label class="toggle"><input type="checkbox" data-faith="${r[0]}" ${s.hiddenFaiths.includes(r[0]) ? '' : 'checked'}> ${esc(lb(r[1], r[2]))}</label>`).join('');
  const tabs = TABS.filter(([id]) => !ALWAYS_SHOWN.has(id)).map(([id, ta, en]) => `<label class="toggle"><input type="checkbox" data-section="${id}" ${s.hiddenSections.includes(id) ? '' : 'checked'}> ${esc(lb(ta, en))}</label>`).join('');
  const m = s.music;
  const musicSel = (k, label, opts) => `<label class="field">${esc(label)} <select data-music="${k}">${opts.map(([v, t]) => `<option value="${v}"${m[k] === v ? ' selected' : ''}>${esc(t)}</option>`).join('')}</select></label>`;
  return `<h1 class="page">${esc(lb('அமைப்புகள்', 'Settings'))}</h1>
    <section class="group"><h3>${esc(lb('காட்சி', 'Display'))}</h3>
      ${check('weather', lb('வானிலை', 'Weather'))}${check('animations', lb('அனிமேஷன்', 'Animations'))}${check('weatherAnimations', lb('வானிலை அனிமேஷன்', 'Weather animations'))}
      ${check('clock24h', lb('24 மணி நேரம்', '24-hour clock'))}${check('tamilOnly', lb('தமிழ் மட்டும்', 'Tamil only'))}
      ${select('units', lb('அளவுகள்', 'Units'), [['AUTO', lb('தானியங்கி', 'Auto')], ['METRIC', '°C · km/h'], ['IMPERIAL', '°F · mph']])}
    </section>
    <section class="group"><h3>${esc(lb('நாட்காட்டி', 'Calendar'))}</h3>
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
      ${check('slokaVoice', lb('ஸ்லோகம் சத்தமாக', 'Read sloka aloud'))}
      ${musicSel('raga', lb('ராகம்', 'Raga'), [['MOHANAM', 'Mohanam'], ['HINDOLAM', 'Hindolam'], ['REVATHI', 'Revathi']])}
      ${musicSel('tempo', lb('வேகம்', 'Tempo'), [['SLOW', lb('மெதுவாக', 'Slow')], ['MEDIUM', lb('நடுத்தரம்', 'Medium')], ['FAST', lb('வேகமாக', 'Fast')]])}
      ${musicSel('bell', lb('மணி', 'Bell'), [['OFF', lb('இல்லை', 'Off')], ['SOFT', lb('மெல்லிய', 'Soft')], ['NORMAL', lb('சாதாரண', 'Normal')], ['PROMINENT', lb('தெளிவான', 'Prominent')]])}
      ${musicSel('pitch', lb('சுருதி', 'Pitch'), [['C', 'C'], ['D', 'D'], ['E', 'E'], ['F', 'F'], ['G', 'G']])}
    </section>
    <section class="group"><h3>${esc(lb('நினைவூட்டல்கள்', 'Reminders'))}</h3>
      ${check('rahuReminder', lb('ராகு காலத்திற்கு 10 நிமிடம் முன்', '10 minutes before Rahu Kalam'))}
      ${check('nallaReminder', lb('நல்ல நேரம் தொடங்கும்போது', 'When Nalla Neram begins'))}
      ${reminderNote()}
    </section>
    <section class="group"><h3>${esc(lb('தரவு', 'Data'))}</h3><button class="pill" data-act="reset-settings">${esc(lb('இயல்புநிலைக்கு மீட்டமை', 'Reset to defaults'))}</button></section>`;
}

// ---- about
function viewAbout() {
  return `<h1 class="page">${esc(lb('பற்றி', 'About'))}</h1>
    <p>${esc(lb('இந்த நாட்காட்டியின் கணக்குகள் இந்த சாதனத்திலேயே நடக்கின்றன. சூரியன், சந்திரன் நிலைகள் Meeus நுட்பங்களிலிருந்து; லஹிரி அயனாம்சம்.', 'Every calculation runs on this device from Sun and Moon positions (Meeus algorithms, Lahiri ayanamsa). The engine matches the Android TV app.'))}</p>
    <p class="small">${esc(lb('விடுமுறை நாட்கள் அரசுப் பட்டியல்களைப் பொறுத்தது; இஸ்லாமிய நாட்கள் பிறை பார்த்தலால் ஒரு நாள் மாறலாம்.', 'Holidays follow official lists where known. Islamic dates are calculated and local moon sighting may differ by a day.'))}</p>
    <p class="small">${esc(lb('ஸ்லோகங்கள் பொது உரிமையுள்ள பாரம்பரிய நூல்கள்.', 'All sloka texts are public-domain classics.'))}</p>
    <p><a href="https://github.com/kamalsoft/KTamilCalendar" target="_blank" rel="noopener">github.com/kamalsoft/KTamilCalendar</a></p>
    <p class="small">${esc(lb('தொடர்ச்சியாக பஞ்சாங்கம் சரிபார்க்க சுவிஸ் எபிமெரிஸ் ஒப்பீடு.', 'Panchangam accuracy is checked against the Swiss Ephemeris.'))}</p>
    <button class="pill" data-act="disclaimer">${esc(lb('மறுப்பு அறிவிப்பு', 'Show disclaimer'))}</button>`;
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
  fetchWeather(p.latitude, p.longitude, unitsImperial())
    .then((w) => { if (state.weatherKey === key) state.weather = w; done(); })
    .catch(() => { if (state.weatherKey === key) state.weatherErr = lb('வானிலை கிடைக்கவில்லை', 'Weather unavailable (offline?)'); done(); });
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
}
function ambientTick() {
  if (!state.ambientOn) return;
  const d = dayData(todayEpoch());
  const now = new Date();
  const nowSec = wallClock(now.getTime(), state.place.zoneId).sec;
  const clock = timeOf(`${String(Math.floor(nowSec / 3600)).padStart(2, '0')}:${String(Math.floor((nowSec % 3600) / 60)).padStart(2, '0')}:00`);
  const next = d.timings.nallaNeram.find((w) => secOf(w.start) > nowSec) || d.timings.nallaNeram[0];
  const rahu = d.timings.rahuKalam;
  $('ambient').innerHTML = `<div class="amb-inner">
    <div class="amb-clock">${clock}</div>
    <div class="amb-date">${esc(dateLong(d.epochDay))}</div>
    <div class="amb-line">${esc(lb(CONTENT.names.months[d.tamilDate.monthIndex][0] + ' ' + d.tamilDate.day, CONTENT.names.months[d.tamilDate.monthIndex][1] + ' ' + d.tamilDate.day))}</div>
    <div class="amb-line">${esc(lb('திதி', 'Tithi'))}: ${esc(lb(d.panchangam.tithi.nameTa, d.panchangam.tithi.nameEn))}</div>
    <div class="amb-line">${esc(lb('அடுத்த நல்ல நேரம்', 'Next Nalla Neram'))}: ${next ? timeOf(next.start) : '—'}</div>
    <div class="amb-line">${esc(lb('ராகு காலம்', 'Rahu Kalam'))}: ${timeOf(rahu.start)} – ${timeOf(rahu.end)}</div>
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

async function searchPlaces(q) {
  state.search = { query: q, results: [], error: '' };
  if (!q.trim()) return rerender();
  try {
    const r = await fetch(`https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(q)}&count=8&language=en&format=json`);
    const j = await r.json();
    state.search.results = (j.results || []).map((x) => ({ name: x.name, latitude: x.latitude, longitude: x.longitude, zoneId: x.timezone, countryCode: x.country_code, admin1: x.admin1 || '', country: x.country || '', countryName: x.country || '' }));
    if (!state.search.results.length) state.search.error = lb('எதுவும் கிடைக்கவில்லை', 'No matching places');
  } catch (e) {
    state.search.error = lb('தேட முடியவில்லை (இணைய இணைப்பு?)', 'Search needs an internet connection');
  }
  rerender();
}

function onClick(e) {
  const el = e.target.closest('[data-act]');
  if (!el) return;
  const a = el.dataset.act;
  state.lastInput = Date.now();
  if (a === 'tab') { state.tab = el.dataset.tab; rerender(); }
  else if (a === 'day') { state.day += Number(el.dataset.d); rerender(); }
  else if (a === 'day-today') { state.day = todayEpoch(); rerender(); }
  else if (a === 'open-today') { state.day = Number(el.closest('[data-d]')?.dataset.d ?? state.day); state.tab = 'today'; rerender(); }
  else if (a === 'pick-day') { state.day = Number(el.dataset.d); state.tab = 'today'; state.rasi = null; rerender(); }
  else if (a === 'month') { const [y, m] = state.monthOf || [yearOf(state.day), monthOf(state.day) - 1]; const n = new Date(Date.UTC(y, m + Number(el.dataset.m), 1)); state.monthOf = [n.getUTCFullYear(), n.getUTCMonth()]; rerender(); }
  else if (a === 'month-today') { const t = todayEpoch(); state.monthOf = [yearOf(t), monthOf(t) - 1]; state.day = t; rerender(); }
  else if (a === 'kind') { state.kindFilter = el.dataset.k; rerender(); }
  else if (a === 'rasi') { state.rasi = Number(el.dataset.r); rerender(); }
  else if (a === 'rasi-read') { const d = dayData(todayEpoch()); const g = gochara(state.rasi, skyPositions(d.epochDay, state.place.zoneId)); speakText(g.month.ta); }
  else if (a === 'sloka-read') { const d = dayData(state.day); speak(slokaFor(d)); }
  else if (a === 'sloka-music') toggleMusic();
  else if (a === 'place-preset') setPlace(PLACES.find((p) => p.key === el.dataset.key));
  else if (a === 'place-recent') setPlace({ custom: state.settings.recentPlaces[Number(el.dataset.i)] });
  else if (a === 'place-result') { const r = state.search.results[Number(el.dataset.i)]; setPlace({ custom: { nameTa: r.name, nameEn: r.name, latitude: r.latitude, longitude: r.longitude, zoneId: r.zoneId, countryCode: r.countryCode, admin1: r.admin1, countryName: r.countryName } }); }
  else if (a === 'place-auto') autoLocate();
  else if (a === 'theme') { applySettingChange('theme', el.dataset.id); }
  else if (a === 'notify-allow') { if ('Notification' in window) Notification.requestPermission().then(() => rerender()); }
  else if (a === 'disclaimer') { state.settings.disclaimerSeen = false; saveSettings(state.settings); showDisclaimer(); }
  else if (a === 'reset-settings') { if (confirm(lb('அமைப்புகளை மீட்டமைக்கவா?', 'Reset all settings?'))) { localStorage.removeItem('ktc.settings.v2'); location.reload(); } }
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
  if (el.dataset.setting) {
    const key = el.dataset.setting;
    const value = el.type === 'checkbox' ? el.checked : (key === 'ambientMinutes' ? Number(el.value) : el.value);
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
  const map = { background: '--bg', backgroundDeep: '--bg-deep', backgroundMid: '--bg-mid', surface: '--surface', surfaceFocused: '--surface-hi', surfaceSelected: '--surface-sel', accent: '--accent', accentSoft: '--accent-soft', alert: '--alert', alertSoft: '--alert-soft', amber: '--amber', text: '--text', textMuted: '--muted', textDim: '--dim' };
  for (const [k, v] of Object.entries(map)) root.setProperty(v, t.colors[k]);
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
  $('view').addEventListener('input', (e) => { if (e.target.id === 'search') state.search.query = e.target.value; });
  $('ambient').addEventListener('click', () => showAmbient(false));
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
  scheduleToday();
  setInterval(scheduleToday, 60 * 60 * 1000);
  if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js');
}

boot();
