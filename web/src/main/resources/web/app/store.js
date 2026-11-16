// Settings and places, kept on this device (localStorage). Everything here is a convenience: if storage is
// blocked the app still works with the defaults.
const KEY = 'ktc.settings.v2';

export const DEFAULTS = {
  theme: 'GRAPHITE',
  janmaStar: -1,                        // birth star (0 Aswini .. 26 Revathi), -1 = not set
  janmaRasi: -1,                        // birth Rasi (0 Mesham .. 11 Meenam), -1 = not set
  monthView: 'grid',                    // grid | page: the Month tab as a grid or as a wall-calendar page
  skin: 'CLASSIC',                      // CLASSIC | GLASS | PAPER: how cards are drawn
  place: { preset: 'chennai' },          // { preset: key } or { custom: GeoLocation }
  recentPlaces: [],                     // up to 5 custom places
  pinnedPlaces: [],                     // [{ id, label, place }]: places kept for the offline year pack
  autoLocate: false,                    // use the current location when the app opens (if already allowed)
  hiddenSections: [],                   // tab ids hidden from the tab bar
  hiddenFaiths: [],                     // religion ids
  weather: true,
  usHolidays: true,
  schoolCalendars: true,
  tamilNaduHolidays: true,
  animations: true,
  weatherAnimations: true,
  clock24h: false,
  tamilOnly: false,
  language: 'en',                       // en | ta-en | te-en | ml-en | kn-en | hi-en
  units: 'AUTO',                        // AUTO | IMPERIAL | METRIC
  convention: 'AUTO',                   // AUTO | TRADITIONAL_6AM | SUNRISE_BASED
  ambient: true,
  ambientMinutes: 2,
  rahuReminder: false,
  nallaReminder: false,
  slokaMusic: true,
  slokaVoice: false,
  music: { raga: 'MOHANAM', tempo: 'SLOW', bell: 'NORMAL', pitch: 'F', mix: 'BALANCED' },
  disclaimerSeen: false,
};

export function loadSettings() {
  try {
    const saved = JSON.parse(localStorage.getItem(KEY) || '{}');
    return { ...DEFAULTS, ...saved, music: { ...DEFAULTS.music, ...(saved.music || {}) } };
  } catch (e) {
    return structuredClone(DEFAULTS);
  }
}

export function saveSettings(s) {
  try { localStorage.setItem(KEY, JSON.stringify(s)); } catch (e) { /* not persisted */ }
}
