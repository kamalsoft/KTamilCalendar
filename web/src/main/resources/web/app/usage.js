// Anonymous app-open counts, for the developer. Only the country and state of the chosen place are sent, never the
// city, coordinates or any device identifier. Off unless the user turns it on in Settings, and off until USAGE_URL is set.

// The Google Apps Script web app (see tools/usage/Code.gs). Empty means the feature is off.
export const USAGE_URL = 'https://script.google.com/macros/s/AKfycbxcSZYKyP8E4Wc9ZjSMjHa68UpDOTitgtacS2ZgDbOs4DeGc662wpD8r1jIUg4wrsuSCw/exec';

/** Sends one open. Called once per page load, and only when the user has chosen to share. */
export function sendOpen(settings, place, platform) {
  if (!USAGE_URL || !settings.shareOpens || !place) return;
  // no-cors: the app does not need the reply, and Apps Script does not allow reading it from another site.
  fetch(USAGE_URL, {
    method: 'POST',
    mode: 'no-cors',
    headers: { 'Content-Type': 'text/plain' },
    body: JSON.stringify({ platform, country: place.countryCode || '', state: place.admin1 || '' }),
  }).catch(() => { /* offline: the open is simply not counted */ });
}

/** Reads the counts (JSONP, because Apps Script cannot be read by a page on another site with fetch). */
export function fetchUsage(key) {
  if (!USAGE_URL) return Promise.reject(new Error('Usage counts are not set up.'));
  return new Promise((resolve, reject) => {
    const name = `ktcUsage${Date.now()}`;
    const script = document.createElement('script');
    const done = () => { delete window[name]; script.remove(); };
    window[name] = (data) => { done(); resolve(data); };
    script.onerror = () => { done(); reject(new Error('Could not reach the counts.')); };
    script.src = `${USAGE_URL}?key=${encodeURIComponent(key)}&callback=${name}`;
    document.head.appendChild(script);
  });
}
