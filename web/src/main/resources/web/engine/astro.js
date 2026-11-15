// Port of shared/.../core/astro/Astro.kt and Planets.kt. Same formulas, so the results match the APK.
const RAD = Math.PI / 180;
const J2000 = 2451545.0;
const DELTA_T_DAYS = 69.2 / 86400.0;
const MS_DAY = 86400000;

export const julianDay = (ms) => ms / MS_DAY + 2440587.5;
export const instantOf = (jd) => Math.round((jd - 2440587.5) * MS_DAY);

export const norm360 = (x) => {
  const r = x % 360;
  return r < 0 ? r + 360 : r;
};
const norm180 = (x) => {
  const r = norm360(x);
  return r > 180 ? r - 360 : r;
};
const centuries = (jd) => (jd + DELTA_T_DAYS - J2000) / 36525.0;

function nutationLongitude(t) {
  const omega = 125.04452 - 1934.136261 * t;
  const lSun = 280.4665 + 36000.7698 * t;
  const lMoon = 218.3165 + 481267.8813 * t;
  return (-17.2 * Math.sin(omega * RAD) - 1.32 * Math.sin(2 * lSun * RAD) -
    0.23 * Math.sin(2 * lMoon * RAD) + 0.21 * Math.sin(2 * omega * RAD)) / 3600.0;
}

export function sunLongitude(jd) {
  const t = centuries(jd);
  const l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t;
  const m = (357.52911 + 35999.05029 * t - 0.0001537 * t * t) * RAD;
  const c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * Math.sin(m) +
    (0.019993 - 0.000101 * t) * Math.sin(2 * m) + 0.000289 * Math.sin(3 * m);
  return norm360(l0 + c - 0.005691611 + nutationLongitude(t));
}

export function moonLongitude(jd) {
  const t = centuries(jd);
  const t2 = t * t, t3 = t2 * t, t4 = t3 * t;
  const lp = 218.3164477 + 481267.88123421 * t - 0.0015786 * t2 + t3 / 538841.0 - t4 / 65194000.0;
  const d = 297.8501921 + 445267.1114034 * t - 0.0018819 * t2 + t3 / 545868.0 - t4 / 113065000.0;
  const m = 357.5291092 + 35999.0502909 * t - 0.0001536 * t2 + t3 / 24490000.0;
  const mp = 134.9633964 + 477198.8675055 * t + 0.0087414 * t2 + t3 / 69699.0 - t4 / 14712000.0;
  const f = 93.2720950 + 483202.0175233 * t - 0.0036539 * t2 - t3 / 3526000.0 + t4 / 863310000.0;
  const e = 1.0 - 0.002516 * t - 0.0000074 * t2;

  let sum = 0;
  for (let i = 0; i < MOON_TERMS.length; i += 5) {
    const cd = MOON_TERMS[i], cm = MOON_TERMS[i + 1], cmp = MOON_TERMS[i + 2], cf = MOON_TERMS[i + 3], coeff = MOON_TERMS[i + 4];
    const arg = (cd * d + cm * m + cmp * mp + cf * f) * RAD;
    const eFactor = Math.abs(cm) === 1 ? e : Math.abs(cm) === 2 ? e * e : 1.0;
    sum += coeff * eFactor * Math.sin(arg);
  }
  const a1 = (119.75 + 131.849 * t) * RAD;
  const a2 = (53.09 + 479264.290 * t) * RAD;
  sum += 3958.0 * Math.sin(a1) + 1962.0 * Math.sin((lp - f) * RAD) + 318.0 * Math.sin(a2);
  return norm360(lp + sum / 1000000.0 + nutationLongitude(t));
}

export const ayanamsa = (jd) => {
  const t = centuries(jd);
  return 23.857092 + 1.396971 * t + 0.000308 * t * t;
};
export const sunSidereal = (jd) => norm360(sunLongitude(jd) - ayanamsa(jd));
export const moonSidereal = (jd) => norm360(moonLongitude(jd) - ayanamsa(jd));

