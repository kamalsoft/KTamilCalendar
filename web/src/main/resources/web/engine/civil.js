// Calendar-date and time-zone helpers. Dates are epoch days (integers, UTC-based) so arithmetic is exact.
export const MS_DAY = 86400000;

export const epochDay = (y, m, d) => Math.round(Date.UTC(y, m - 1, d) / MS_DAY);
export const fromEpochDay = (n) => {
  const dt = new Date(n * MS_DAY);
  return { y: dt.getUTCFullYear(), m: dt.getUTCMonth() + 1, d: dt.getUTCDate() };
};
const pad = (v) => String(v).padStart(2, '0');
export const isoOf = (n) => {
  const { y, m, d } = fromEpochDay(n);
  return `${y}-${pad(m)}-${pad(d)}`;
};
export const parseIso = (s) => {
  const [y, m, d] = s.split('-').map(Number);
  return epochDay(y, m, d);
};
/** Sunday = 0 … Saturday = 6 (1970-01-01 was a Thursday). */
export const dayOfWeek = (n) => (((n + 4) % 7) + 7) % 7;
export const monthOf = (n) => fromEpochDay(n).m;
export const yearOf = (n) => fromEpochDay(n).y;
export const dayOfMonth = (n) => fromEpochDay(n).d;

const formatters = new Map();
function formatterFor(zone) {
  if (!formatters.has(zone)) {
    formatters.set(zone, new Intl.DateTimeFormat('en-US', {
      timeZone: zone, hourCycle: 'h23', year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit',
    }));
  }
  return formatters.get(zone);
}

/** Wall-clock date and second-of-day in [zone] at instant [ms] (whole seconds, truncated like java.time). */
export function wallClock(ms, zone) {
  const p = {};
  for (const part of formatterFor(zone).formatToParts(new Date(ms))) p[part.type] = part.value;
  const wallMs = Date.UTC(+p.year, +p.month - 1, +p.day, +p.hour, +p.minute, +p.second);
  const day = Math.floor(wallMs / MS_DAY);
  return { day, sec: Math.round((wallMs - day * MS_DAY) / 1000) };
}

const offsetAt = (ms, zone) => {
  const w = wallClock(ms, zone);
  return w.day * MS_DAY + w.sec * 1000 - ms;
};

/** Instant (ms) of local wall time [day] + [sec] in [zone]. */
export function zonedMs(day, sec, zone) {
  const wall = day * MS_DAY + sec * 1000;
  const first = wall - offsetAt(wall, zone);
  return wall - offsetAt(first, zone);
}

export const hms = (sec) => {
  const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60), s = sec % 60;
  return `${pad(h)}:${pad(m)}:${pad(s)}`;
};
