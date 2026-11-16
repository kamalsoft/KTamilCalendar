// Uttarayanam and Dakshinayanam from the Sun's sidereal longitude (Lahiri), at the place's local noon.
// Uttarayanam: the Sun from Makara (270 degrees, about 14 January) to the end of Mithuna.
// Dakshinayanam: the Sun from Karka (90 degrees, about 16 July) to the end of Dhanusu.
import * as A from './astro.js';
import { epochDay, wallClock, zonedMs } from './civil.js';

export const AYANAM_NAMES = {
  UTTARAYANAM: ['உத்தராயணம்', 'Uttarayanam'],
  DAKSHINAYANAM: ['தட்சிணாயனம்', 'Dakshinayanam'],
};

/** Kind of ayanam for a sidereal Sun longitude in degrees. */
export const ayanamOfSun = (sunLon) => (sunLon >= 90 && sunLon < 270 ? 'DAKSHINAYANAM' : 'UTTARAYANAM');

/** Epoch day a Sun crossing at [jd] counts from: the next day if the crossing is after local sunset. */
const countsFrom = (jd, zone, latitude, longitude) => {
  const day = wallClock(A.instantOf(jd), zone).day;
  const [, setJd] = A.sunriseSunset(day, latitude, longitude);
  return jd >= setJd ? day + 1 : day;
};

/** Julian day of the local noon of an epoch day. */
export const noonJd = (epochDay, zone) => A.julianDay(zonedMs(epochDay, 12 * 3600, zone));

/** Epoch day of a Julian day in the place's time zone. */
const localDay = (jd, zone) => wallClock(A.instantOf(jd), zone).day;

/**
 * Ayanam facts for an epoch day: the kind, the days since it began, and the day the next one starts.
 * The start is found by stepping back one day at a time, then bisecting the change to about a second.
 */
export function ayanamFacts(epochDay, place) {
  const { zoneId: zone, latitude, longitude } = place;
  const jd = noonJd(epochDay, zone);
  const kind = ayanamOfSun(A.sunSidereal(jd));
  let t = jd;
  for (let i = 0; i < 200 && ayanamOfSun(A.sunSidereal(t - 1)) === kind; i++) t -= 1;
  let lo = t - 1, hi = t;
  for (let i = 0; i < 20; i++) {
    const mid = (lo + hi) / 2;
    if (ayanamOfSun(A.sunSidereal(mid)) === kind) hi = mid; else lo = mid;
  }
  const next = A.findCrossing(jd, 200, 0.5, kind === 'UTTARAYANAM' ? 90 : 270, A.sunSidereal);
  return {
    kind,
    daysIn: Math.floor(jd - hi),
    startDay: countsFrom(hi, zone, latitude, longitude),
    nextKind: kind === 'UTTARAYANAM' ? 'DAKSHINAYANAM' : 'UTTARAYANAM',
    nextDay: next === null ? null : countsFrom(next, zone, latitude, longitude),
  };
}

/** Ayanam kind for every day of a year, as [epochDay, kind] (uses the Sun at local noon). */
export function ayanamYear(year, zone) {
  const out = [];
  const from = epochDay(year, 1, 1);
  const to = epochDay(year, 12, 31);
  for (let e = from; e <= to; e++) out.push([e, ayanamOfSun(A.sunSidereal(noonJd(e, zone)))]);
  return out;
}

const hmsOf = (sec) => {
  const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60), s = Math.floor(sec % 60);
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
};

/**
 * Every ayanam change in a year, with the exact moment the Sun crosses the boundary (270 or 90 degrees, Lahiri).
 * Each change also gets the day it takes effect: if the crossing is after local sunset, the new ayanam (and the new
 * Tamil solar month) begins at the next sunrise (validator, Sankranti rule).
 */
export function ayanamChanges(year, zone, latitude, longitude) {
  const out = [];
  const endJd = A.julianDay(zonedMs(epochDay(year, 12, 31), 24 * 3600, zone));
  let cur = A.julianDay(zonedMs(epochDay(year, 1, 1), 0, zone));
  while (cur < endJd) {
    const a = A.findCrossing(cur, 200, 0.5, 270, A.sunSidereal);
    const b = A.findCrossing(cur, 200, 0.5, 90, A.sunSidereal);
    const candidates = [[a, 'UTTARAYANAM'], [b, 'DAKSHINAYANAM']].filter(([t]) => t !== null && t < endJd);
    if (!candidates.length) break;
    const [jd, kind] = candidates.sort((x, y) => x[0] - y[0])[0];
    const local = wallClock(A.instantOf(jd), zone);
    const [, setJd] = A.sunriseSunset(local.day, latitude, longitude);
    const effectiveDay = jd >= setJd ? local.day + 1 : local.day;
    out.push({ kind, jd, day: local.day, time: hmsOf(local.sec), effectiveDay, afterSunset: jd >= setJd });
    cur = jd + 0.01;
  }
  return out;
}
