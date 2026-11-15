// Tamil calendar web app: a month grid plus the selected day's panchangam, from static month files.
const MONTHS_TA = ['ஜனவரி', 'பிப்ரவரி', 'மார்ச்', 'ஏப்ரல்', 'மே', 'ஜூன்', 'ஜூலை', 'ஆகஸ்ட்', 'செப்டம்பர்', 'அக்டோபர்', 'நவம்பர்', 'டிசம்பர்'];
const MONTHS_EN = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
// Two-letter Tamil weekday abbreviations (Sunday first) so seven headers fit a phone width.
const WEEKDAYS_TA_SHORT = ['ஞா', 'தி', 'செ', 'பு', 'வி', 'வெ', 'ச'];
const DAY = 86400000;

const state = { year: 0, month: 0, place: 'chennai', today: '', selected: '', days: new Map(), weekdays: [], cache: new Map() };
const $ = (id) => document.getElementById(id);

const iso = (d) => d.toISOString().slice(0, 10);
const monthKey = () => `${state.year}-${String(state.month + 1).padStart(2, '0')}`;
const bi = (o) => `${o.ta} · ${o.en}`;
const hm = (s) => (s.includes('T') ? s.slice(11, 16) : s.slice(0, 5));
const esc = (s) => String(s).replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);

// Preferences are a convenience only: private windows can block storage.
function remember(key, value) { try { localStorage.setItem(key, value); } catch (e) { /* not persisted */ } }
function recall(key) { try { return localStorage.getItem(key); } catch (e) { return null; } }

/** The Sunday-to-Saturday span shown by the grid for the current month. */
function gridRange() {
  const first = new Date(Date.UTC(state.year, state.month, 1));
  const last = new Date(Date.UTC(state.year, state.month + 1, 0));
  return {
    from: new Date(first.getTime() - first.getUTCDay() * DAY),
    to: new Date(last.getTime() + (6 - last.getUTCDay()) * DAY),
  };
}

// Static month files written by the Kotlin generator: data/<place>/<yyyy-MM>.json.
const ZONES = { chennai: 'Asia/Kolkata', singapore: 'Asia/Singapore', colombo: 'Asia/Colombo', naperville: 'America/Chicago', london: 'Europe/London' };

async function fetchMonth() {
  const key = `${state.place}/${monthKey()}`;
  if (!state.cache.has(key)) {
    const res = await fetch(`data/${key}.json`);
    if (!res.ok) throw new Error(res.status === 404 ? 'no data for this month' : `HTTP ${res.status}`);
    state.cache.set(key, await res.json());
  }
  return state.cache.get(key);
}

/** Today's date in the selected place's time zone, as yyyy-MM-dd. */
function todayIn(place) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONES[place] }).format(new Date());
}

async function render() {
  const { from, to } = gridRange();
  const status = $('status');
  $('monthTitle').textContent = `${MONTHS_TA[state.month]} · ${MONTHS_EN[state.month]} ${state.year}`;
  try {
    const data = await fetchMonth();
    status.hidden = true;
    state.today = todayIn(state.place);
    state.weekdays = data.weekdays;
    state.days = new Map(data.days.map((d) => [d.date, d]));
    if (!state.selected.startsWith(monthKey())) {
      state.selected = state.today.startsWith(monthKey()) ? state.today : `${monthKey()}-01`;
    }
    drawWeekdays();
    drawGrid();
    renderDetail(state.days.get(state.selected));
  } catch (e) {
    status.hidden = false;
    status.className = 'status error';
    status.textContent = `Couldn't load · ஏற்ற முடியவில்லை (${e.message})`;
  }
}

function drawWeekdays() {
  $('weekdays').innerHTML = state.weekdays.map((w, i) => `<div>${WEEKDAYS_TA_SHORT[i]}<small>${esc(w.en.slice(0, 3))}</small></div>`).join('');
}

