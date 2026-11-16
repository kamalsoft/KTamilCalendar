// Port of shared/.../core/astro/TamilCalendarEngine.kt (rules, muhurtham, timings, festivals).
// Dates are epoch days; times are seconds of the local day; instants are ms since the epoch.
import * as A from './astro.js';
import { epochDay, isoOf, dayOfWeek, wallClock, zonedMs, hms, dayOfMonth, monthOf, yearOf, MS_DAY } from './civil.js';
import { CONTENT } from './content.js';
import { timingConvention } from './locations.js';
import { easter } from './holidays.js';

const NAK_SPAN = 360 / 27;
const LAGNA_STEP_DAYS = 10 / 1440;
const RAHU_SEGMENT = [8, 2, 7, 5, 6, 4, 3];
const YAMA_SEGMENT = [5, 4, 3, 2, 1, 7, 6];
const KULIGAI_SEGMENT = [7, 6, 5, 4, 3, 2, 1];
const GOWRI_CYCLE = [0, 1, 2, 3, 4, 5, 6, 7]; // indices into CONTENT.enums.Gowri
const AVOIDED_MUHURTHAM_MONTHS = new Set([3, 5, 8]);
const SHUKLA_MUHURTHAM_TITHIS = new Set([1, 2, 4, 6, 9, 10, 12]);
const KRISHNA_MUHURTHAM_TITHIS = new Set([1, 2, 4]);
const MUHURTHAM_STARS = new Set([3, 4, 9, 11, 12, 14, 16, 18, 20, 21, 22, 25, 26]);
const AT = { SUNRISE: 'SUNRISE', SUNSET: 'SUNSET', NOON: 'NOON', APARAHNA: 'APARAHNA', MOONRISE_K4: 'MOONRISE_K4', MIDNIGHT: 'MIDNIGHT' };

const names = CONTENT.names;
const pair = (p) => ({ ta: p[0], en: p[1] });
const mod = (a, n) => ((a % n) + n) % n;

/** One engine instance per place and timing convention, like the APK's TamilCalendarEngine. */
export class TamilCalendarEngine {
  constructor(location, convention = timingConvention(location)) {
    this.location = location;
    this.zone = location.zoneId;
    this.convention = convention;
  }

  /** All days from epoch day [from] to [to], inclusive. Returns plain objects (see day shape in calendar.md). */
  compute(from, to) {
    if (to < from) throw new Error('to < from');
    const sky = new SkyContext(this, from - 45, to + 45);
    const cores = new Map();
    const core = (d) => {
      if (!cores.has(d)) cores.set(d, new DayCore(this, d, sky));
      return cores.get(d);
    };
    const dates = [];
    for (let d = from; d <= to; d++) dates.push(d);
    const events = this.computeFestivals(dates, core);
    return dates.map((d) => this.buildDay(core(d), core(d + 1), core(d - 1), events.get(d) || []));
  }

  // ------------------------------------------------------------ astronomy per day
  sunriseSunsetOf(day) {
    return A.sunriseSunset(day, this.location.latitude, this.location.longitude);
  }