/** Sidereal ascendant (Lagna) at a UT Julian day: the zodiac degree rising on the eastern horizon. */
export function ascendantSidereal(jd, latitude, longitude) {
  const t = (jd - J2000) / 36525;
  const gmst = norm360(280.46061837 + 360.98564736629 * (jd - J2000) + 0.000387933 * t * t - (t * t * t) / 38710000);
  const lst = (gmst + longitude) * RAD;
  const eps = (23.4392911 - 0.0130042 * t) * RAD;
  const phi = latitude * RAD;
  const tropical = Math.atan2(Math.cos(lst), -(Math.sin(lst) * Math.cos(eps) + Math.tan(phi) * Math.sin(eps))) / RAD;
  return norm360(tropical - ayanamsa(jd));
}
export const elongation = (jd) => norm360(moonLongitude(jd) - sunLongitude(jd));
export const yogaSum = (jd) => norm360(sunSidereal(jd) + moonSidereal(jd));

/** First instant in [fromJd, fromJd + maxDays] where the increasing [angle] reaches [target]; null if none. */
export function findCrossing(fromJd, maxDays, stepDays, target, angle) {
  let t0 = fromJd;
  let g0 = norm180(angle(t0) - target);
  if (g0 === 0) return t0;
  const end = fromJd + maxDays;
  while (t0 < end) {
    const t1 = Math.min(t0 + stepDays, end);
    const g1 = norm180(angle(t1) - target);
    if (g0 < 0 && g1 >= 0 && g1 - g0 < 180) {
      let lo = t0, hi = t1;
      for (let k = 0; k < 40; k++) {
        const mid = (lo + hi) / 2;
        if (norm180(angle(mid) - target) < 0) lo = mid; else hi = mid;
      }
      return (lo + hi) / 2;
    }
    t0 = t1;
    g0 = g1;
  }
  return null;
}

/** Sunrise and sunset (upper limb) as [riseJd, setJd] for the civil date [day] (epoch day). */
export function sunriseSunset(day, latitude, longitude) {
  const n = day + 2440588.0 - J2000;
  const jStar = n - longitude / 360.0;
  const m = norm360(357.5291 + 0.98560028 * jStar);
  const mr = m * RAD;
  const c = 1.9148 * Math.sin(mr) + 0.02 * Math.sin(2 * mr) + 0.0003 * Math.sin(3 * mr);
  const lambda = norm360(m + c + 180.0 + 102.9372) * RAD;
  const transit = J2000 + jStar + 0.0053 * Math.sin(mr) - 0.0069 * Math.sin(2 * lambda);
  const decl = Math.asin(Math.sin(lambda) * Math.sin(23.4397 * RAD));
  const phi = latitude * RAD;
  const cosW = (Math.sin(-0.833 * RAD) - Math.sin(phi) * Math.sin(decl)) / (Math.cos(phi) * Math.cos(decl));
  const w = Math.acos(Math.min(1, Math.max(-1, cosW))) / RAD;
  return [transit - w / 360.0, transit + w / 360.0];
}

export const floorDiv = (angle, span) => Math.floor(angle / span);