function drawGrid() {
  const { from, to } = gridRange();
  const cells = [];
  for (let d = from; d <= to; d = new Date(d.getTime() + DAY)) {
    const key = iso(d);
    const day = state.days.get(key);
    const classes = ['cell'];
    if (d.getUTCMonth() !== state.month) classes.push('other');
    if (day && day.holiday) classes.push('holiday');
    if (key === state.today) classes.push('today');
    if (key === state.selected) classes.push('selected');
    const tamilDay = day ? `<span class="t">${day.tamilDate.day}</span>` : '';
    const monthStart = day && day.tamilDate.day === 1 ? `<span class="m">${esc(day.tamilDate.month.ta)}</span>` : '';
    const marks = day
      ? (day.holiday ? '<i class="dot alert"></i>' : '') + (day.importantDates.length ? '<i class="dot"></i>' : '')
      : '';
    cells.push(`<button class="${classes.join(' ')}" data-date="${key}"><span class="g">${d.getUTCDate()}</span>${tamilDay}${monthStart}<span class="marks">${marks}</span></button>`);
  }
  $('grid').innerHTML = cells.join('');
}

function renderDetail(day) {
  const panel = $('detail');
  if (!day) {
    panel.innerHTML = '';
    return;
  }
  const enDate = new Date(`${day.date}T00:00:00Z`).toLocaleDateString('en-GB', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC',
  });
  const fact = (label, el, extra = '') =>
    `<div><dt>${label}</dt><dd>${esc(bi(el.name))}${el.endsAt ? ` <small>→ ${hm(el.endsAt)}</small>` : ''}${extra}</dd></div>`;
  const chips = day.importantDates.map((x) =>
    `<li class="chip${x.holiday ? ' holiday' : ''}">${esc(bi(x.title))}</li>`).join('');
  const timings = day.timings.map((t) =>
    `<li class="${t.quality.toLowerCase()}"><span>${esc(bi(t.type))}${t.gowri ? ` · ${esc(bi(t.gowri.name))}` : ''}</span><time>${hm(t.start)} – ${hm(t.end)}</time></li>`).join('');
  const chandrashtamam = `<p class="warn">சந்திராஷ்டமம் · Chandrashtamam — ${esc(bi(day.chandrashtamam.rasi))}<br><small>${esc(day.chandrashtamam.nakshatras)}</small></p>`;

  panel.innerHTML = `
    <div>
      <p class="en-date">${esc(enDate)}</p>
      <h2>${esc(bi(day.weekday))}</h2>
      <p class="ta-date">${esc(bi(day.tamilDate.month))} ${day.tamilDate.day} · ${esc(bi(day.tamilDate.year))}</p>
    </div>
    ${day.holiday || chips ? `<ul class="chips">${day.holiday ? '<li class="chip holiday">விடுமுறை · Holiday</li>' : ''}${chips}</ul>` : ''}
    <dl class="facts">
      <div><dt>உதயம் · Sunrise</dt><dd>${hm(day.sunrise)}</dd></div>
      <div><dt>அஸ்தமனம் · Sunset</dt><dd>${hm(day.sunset)}</dd></div>
      ${fact('திதி · Tithi', day.tithi, `<br><small>${esc(bi(day.paksha))}</small>`)}
      ${fact('நட்சத்திரம் · Nakshatram', day.nakshatram)}
      ${fact('யோகம் · Yogam', day.yogam)}
      <div><dt>சந்திர ராசி · Moon sign</dt><dd>${esc(bi(day.moonRasi))}</dd></div>
    </dl>
    <section>
      <h3>நேரங்கள் · Timings</h3>
      <ul class="timings">${timings}</ul>
    </section>
    ${chandrashtamam}
  `;
}

function shift(months) {
  const next = new Date(Date.UTC(state.year, state.month + months, 1));
  state.year = next.getUTCFullYear();
  state.month = next.getUTCMonth();
  render();
}

$('prev').onclick = () => shift(-1);
$('next').onclick = () => shift(1);
$('today').onclick = () => {
  if (!state.today) return;
  state.year = Number(state.today.slice(0, 4));
  state.month = Number(state.today.slice(5, 7)) - 1;
  state.selected = state.today;
  render();
};

$('grid').onclick = (e) => {
  const cell = e.target.closest('.cell');
  if (!cell) return;
  state.selected = cell.dataset.date;
  drawGrid();
  renderDetail(state.days.get(state.selected));
  if (matchMedia('(max-width: 899px)').matches) $('detail').scrollIntoView({ behavior: 'smooth', block: 'start' });
};

$('place').onchange = (e) => {
  state.place = e.target.value;
  remember('place', state.place);
  state.selected = '';
  render();
};

const now = new Date();
state.year = now.getFullYear();
state.month = now.getMonth();
const savedPlace = recall('place');
if (savedPlace && [...$('place').options].some((o) => o.value === savedPlace)) {
  state.place = savedPlace;
  $('place').value = savedPlace;
}
render();

if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js');