  buildDay(c, next, prev, events) {
    const sunrise = this.localTime(c.sunriseJd);
    const sunset = this.localTime(c.sunsetJd);
    const tithiIdx = c.tithi(AT.SUNRISE);
    const starIdx = c.star(AT.SUNRISE);
    const yogaIdx = A.floorDiv(A.yogaSum(c.sunriseJd), NAK_SPAN) % 27;
    const moonRasi = A.floorDiv(A.moonSidereal(c.sunriseJd), 30);
    const tithiNames = tithiNamesOf(tithiIdx);

    const panchangam = {
      tithi: { index: tithiIdx, nameTa: tithiNames[0], nameEn: tithiNames[1], endsAt: this.endOf(c.sunriseJd, ((tithiIdx + 1) % 30) * 12, A.elongation) },
      nakshatram: { index: starIdx, nameTa: names.nakshatras[starIdx][0], nameEn: names.nakshatras[starIdx][1], endsAt: this.endOf(c.sunriseJd, ((starIdx + 1) % 27) * NAK_SPAN, A.moonSidereal) },
      yogam: { index: yogaIdx, nameTa: names.yogas[yogaIdx][0], nameEn: names.yogas[yogaIdx][1], endsAt: this.endOf(c.sunriseJd, ((yogaIdx + 1) % 27) * NAK_SPAN, A.yogaSum) },
      paksha: tithiIdx < 15 ? 'SHUKLA' : 'KRISHNA',
      moonRasi,
      moonRasiEndsAt: this.endOf(c.sunriseJd, ((moonRasi + 1) % 12) * 30, A.moonSidereal, 3),
      sunRasi: A.floorDiv(A.sunSidereal(c.sunriseJd), 30),
      lagnam: this.lagnaSegments(c.sunriseJd, next.sunriseJd),
      karanam: A.floorDiv(A.elongation(c.sunriseJd), 6),
      sraardhaTithi: this.sraardhaTithiOf(c.sunriseJd, c.sunsetJd),
    };

    const timings = this.timingsFor(c.day, sunrise, sunset);
    const tags = new Set();
    if (this.observed(c, next, prev, 14, AT.SUNRISE)) tags.add('POURNAMI');
    if (this.observed(c, next, prev, 29, AT.SUNRISE)) tags.add('AMAVASAI');
    if (this.observed(c, next, prev, 10, AT.SUNRISE) || this.observed(c, next, prev, 25, AT.SUNRISE)) tags.add('EKADASI');
    if (this.observed(c, next, prev, 12, AT.SUNSET) || this.observed(c, next, prev, 27, AT.SUNSET)) tags.add('PRADOSHAM');
    if (this.starObserved(c, next, prev, 2, AT.SUNRISE)) tags.add('KRITHIGAI');
    if (this.observed(c, next, prev, 5, AT.SUNSET)) tags.add('SASHTI');
    if (this.observed(c, next, prev, 28, AT.MIDNIGHT)) tags.add('MASA_SIVARATRI');
    if (this.observed(c, next, prev, 18, AT.MOONRISE_K4)) tags.add('SANKATAHARA');
    if (this.starObserved(c, next, prev, 21, AT.SUNRISE)) tags.add('THIRUVONAM');
    const weekday = dayOfWeek(c.day);
    if (c.tamilMonth === 5 && weekday === 6) tags.add('PURATTASI_SANI');
    if (c.tamilMonth === 3 && weekday === 5) tags.add('AADI_VELLI');

    const allEvents = events.slice();
    if (this.isMuhurthamCandidate(c, tithiIdx, starIdx)) {
      tags.add('SUBHA_MUHURTHAM');
      const window = timings.nallaNeram.find((w) => +w.start.slice(0, 2) < 12) || timings.nallaNeram[0];
      allEvents.push({
        date: isoOf(c.day), kind: 'MUHURTHAM', titleTa: `சுப முகூர்த்தம் (${panchangam.paksha === 'SHUKLA' ? 'வளர்பிறை' : 'தேய்பிறை'})`,
        titleEn: 'Subha Muhurtham', isHoliday: false, source: 'COMPUTED', detail: window ? rangeLabel(window.start, window.end) : null,
        region: null, religion: null, deity: null,
      });
    }

    const chandraRasi = (moonRasi + 5) % 12;
    return {
      date: isoOf(c.day),
      epochDay: c.day,
      weekdayIndex: weekday,
      sunrise,
      sunset,
      tamilDate: this.tamilDate(c),
      panchangam,
      timings,
      chandrashtamam: { rasi: chandraRasi, nakshatras: this.nakshatrasIn(chandraRasi), until: panchangam.moonRasiEndsAt },
      tags: [...tags],
      events: allEvents,
    };
  }

  tamilDate(c) {
    const m = c.tamilMonth;
    const d = isoOf(c.day);
    const year = yearOf(c.day);
    const mo = monthOf(c.day);
    const startYear = m >= 9 || (m === 8 && mo <= 2) ? year - 1 : year;
    return {
      monthIndex: m,
      day: c.tamilDay,
      yearCycleIndex: mod(startYear - 1987, 60),
      thiruvalluvarAandu: startYear + 31 + (m >= 9 ? 1 : 0),
      iso: d,
    };
  }