/** Meeus Table 47.A (longitude): D, M, M', F, coefficient (1e-6 degrees). */
const MOON_TERMS = [
  0, 0, 1, 0, 6288774, 2, 0, -1, 0, 1274027, 2, 0, 0, 0, 658314, 0, 0, 2, 0, 213618,
  0, 1, 0, 0, -185116, 0, 0, 0, 2, -114332, 2, 0, -2, 0, 58793, 2, -1, -1, 0, 57066,
  2, 0, 1, 0, 53322, 2, -1, 0, 0, 45758, 0, 1, -1, 0, -40923, 1, 0, 0, 0, -34720,
  0, 1, 1, 0, -30383, 2, 0, 0, -2, 15327, 0, 0, 1, 2, -12528, 0, 0, 1, -2, 10980,
  4, 0, -1, 0, 10675, 0, 0, 3, 0, 10034, 4, 0, -2, 0, 8548, 2, 1, -1, 0, -7888,
  2, 1, 0, 0, -6766, 1, 0, -1, 0, -5163, 1, 1, 0, 0, 4987, 2, -1, 1, 0, 4036,
  2, 0, 2, 0, 3994, 4, 0, 0, 0, 3861, 2, 0, -3, 0, 3665, 0, 1, -2, 0, -2689,
  2, 0, -1, 2, -2602, 2, -1, -2, 0, 2390, 1, 0, 1, 0, -2348, 2, -2, 0, 0, 2236,
  0, 1, 2, 0, -2120, 0, 2, 0, 0, -2069, 2, -2, -1, 0, 2048, 2, 0, 1, -2, -1773,
  2, 0, 0, 2, -1595, 4, -1, -1, 0, 1215, 0, 0, 2, 2, -1110, 3, 0, -1, 0, -892,
  2, 1, 1, 0, -810, 4, -1, -2, 0, 759, 0, 2, -1, 0, -713, 2, 2, -1, 0, -700,
  2, 1, -2, 0, 691, 2, -1, 0, -2, 596, 4, 0, 1, 0, 549, 0, 0, 4, 0, 537,
  4, -1, 0, 0, 520, 1, 0, -2, 0, -487, 2, 1, 0, -2, -399, 0, 0, 2, -2, -381,
  1, 1, 1, 0, 351, 3, 0, -2, 0, -340, 4, 0, -3, 0, 330, 2, -1, 2, 0, 327,
  0, 2, 1, 0, -323, 1, 1, -1, 0, 299, 2, 0, 3, 0, 294,
];

// ---- Planets.kt: Jupiter, Saturn (JPL Keplerian elements) and the lunar node.
const EARTH = [1.00000261, 0.00000562, 0.01671123, -0.00004392, -0.00001531, -0.01294668, 100.46457166, 35999.37244981, 102.93768193, 0.32327364, 0, 0];
const JUPITER = [5.20288700, -0.00011607, 0.04838624, -0.00013253, 1.30439695, -0.00183714, 34.39644051, 3034.74612775, 14.72847983, 0.21252668, 100.47390909, 0.20469106];
const SATURN = [9.53667594, -0.00125060, 0.05386179, -0.00050991, 2.48599187, 0.00193609, 49.95424423, 1222.49362201, 92.59887831, -0.41897216, 113.66242448, -0.28867794];

function helio(el, t) {
  const [a0, aR, e0, eR, i0, iR, l0, lR, w0, wR, o0, oR] = el;
  const a = a0 + aR * t, e = e0 + eR * t;
  const i = (i0 + iR * t) * RAD, l = l0 + lR * t, w = w0 + wR * t, o = (o0 + oR * t) * RAD;
  const argP = w * RAD - o;
  let m = ((((l - w) % 360) + 360) % 360) * RAD;
  if (m > Math.PI) m -= 2 * Math.PI;
  let ea = m + e * Math.sin(m);
  for (let k = 0; k < 8; k++) ea -= (ea - e * Math.sin(ea) - m) / (1 - e * Math.cos(ea));
  const xp = a * (Math.cos(ea) - e);
  const yp = a * Math.sqrt(1 - e * e) * Math.sin(ea);
  const cw = Math.cos(argP), sw = Math.sin(argP), co = Math.cos(o), so = Math.sin(o), ci = Math.cos(i), si = Math.sin(i);
  return [
    (cw * co - sw * so * ci) * xp + (-sw * co - cw * so * ci) * yp,
    (cw * so + sw * co * ci) * xp + (-sw * so + cw * co * ci) * yp,
    (sw * si) * xp + (cw * si) * yp,
  ];
}

function geocentricTropical(el, jd) {
  const t = (jd - 2451545.0) / 36525.0;
  const p = helio(el, t);
  const e = helio(EARTH, t);
  return norm360(Math.atan2(p[1] - e[1], p[0] - e[0]) / RAD);
}

export const jupiterSidereal = (jd) => norm360(geocentricTropical(JUPITER, jd) - ayanamsa(jd));
export const saturnSidereal = (jd) => norm360(geocentricTropical(SATURN, jd) - ayanamsa(jd));
export function rahuSidereal(jd) {
  const t = (jd - 2451545.0) / 36525.0;
  return norm360(125.0445479 - 1934.1362891 * t + 0.0020754 * t * t - ayanamsa(jd));
}
export const ketuSidereal = (jd) => norm360(rahuSidereal(jd) + 180);
