// Interface languages. Each label in the app is written as lb(tamil, english). The translations live in
// i18n/<code>.json, keyed by the Tamil text. A label with no translation shows English, so nothing is blank.
// Translations are reviewed by native speakers; see web/tools/check_translations.py for what is still missing.

export const LANGUAGES = [
  ['en', 'English'],
  ['ta-en', 'தமிழ் · English'],
  ['te-en', 'తెలుగు · English'],
  ['ml-en', 'മലയാളം · English'],
  ['kn-en', 'ಕನ್ನಡ · English'],
  ['hi-en', 'हिन्दी · English'],
];

const FILES = { 'te-en': 'te', 'ml-en': 'ml', 'kn-en': 'kn', 'hi-en': 'hi' };
const loaded = {};

// Loads the translation file for a language once. Until it arrives, labels show English.
export async function loadTranslations(lang) {
  if (!FILES[lang] || loaded[lang]) return;
  loaded[lang] = await fetch(`i18n/${FILES[lang]}.json`).then((r) => (r.ok ? r.json() : {})).catch(() => ({}));
}

// The label in the chosen language, or undefined when the language has no translation for it.
export const translated = (lang, ta) => loaded[lang]?.[ta];
