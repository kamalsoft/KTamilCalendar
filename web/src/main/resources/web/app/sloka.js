// The daily sloka: which verse the day gets (port of Slokas.forDay), read-aloud, and generated temple music.
import { CONTENT } from '../engine/content.js';

export function slokaFor(day) {
  const tags = new Set(day.tags);
  const byTag = (() => {
    if (tags.has('SASHTI') || tags.has('KRITHIGAI')) return 'THIRUPPUGAZH';
    if (tags.has('PRADOSHAM') || tags.has('MASA_SIVARATRI')) return 'SIVAPURANAM';
    if (tags.has('SANKATAHARA')) return 'VINAYAGAR';
    if (tags.has('EKADASI') || tags.has('THIRUVONAM') || tags.has('PURATTASI_SANI')) return 'SUPRABHATAM';
    if (tags.has('AADI_VELLI') || tags.has('POURNAMI')) return 'NARAYANI';
    return null;
  })();
  const byWeekday = ['GAYATRI', 'SIVAPURANAM', 'THIRUPPUGAZH', 'VINAYAGAR', 'KURAL', 'NARAYANI', 'SUPRABHATAM'][day.weekdayIndex];
  return CONTENT.slokas[byTag || byWeekday];
}

let speaking = false;
/** Read the verse aloud in Tamil if the device has a Tamil voice; otherwise the English meaning. */
export function speak(sloka) {
  if (!('speechSynthesis' in window)) return;
  window.speechSynthesis.cancel();
  const voices = window.speechSynthesis.getVoices();
  const tamil = voices.find((v) => v.lang && v.lang.toLowerCase().startsWith('ta'));
  const u = new SpeechSynthesisUtterance(tamil ? sloka.textTa : sloka.meaningEn);
  if (tamil) u.voice = tamil;
  u.lang = tamil ? 'ta-IN' : 'en-IN';
  u.rate = 0.85;
  u.onend = () => { speaking = false; };
  speaking = true;
  window.speechSynthesis.speak(u);
}
export const stopSpeaking = () => { if ('speechSynthesis' in window) window.speechSynthesis.cancel(); speaking = false; };
export const isSpeaking = () => speaking;

// Ragas as semitone offsets from Sa (Mohanam: Sa Ri2 Ga2 Pa Dha2 ...; Hindolam and Revathi use their own degrees).
const RAGAS = {
  MOHANAM: [0, 2, 4, 7, 9, 12],
  HINDOLAM: [0, 3, 5, 8, 10, 12],
  REVATHI: [0, 2, 5, 7, 9, 12],
};
const TEMPO = { SLOW: 1.1, MEDIUM: 0.8, FAST: 0.6 };
const PITCH = { C: 0, D: 2, E: 4, F: 5, G: 7 };
const BELL_EVERY = { OFF: 0, SOFT: 14, NORMAL: 9, PROMINENT: 6 };
const LEVEL = { OFF: 0, SOFT: 0.12, NORMAL: 0.2, PROMINENT: 0.3 };

/**
 * Background temple music: tanpura drone, a plucked pattern (Pa, Sa', Sa', Sa), a slow flute-like phrase in
 * the chosen raga and a temple bell. Synthesised in the browser, so nothing is licensed or downloaded.
 */
export class TempleMusic {
  constructor(style) {
    this.style = style;
    this.ctx = null;
    this.timer = null;
  }
  start() {
    if (this.ctx) return;
    const Ctx = window.AudioContext || window.webkitAudioContext;
    if (!Ctx) return;
    const ctx = new Ctx();
    this.ctx = ctx;
    const st = this.style;
    const sa = 130.81 * Math.pow(2, (PITCH[st.pitch] ?? 5) / 12);
    const master = ctx.createGain();
    master.gain.value = st.mix === 'MUSIC' ? 0.5 : st.mix === 'DRONE' ? 0.3 : 0.4;
    master.connect(ctx.destination);
    // Tanpura drone: Sa, Pa and upper Sa with a slow swell.
    [[sa, 0.5], [sa * 1.5, 0.25], [sa * 2, 0.2]].forEach(([f, g]) => {
      const o = ctx.createOscillator(), amp = ctx.createGain();
      o.frequency.value = f; o.type = 'sine';
      amp.gain.value = g * 0.5;
      const lfo = ctx.createOscillator(), depth = ctx.createGain();
      lfo.frequency.value = 0.12; depth.gain.value = g * 0.15;
      lfo.connect(depth).connect(amp.gain);
      o.connect(amp).connect(master);
      o.start(); lfo.start();
    });
    const beat = TEMPO[st.tempo] ?? 1.1;
    const scale = RAGAS[st.raga] || RAGAS.MOHANAM;
    const contour = [0, 2, 4, 3, 2, 1, 0, 5]; // scale degrees of a simple phrase
    let next = ctx.currentTime + 0.3, bellAt = ctx.currentTime + 2, step = 0, pluck = 0;
    const pluckRatios = [1.5, 2, 2, 1];
    const bellEvery = BELL_EVERY[st.bell] ?? 9;
    const bellLevel = LEVEL[st.bell] ?? 0.2;
    const note = (f, when, dur, level, type) => {
      const o = ctx.createOscillator(), g = ctx.createGain();
      o.type = type; o.frequency.value = f;
      g.gain.setValueAtTime(0, when);
      g.gain.linearRampToValueAtTime(level, when + Math.min(0.15, dur * 0.3));
      g.gain.exponentialRampToValueAtTime(0.0001, when + dur);
      o.connect(g).connect(master);
      o.start(when); o.stop(when + dur + 0.05);
    };
    this.timer = setInterval(() => {
      const horizon = ctx.currentTime + 1.2;
      while (next < horizon) {
        if (st.mix !== 'DRONE') {
          const deg = contour[step % contour.length];
          note(sa * Math.pow(2, scale[deg] / 12) * 2, next, beat * 0.9, 0.08, 'sine');
          step++;
        }
        note(sa * pluckRatios[pluck % 4], next, 0.9, 0.05, 'triangle');
        pluck++;
        next += beat * 1.1;
      }
      while (bellEvery && bellAt < horizon) {
        note(1800, bellAt, 3, bellLevel * 0.5, 'sine');
        note(2700, bellAt, 2, bellLevel * 0.25, 'sine');
        bellAt += bellEvery;
      }
    }, 250);
  }
  stop() {
    clearInterval(this.timer);
    this.timer = null;
    if (this.ctx) this.ctx.close();
    this.ctx = null;
  }
}