  observed(c, next, prev, x, at) {
    const matches = (a, b) => {
      const t0 = a.tithi(at);
      return t0 === x || (t0 === (x + 29) % 30 && b.tithi(at) === (x + 1) % 30);
    };
    return matches(c, next) && prev.tithi(at) !== x;
  }

  starObserved(c, next, prev, n, at) {
    const s0 = c.star(at);
    const hit = s0 === n || (s0 === (n + 26) % 27 && next.star(at) === (n + 1) % 27);
    return hit && prev.star(at) !== n;
  }

  endOf(fromJd, target, angle, maxDays = 2) {
    const t = A.findCrossing(fromJd, maxDays, 0.25, target, angle);
    return t === null ? null : this.localDateTime(t);
  }

  /** Sraardha tithi: the tithi that covers the whole window 13:30–16:00 local, Amavasai excluded (see Kotlin). */
  sraardhaTithiOf(sunriseJd, sunsetJd) {
    // Aparahna Kala: the 4th fifth of the daytime. The tithi covering more than half of it is the Sraardha tithi.
    const unit = (sunsetJd - sunriseJd) / 5;
    const samples = 16;
    const counts = new Array(30).fill(0);
    for (let i = 0; i < samples; i++) counts[A.floorDiv(A.elongation(sunriseJd + (3 + (i + 0.5) / samples) * unit), 12)]++;
    const best = counts.indexOf(Math.max(...counts));
    return counts[best] * 2 > samples && best !== 29 ? best : null;
  }

  /** The rising signs from sunrise to the next sunrise (same scan as Kotlin's lagnaSegments). */
  lagnaSegments(sunriseJd, nextSunriseJd) {
    const rasiAt = (jd) => A.floorDiv(A.ascendantSidereal(jd, this.location.latitude, this.location.longitude), 30);
    let rasi = rasiAt(sunriseJd);
    const out = [{ rasi, start: this.localDateTime(sunriseJd) }];
    let t = sunriseJd;
    while (t < nextSunriseJd) {
      const t1 = Math.min(t + LAGNA_STEP_DAYS, nextSunriseJd);
      const r1 = rasiAt(t1);
      if (r1 !== rasi) {
        let lo = t, hi = t1;
        for (let i = 0; i < 20; i++) {
          const mid = (lo + hi) / 2;
          if (rasiAt(mid) === rasi) lo = mid; else hi = mid;
        }
        out.push({ rasi: r1, start: this.localDateTime(hi) });
        rasi = r1;
      }
      t = t1;
    }
    return out;
  }

  localTime(jd) {
    const w = wallClock(A.instantOf(jd), this.zone);
    return hms(w.sec);
  }

  localDateTime(jd) {
    const w = wallClock(A.instantOf(jd), this.zone);
    return `${isoOf(w.day)}T${hms(w.sec)}`;
  }

