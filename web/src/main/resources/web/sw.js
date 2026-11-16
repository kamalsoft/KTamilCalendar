// Offline copy for the installed app. Network first, so a deploy shows up on the next load;
// the last good copy of each file is kept for when the phone is offline.
const CACHE = 'ktamilcalendar-v9';
const SHELL = [
  './',
  'index.html',
  'style.css',
  'app.js',
  'app/store.js',
  'app/i18n.js',
  'watermark.svg',
  'i18n/te.json',
  'i18n/ml.json',
  'i18n/kn.json',
  'i18n/hi.json',
  'app/weather.js',
  'app/sloka.js',
  'app/reminders.js',
  'app/version.js',
  'engine/astro.js',
  'engine/civil.js',
  'engine/content.js',
  'engine/engine.js',
  'engine/calendar.js',
  'engine/holidays.js',
  'engine/icons.js',
  'engine/locations.js',
  'engine/rasi.js',
  'engine/school_calendars.json',
  'manifest.webmanifest',
  'apple-touch-icon.png',
  'icon-192.png',
  'icon-512.png',
];

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)));
  // No skipWaiting here: the page asks the user (see the update banner) and then sends SKIP_WAITING.
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('message', (e) => {
  if (e.data === 'SKIP_WAITING') self.skipWaiting();
});

self.addEventListener('fetch', (e) => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.origin !== location.origin) return;

  e.respondWith(
    fetch(e.request)
      .then((res) => {
        if (res.ok) {
          const copy = res.clone(); // clone before the page reads the body
          caches.open(CACHE).then((c) => c.put(e.request, copy));
        }
        return res;
      })
      .catch(() => caches.match(e.request)),
  );
});
