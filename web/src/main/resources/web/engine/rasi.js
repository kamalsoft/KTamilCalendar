// Port of RasiPalanCalculator (Moon-house predictions), TransitForecast (gochara) and the Gochara tables.
import * as A from './astro.js';
import { CONTENT } from './content.js';
import { zonedMs } from './civil.js';

const mod = (a, n) => ((a % n) + n) % n;
const SUN_GOOD = new Set([3, 6, 10, 11]);
const JUP_GOOD = new Set([2, 5, 7, 9, 11]);
const SAT_GOOD = new Set([3, 6, 11]);
const RAHU_GOOD = new Set([3, 6, 11]);

/** Rasi palan for every rasi, given today's Moon sign: the Moon's house counted from each rasi. */
export function rasiPalan(moonRasi) {
  return Array.from({ length: 12 }, (_, rasi) => {
    const house = mod(moonRasi - rasi, 12) + 1;
    const msg = CONTENT.rasiMessages[house - 1];
    return { rasi, moonHouse: house, outlook: msg.outlook, messageTa: msg.ta, messageEn: msg.en };
  });
}

/** Rasi (0 = Mesham) of each graha on a civil date at local noon. */
export function skyPositions(day, zone) {
  const jd = A.julianDay(zonedMs(day, 12 * 3600, zone));
  const rasiOf = (lon) => Math.floor(lon / 30) % 12;
  return {
    sun: rasiOf(A.sunSidereal(jd)), moon: rasiOf(A.moonSidereal(jd)),
    jupiter: rasiOf(A.jupiterSidereal(jd)), saturn: rasiOf(A.saturnSidereal(jd)),
    rahu: rasiOf(A.rahuSidereal(jd)), ketu: rasiOf(A.ketuSidereal(jd)),
  };
}

const GRAHA_LABELS = { sun: 'சூ', moon: 'சந்', jupiter: 'கு', saturn: 'ச', rahu: 'ரா', ketu: 'கே' };
/** Short Tamil labels of the grahas sitting in [rasi] (for the chart). */
export const grahasIn = (sky, rasi) => Object.keys(GRAHA_LABELS).filter((g) => sky[g] === rasi).map((g) => GRAHA_LABELS[g]);

const house = (planet, rasi) => mod(planet - rasi, 12) + 1;
const outlook = (good, of) => (good * 2 > of ? 'GOOD' : good * 2 === of ? 'NEUTRAL' : 'CAUTION');

/** Monthly (Sun + Jupiter) and yearly (Jupiter, Saturn, Rahu) gochara for [rasi]. */
export function gochara(rasi, sky) {
  const sunH = house(sky.sun, rasi), jupH = house(sky.jupiter, rasi), satH = house(sky.saturn, rasi), rahH = house(sky.rahu, rasi);
  const g = CONTENT.gochara;
  const sun = g.sun[sunH - 1], jup = g.jupiter[jupH - 1], sat = g.saturn[satH - 1];
  const rahu = RAHU_GOOD.has(rahH) ? g.rahuGood : g.rahuBad;
  const sunIn = CONTENT.names.rasis[sky.sun];
  const monthGood = [SUN_GOOD.has(sunH), JUP_GOOD.has(jupH)].filter(Boolean).length;
  const yearGood = [JUP_GOOD.has(jupH), SAT_GOOD.has(satH), RAHU_GOOD.has(rahH)].filter(Boolean).length;
  return {
    month: {
      ta: `சூரியன் ${sunIn[0]} ராசியில் (${sunH}-ஆம் இடம்): ${sun[0]}\nகுரு ${jupH}-ஆம் இடம்: ${jup[0]}`,
      en: `Sun in ${sunIn[1]} (house ${sunH}): ${sun[1]}\nJupiter in house ${jupH}: ${jup[1]}`,
      outlook: outlook(monthGood, 2),
    },
    year: {
      ta: `குரு ${jupH}-ஆம் இடம்: ${jup[0]}\nசனி ${satH}-ஆம் இடம்: ${sat[0]}\n${rahu[0]}`,
      en: `Jupiter in house ${jupH}: ${jup[1]}\nSaturn in house ${satH}: ${sat[1]}\n${rahu[1]}`,
      outlook: outlook(yearGood, 3),
    },
  };
}