  // ------------------------------------------------------------ timing windows
  timingsFor(day, sunrise, sunset) {
    const sunriseSec = toSec(sunrise), sunsetSec = toSec(sunset);
    // Validator: the daytime is divided into eight equal parts from sunrise, at every place.
    const dayStart = sunriseSec;
    const part = Math.trunc((sunsetSec - sunriseSec) / 8);
    const w = dayOfWeek(day);
    const segment = (type, n, gowri = null) => {
      const s = dayStart + (n - 1) * part;
      return { type, start: hms(s), end: hms(s + part), startSec: s, endSec: s + part, gowri };
    };
    const rahu = segment('RAHU_KALAM', RAHU_SEGMENT[w]);
    const yama = segment('YAMAGANDAM', YAMA_SEGMENT[w]);
    const kuligai = segment('KULIGAI', KULIGAI_SEGMENT[w]);
    const gowriAll = [1, 2, 3, 4, 5, 6, 7, 8].map((n) => segment('GOWRI_NALLA_NERAM', n, GOWRI_CYCLE[(w + n - 1) % 8]));
    const gowriGood = gowriAll.filter((g) => CONTENT.enums.Gowri[g.gowri][3]);

    // A good window never overlaps an avoid window: Rahu Kalam, Yamagandam and Kuligai are cut out of the good times.
    const blocked = [rahu, yama, kuligai].map((x) => [x.startSec, x.endSec]);
    const free = [];
    const gowriFree = [];
    for (const g of gowriGood) {
      let pieces = [[g.startSec, g.endSec]];
      for (const [bs, be] of blocked) {
        pieces = pieces.flatMap(([s, e]) => {
          if (be <= s || bs >= e) return [[s, e]];
          const out = [];
          if (bs > s) out.push([s, bs]);
          if (be < e) out.push([be, e]);
          return out;
        });
      }
      for (const p of pieces) {
        if (p[1] - p[0] >= 30 * 60) gowriFree.push({ type: 'GOWRI_NALLA_NERAM', start: hms(p[0]), end: hms(p[1]), startSec: p[0], endSec: p[1], gowri: g.gowri });
        const last = free[free.length - 1];
        if (last && last[1] === p[0]) last[1] = p[1];
        else free.push([p[0], p[1]]);
      }
    }
    const nallaNeram = free.filter(([s, e]) => e - s >= 30 * 60).map(([s, e]) => ({ type: 'NALLA_NERAM', start: hms(s), end: hms(e), gowri: null }));
    const strip = (x) => ({ type: x.type, start: x.start, end: x.end, gowri: x.gowri });
    return {
      nallaNeram,
      gowriNallaNeram: gowriFree.map(strip),
      rahuKalam: strip(rahu),
      yamagandam: strip(yama),
      kuligai: strip(kuligai),
    };
  }

  nakshatrasIn(rasi) {
    const lo = rasi * 30, hi = lo + 30;
    const out = [];
    for (let k = 0; k < 27; k++) if (k * NAK_SPAN < hi - 1e-6 && (k + 1) * NAK_SPAN > lo + 1e-6) out.push(k);
    return out;
  }

  isMuhurthamCandidate(c, tithi, star) {
    if (AVOIDED_MUHURTHAM_MONTHS.has(c.tamilMonth)) return false;
    if (c.lunarMonth(AT.SUNRISE).adhika) return false;
    const w = dayOfWeek(c.day);
    if (w === 2 || w === 6) return false;
    const tithiOk = tithi < 15 ? SHUKLA_MUHURTHAM_TITHIS.has(tithi) : KRISHNA_MUHURTHAM_TITHIS.has(tithi - 15);
    return tithiOk && MUHURTHAM_STARS.has(star);
  }

