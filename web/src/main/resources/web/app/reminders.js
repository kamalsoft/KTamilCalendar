// Reminders while the app is open: 10 minutes before Rahu Kalam, and when each Nalla Neram starts.
// Web apps can't wake a closed phone for these, so this is an in-page reminder (see README).
import { zonedMs, MS_DAY } from '../engine/civil.js';

const MAX_DELAY = 2 ** 31 - 1;
let timers = [];

export function clearReminders() {
  timers.forEach(clearTimeout);
  timers = [];
}

/** Schedules today's reminders for [day] (epoch day) in [zone]. [notify] receives {title, body}. */
export function scheduleReminders(day, zone, timings, settings, notify) {
  clearReminders();
  const now = Date.now();
  const at = (hms, offsetSec = 0) => {
    const [h, m, s] = hms.split(':').map(Number);
    return zonedMs(day, h * 3600 + m * 60 + s + offsetSec, zone);
  };
  const plan = [];
  if (settings.rahuReminder) plan.push({ when: at(timings.rahuKalam.start, -600), title: settings.language === 'en' ? 'Rahu Kalam' : 'ராகு காலம் · Rahu Kalam', body: `Starts at ${timings.rahuKalam.start.slice(0, 5)} · 10 minutes to go` });
  if (settings.nallaReminder) timings.nallaNeram.forEach((w) => plan.push({ when: at(w.start), title: settings.language === 'en' ? 'Nalla Neram' : 'நல்ல நேரம் · Nalla Neram', body: `Begins now (${w.start.slice(0, 5)}–${w.end.slice(0, 5)})` }));
  for (const p of plan) {
    const delay = p.when - now;
    if (delay <= 0 || delay > MS_DAY) continue;
    timers.push(setTimeout(() => notify(p), Math.min(delay, MAX_DELAY)));
  }
}

/** Shows a system notification when allowed, and always the in-page banner. */
export function notifyUser(p, banner) {
  banner(`${p.title} — ${p.body}`);
  if ('Notification' in window && Notification.permission === 'granted') {
    try { new Notification(p.title, { body: p.body, icon: 'icon-192.png' }); } catch (e) { /* not supported here */ }
  }
}
