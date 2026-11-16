// Prints every Tamil and English pair in the generated content (names, enums, themes) as JSON: [[tamil, english], ...].
// Used by check_translations.py --export so the translation template covers the Panchangam names too.
const { CONTENT } = await import(new URL('../src/main/resources/web/engine/content.js', import.meta.url));
const hasTamil = (s) => /[஀-௿]/.test(s);
const out = new Map();
const add = (ta, en) => { if (hasTamil(ta) && en && !hasTamil(en) && !out.has(ta)) out.set(ta, en); };
const walk = (v) => {
  if (Array.isArray(v)) {
    const strings = v.filter((x) => typeof x === 'string');
    const ta = strings.find(hasTamil);
    const en = strings.find((x) => !hasTamil(x) && /[A-Za-z]/.test(x));
    if (ta && en && strings.length <= 4) add(ta, en);
    v.forEach(walk);
  } else if (v && typeof v === 'object') {
    if (typeof v.ta === 'string' && typeof v.en === 'string') add(v.ta, v.en);
    if (typeof v.nameTa === 'string' && typeof v.nameEn === 'string') add(v.nameTa, v.nameEn);
    // Any field pair such as titleTa / titleEn or messageTa / messageEn; a verse with no English partner is left out.
    for (const k of Object.keys(v)) if (k.endsWith('Ta') && typeof v[k] === 'string' && typeof v[k.slice(0, -2) + 'En'] === 'string') add(v[k], v[k.slice(0, -2) + 'En']);
    Object.values(v).forEach(walk);
  }
};
walk(CONTENT);
console.log(JSON.stringify([...out.entries()]));