  // ------------------------------------------------------------ festivals & holidays
  computeFestivals(dates, core) {
    const tithiObs = (d, x, at) => this.observed(core(d), core(d + 1), core(d - 1), x, at);
    const starObs = (d, n, at) => this.starObserved(core(d), core(d + 1), core(d - 1), n, at);
    const solar = (d, month, day) => core(d).tamilMonth === month && core(d).tamilDay === day;
    const inSolar = (d, month) => core(d).tamilMonth === month;
    const lunar = (d, month, at) => { const lm = core(d).lunarMonth(at); return lm.index === month && !lm.adhika; };
    const nearFullMoon = (d, at) => { const t = core(d).tithi(at); return t >= 11 && t <= 16; };
    const fixed = (d, m, day) => monthOf(d) === m && dayOfMonth(d) === day;
    const HINDU_KINDS = new Set(['FESTIVAL', 'VIRATHAM']);

    const R = (titleTa, titleEn, kind, isHoliday, matches, opts = {}) => ({
      titleTa, titleEn, kind, isHoliday, matches,
      deity: opts.deity ?? null,
      religion: opts.religion !== undefined ? opts.religion : (HINDU_KINDS.has(kind) ? 'HINDU' : null),
    });

    const rules = [
      R('தமிழ்ப் புத்தாண்டு', 'Tamil New Year', 'FESTIVAL', true, (d) => solar(d, 0, 1)),
      R('போகி', 'Bhogi', 'FESTIVAL', false, (d) => solar(d + 1, 9, 1)),
      R('தைப் பொங்கல்', 'Thai Pongal', 'FESTIVAL', true, (d) => solar(d, 9, 1)),
      R('திருவள்ளுவர் தினம்', 'Thiruvalluvar Day', 'GOVT_HOLIDAY', true, (d) => solar(d, 9, 2)),
      R('உழவர் திருநாள்', 'Uzhavar Thirunal', 'GOVT_HOLIDAY', true, (d) => solar(d, 9, 3)),
      R('ஆடிப் பெருக்கு', 'Aadi Perukku', 'FESTIVAL', false, (d) => solar(d, 3, 18), { deity: 'AMMAN' }),
      R('தைப்பூசம்', 'Thaipusam', 'FESTIVAL', false, (d) => inSolar(d, 9) && starObs(d, 7, AT.SUNRISE) && nearFullMoon(d, AT.SUNRISE), { deity: 'MURUGAN' }),
      R('பங்குனி உத்திரம்', 'Panguni Uthiram', 'FESTIVAL', false, (d) => inSolar(d, 11) && starObs(d, 11, AT.SUNRISE) && nearFullMoon(d, AT.SUNRISE), { deity: 'MURUGAN' }),
      R('வைகாசி விசாகம்', 'Vaikasi Visakam', 'FESTIVAL', false, (d) => inSolar(d, 1) && starObs(d, 15, AT.SUNRISE) && nearFullMoon(d, AT.SUNRISE), { deity: 'MURUGAN' }),
      R('கார்த்திகை தீபம்', 'Karthigai Deepam', 'FESTIVAL', false, (d) => inSolar(d, 7) && starObs(d, 2, AT.SUNSET) && core(d).tithi(AT.SUNSET) >= 9 && core(d).tithi(AT.SUNSET) <= 14, { deity: 'SHIVA' }),
      R('வைகுண்ட ஏகாதசி', 'Vaikunta Ekadasi', 'VIRATHAM', false, (d) => inSolar(d, 8) && tithiObs(d, 10, AT.SUNRISE), { deity: 'PERUMAL' }),
      R('ஆடி அமாவாசை', 'Aadi Amavasai', 'VIRATHAM', false, (d) => inSolar(d, 3) && tithiObs(d, 29, AT.SUNRISE)),
      R('தை அமாவாசை', 'Thai Amavasai', 'VIRATHAM', false, (d) => inSolar(d, 9) && tithiObs(d, 29, AT.SUNRISE)),
      R('மஹாளய அமாவாசை', 'Mahalaya Amavasai', 'VIRATHAM', false, (d) => lunar(d, 5, AT.SUNRISE) && tithiObs(d, 29, AT.SUNRISE)),
      R('யுகாதி', 'Ugadi', 'FESTIVAL', true, (d) => lunar(d, 0, AT.SUNRISE) && tithiObs(d, 0, AT.SUNRISE)),
      R('ஸ்ரீ ராம நவமி', 'Sri Rama Navami', 'FESTIVAL', false, (d) => lunar(d, 0, AT.NOON) && tithiObs(d, 8, AT.NOON), { deity: 'PERUMAL' }),
      R('கிருஷ்ண ஜெயந்தி', 'Krishna Jayanthi', 'FESTIVAL', true, (d) => lunar(d, 4, AT.MIDNIGHT) && tithiObs(d, 22, AT.MIDNIGHT), { deity: 'PERUMAL' }),
      R('விநாயகர் சதுர்த்தி', 'Vinayagar Chathurthi', 'FESTIVAL', true, (d) => lunar(d, 5, AT.NOON) && tithiObs(d, 3, AT.NOON), { deity: 'VINAYAGAR' }),
      R('ஆயுத பூஜை', 'Ayutha Pooja', 'FESTIVAL', true, (d) => lunar(d, 6, AT.APARAHNA) && tithiObs(d, 8, AT.APARAHNA), { deity: 'AMMAN' }),
      R('விஜயதசமி', 'Vijayadasami', 'FESTIVAL', true, (d) => lunar(d, 6, AT.APARAHNA) && tithiObs(d, 9, AT.APARAHNA), { deity: 'AMMAN' }),
      R('தீபாவளி', 'Deepavali', 'FESTIVAL', true, (d) => lunar(d, 6, AT.SUNRISE) && tithiObs(d, 28, AT.SUNRISE)),
      R('மகா சிவராத்திரி', 'Maha Shivaratri', 'VIRATHAM', false, (d) => lunar(d, 10, AT.MIDNIGHT) && tithiObs(d, 28, AT.MIDNIGHT), { deity: 'SHIVA' }),
      R('கந்த சஷ்டி · சூரசம்ஹாரம்', 'Skanda Sashti (Soorasamharam)', 'FESTIVAL', false, (d) => inSolar(d, 6) && tithiObs(d, 5, AT.SUNSET), { deity: 'MURUGAN' }),
      R('ஆருத்ரா தரிசனம்', 'Arudra Darshanam', 'FESTIVAL', false, (d) => inSolar(d, 8) && starObs(d, 5, AT.SUNRISE) && nearFullMoon(d, AT.SUNRISE), { deity: 'SHIVA' }),
      R('மாசி மகம்', 'Masi Magam', 'FESTIVAL', false, (d) => inSolar(d, 10) && starObs(d, 9, AT.SUNRISE) && nearFullMoon(d, AT.SUNRISE), { deity: 'SHIVA' }),
      R('ஆடிப் பூரம்', 'Aadi Pooram', 'FESTIVAL', false, (d) => inSolar(d, 3) && starObs(d, 10, AT.SUNRISE), { deity: 'AMMAN' }),
      R('சித்ரா பௌர்ணமி', 'Chitra Pournami', 'FESTIVAL', false, (d) => inSolar(d, 0) && tithiObs(d, 14, AT.SUNRISE), { deity: 'AMMAN' }),
      R('அனுமன் ஜெயந்தி', 'Hanuman Jayanthi', 'FESTIVAL', false, (d) => inSolar(d, 8) && tithiObs(d, 29, AT.SUNRISE), { deity: 'PERUMAL' }),
      R('நவராத்திரி ஆரம்பம்', 'Navaratri begins', 'FESTIVAL', false, (d) => lunar(d, 6, AT.SUNRISE) && tithiObs(d, 0, AT.SUNRISE), { deity: 'AMMAN' }),
      R('ஆவணி அவிட்டம்', 'Avani Avittam', 'FESTIVAL', false, (d) => inSolar(d, 4) && starObs(d, 22, AT.SUNRISE) && nearFullMoon(d, AT.SUNRISE)),
      R('ஓணம்', 'Onam', 'FESTIVAL', false, (d) => inSolar(d, 4) && starObs(d, 21, AT.SUNRISE), { deity: 'PERUMAL' }),
      R('அட்சய திருதியை', 'Akshaya Tritiya', 'FESTIVAL', false, (d) => lunar(d, 1, AT.SUNRISE) && tithiObs(d, 2, AT.SUNRISE)),
      R('ஹோலி', 'Holi', 'FESTIVAL', false, (d) => lunar(d, 11, AT.SUNRISE) && tithiObs(d, 15, AT.SUNRISE)),
      R('ரக்ஷா பந்தன்', 'Raksha Bandhan', 'FESTIVAL', false, (d) => lunar(d, 4, AT.APARAHNA) && tithiObs(d, 14, AT.APARAHNA)),
      R('மகாவீர் ஜெயந்தி', 'Mahavir Jayanti', 'RELIGIOUS', false, (d) => lunar(d, 0, AT.SUNRISE) && tithiObs(d, 12, AT.SUNRISE), { religion: 'JAIN' }),
      R('பர்யுஷன் ஆரம்பம் (ஸ்வேதாம்பரர்)', 'Paryushan begins (Shvetambara)', 'RELIGIOUS', false, (d) => lunar(d + 7, 5, AT.SUNRISE) && tithiObs(d + 7, 3, AT.SUNRISE), { religion: 'JAIN' }),
      R('சம்வத்சரி', 'Samvatsari', 'RELIGIOUS', false, (d) => lunar(d, 5, AT.SUNRISE) && tithiObs(d, 3, AT.SUNRISE), { religion: 'JAIN' }),
      R('தச லட்சண பர்வம் ஆரம்பம் (திகம்பரர்)', 'Das Lakshana Parva begins (Digambara)', 'RELIGIOUS', false, (d) => lunar(d, 5, AT.SUNRISE) && tithiObs(d, 4, AT.SUNRISE), { religion: 'JAIN' }),
      R('மகாவீர் நிர்வாணம் (தீபாவளி)', 'Mahavir Nirvana (Diwali)', 'RELIGIOUS', false, (d) => lunar(d, 6, AT.SUNSET) && tithiObs(d, 29, AT.SUNSET), { religion: 'JAIN' }),
      R('வைசாகி', 'Vaisakhi', 'RELIGIOUS', false, (d) => solar(d, 0, 1), { religion: 'SIKH' }),
      R('குரு நானக் ஜெயந்தி', 'Guru Nanak Gurpurab', 'RELIGIOUS', false, (d) => lunar(d, 7, AT.SUNRISE) && tithiObs(d, 14, AT.SUNRISE), { religion: 'SIKH' }),
      R('புத்த பூர்ணிமா · வேசாக்', 'Buddha Purnima (Vesak)', 'RELIGIOUS', false, (d) => lunar(d, 1, AT.SUNRISE) && tithiObs(d, 14, AT.SUNRISE), { religion: 'BUDDHIST' }),
      R('ஆங்கிலப் புத்தாண்டு', "New Year's Day", 'GOVT_HOLIDAY', true, (d) => fixed(d, 1, 1)),
      R('குடியரசு தினம்', 'Republic Day', 'GOVT_HOLIDAY', true, (d) => fixed(d, 1, 26)),
      R('அம்பேத்கர் ஜெயந்தி', 'Ambedkar Jayanthi', 'GOVT_HOLIDAY', true, (d) => fixed(d, 4, 14)),
      R('மே தினம்', 'May Day', 'GOVT_HOLIDAY', true, (d) => fixed(d, 5, 1)),
      R('இந்திய சுதந்திர தினம்', 'Independence Day (India)', 'GOVT_HOLIDAY', true, (d) => fixed(d, 8, 15)),
      R('காந்தி ஜெயந்தி', 'Gandhi Jayanthi', 'GOVT_HOLIDAY', true, (d) => fixed(d, 10, 2)),
      R('கிறிஸ்துமஸ்', 'Christmas', 'GOVT_HOLIDAY', true, (d) => fixed(d, 12, 25), { religion: 'CHRISTIAN' }),
      R('புனித வெள்ளி', 'Good Friday', 'GOVT_HOLIDAY', true, (d) => d === easter(yearOf(d)) - 2, { religion: 'CHRISTIAN' }),
    ];

    const out = new Map();
    const lastHit = new Map();
    const first = dates[0], last = dates[dates.length - 1];
    for (let d = first - 25; d <= last; d++) {
      rules.forEach((rule, idx) => {
        if (!rule.matches(d)) return;
        const prev = lastHit.get(idx);
        lastHit.set(idx, d);
        if (prev !== undefined && d - prev < 25) return;
        if (d < first) return;
        if (!out.has(d)) out.set(d, []);
        out.get(d).push({
          date: isoOf(d), kind: rule.kind, titleTa: rule.titleTa, titleEn: rule.titleEn, isHoliday: rule.isHoliday,
          source: 'COMPUTED', detail: null, region: rule.isHoliday ? 'TAMIL_NADU' : null,
          religion: rule.religion, deity: rule.deity,
        });
      });
    }
    return out;
  }
}

