// Panchangam app-open counts, in a Google Sheet. Deploy as a web app (Execute as: Me; Who has access: Anyone),
// then paste the /exec URL into web/src/main/resources/web/app/usage.js as USAGE_URL.
//
// Stored per open: the time (UTC), the day, ISO week, month, platform, country code and state. Nothing else.
// Reading the counts needs the key kept in Script properties as USAGE_KEY (Project Settings → Script properties).

const SHEET_NAME = 'Opens';
const HEADER = ['time', 'day', 'week', 'month', 'platform', 'country', 'state'];

function doPost(e) {
  const body = JSON.parse((e.postData && e.postData.contents) || '{}');
  const now = new Date();
  const sheet = openSheet_();
  sheet.appendRow([
    now,
    Utilities.formatDate(now, 'UTC', 'yyyy-MM-dd'),
    isoWeek_(now),
    Utilities.formatDate(now, 'UTC', 'yyyy-MM'),
    clean_(body.platform, 20),
    clean_(body.country, 2).toUpperCase(),
    clean_(body.state, 60),
  ]);
  return ContentService.createTextOutput('ok');
}

// Summary for the About panel. Returns JSON, or JSONP when a callback is given.
function doGet(e) {
  const key = PropertiesService.getScriptProperties().getProperty('USAGE_KEY');
  const out = (obj) => {
    const text = JSON.stringify(obj);
    const callback = e.parameter.callback;
    return ContentService.createTextOutput(callback ? `${callback}(${text})` : text)
      .setMimeType(callback ? ContentService.MimeType.JAVASCRIPT : ContentService.MimeType.JSON);
  };
  if (!key || e.parameter.key !== key) return out({ error: 'not allowed' });

  const rows = openSheet_().getDataRange().getValues().slice(1);
  const count = (keyOf) => rows.reduce((acc, r) => { const k = keyOf(r); acc[k] = (acc[k] || 0) + 1; return acc; }, {});
  return out({
    total: rows.length,
    days: count((r) => r[1]),
    weeks: count((r) => r[2]),
    months: count((r) => r[3]),
    regions: count((r) => `${r[5]} · ${r[6]}`),
    platforms: count((r) => r[4]),
  });
}

function openSheet_() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName(SHEET_NAME) || ss.insertSheet(SHEET_NAME);
  if (sheet.getLastRow() === 0) sheet.appendRow(HEADER);
  return sheet;
}

function clean_(value, max) {
  return String(value || '').replace(/[\u0000-\u001f]/g, ' ').trim().slice(0, max);
}

// ISO week, e.g. 2026-W46.
function isoWeek_(date) {
  const d = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const day = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - day);
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  const week = Math.ceil(((d - yearStart) / 86400000 + 1) / 7);
  return `${d.getUTCFullYear()}-W${String(week).padStart(2, '0')}`;
}