export function tithiNamesOf(index) {
  if (index === 14) return ['பௌர்ணமி', 'Pournami'];
  if (index === 29) return ['அமாவாசை', 'Amavasai'];
  return names.tithis[index % 15];
}

function rangeLabel(start, end) {
  return `${periodOf(start)} ${clockOf(start)} – ${clockOf(end)}`;
}
function periodOf(hms) {
  const h = +hms.slice(0, 2);
  if (h <= 3) return 'பின்னிரவு';
  if (h <= 11) return 'காலை';
  if (h <= 15) return 'மதியம்';
  if (h <= 18) return 'மாலை';
  return 'இரவு';
}
function clockOf(hms) {
  const [h0, m0, s0] = hms.split(':').map(Number);
  let total = h0 * 3600 + m0 * 60 + s0 + 30;
  total = Math.floor(total / 60) * 60;
  const h = Math.floor(total / 3600) % 24, m = Math.floor((total % 3600) / 60);
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${h12}:${String(m).padStart(2, '0')}`;
}
const toSec = (hms) => {
  const [h, m, s] = hms.split(':').map(Number);
  return h * 3600 + m * 60 + s;
};

/** Per-day astronomy, cached (port of DayCore). */
class DayCore {
  constructor(engine, day, sky) {
    this.engine = engine;
    this.day = day;
    this.sky = sky;
    const [rise, set] = engine.sunriseSunsetOf(day);
    this.sunriseJd = rise;
    this.sunsetJd = set;
    this.midnightJd = A.julianDay(zonedMs(day + 1, 0, engine.zone));
    this.cache = new Map();
  }
  jd(at) {
    switch (at) {
      case AT.SUNRISE: return this.sunriseJd;
      case AT.SUNSET: return this.sunsetJd;
      case AT.NOON: return (this.sunriseJd + this.sunsetJd) / 2;
      case AT.APARAHNA: return this.sunriseJd + 0.7 * (this.sunsetJd - this.sunriseJd);
      case AT.MOONRISE_K4: return this.sunsetJd + 3.25 / 24;
      case AT.MIDNIGHT: return this.midnightJd;
      default: throw new Error(at);
    }
  }
  tithi(at) {
    const key = 'T' + at;
    if (!this.cache.has(key)) this.cache.set(key, A.floorDiv(A.elongation(this.jd(at)), 12) % 30);
    return this.cache.get(key);
  }
  star(at) {
    const key = 'S' + at;
    if (!this.cache.has(key)) this.cache.set(key, A.floorDiv(A.moonSidereal(this.jd(at)), NAK_SPAN) % 27);
    return this.cache.get(key);
  }
  get tamilMonthStart() {
    if (!this._tms) this._tms = this.sky.monthStartFor(this.day);
    return this._tms;
  }
  get tamilMonth() { return this.tamilMonthStart[1]; }
  get tamilDay() { return this.day - this.tamilMonthStart[0] + 1; }
  lunarMonth(at) { return this.sky.lunarMonthAt(this.jd(at)); }
}

/** Sankrantis and new moons around the requested range (port of SkyContext). */
class SkyContext {
  constructor(engine, fromDay, toDay) {
    this.monthStarts = []; // [civilEpochDay, rasi]
    this.newMoons = [];
    this.newMoonSunRasi = [];
    const zone = engine.zone, loc = engine.location;
    const startJd = A.julianDay(zonedMs(fromDay, 0, zone));
    const endJd = A.julianDay(zonedMs(toDay, 0, zone));
    let jd = startJd - 35;
    let rasi = A.floorDiv(A.sunSidereal(jd), 30);
    while (jd <= endJd) {
      const next = (rasi + 1) % 12;
      const sankranti = A.findCrossing(jd, 40, 1.0, next * 30, A.sunSidereal);
      if (sankranti === null) break;
      const civil = wallClock(A.instantOf(sankranti), zone).day;
      const sunset = engine.sunriseSunsetOf(civil)[1];
      this.monthStarts.push([sankranti < sunset ? civil : civil + 1, next]);
      rasi = next;
      jd = sankranti + 1;
    }
    jd = startJd - 30;
    while (jd <= endJd + 30) {
      const nm = A.findCrossing(jd, 35, 1.0, 0, A.elongation);
      if (nm === null) break;
      this.newMoons.push(nm);
      this.newMoonSunRasi.push(A.floorDiv(A.sunSidereal(nm), 30));
      jd = nm + 1;
    }
    this.loc = loc;
  }
  monthStartFor(day) {
    let found = null;
    for (const ms of this.monthStarts) if (ms[0] <= day) found = ms;
    if (!found) throw new Error(`No sankranti before ${isoOf(day)}`);
    return found;
  }
  lunarMonthAt(jd) {
    let i = -1;
    this.newMoons.forEach((nm, k) => { if (nm <= jd) i = k; });
    if (i < 0) i = 0;
    const rasi = this.newMoonSunRasi[i];
    const adhika = i + 1 < this.newMoonSunRasi.length && this.newMoonSunRasi[i + 1] === rasi;
    return { index: (rasi + 1) % 12, adhika };
  }
}
