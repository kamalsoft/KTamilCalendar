// Port of domain/model LocationPolicy and the GeoLocation presets.
export const PLACES = [
  { key: 'chennai', nameTa: 'சென்னை', nameEn: 'Chennai', latitude: 13.0827, longitude: 80.2707, zoneId: 'Asia/Kolkata', countryCode: 'IN', admin1: 'Tamil Nadu', countryName: 'India' },
  { key: 'singapore', nameTa: 'சிங்கப்பூர்', nameEn: 'Singapore', latitude: 1.3521, longitude: 103.8198, zoneId: 'Asia/Singapore', countryCode: 'SG', admin1: 'Singapore', countryName: 'Singapore' },
  { key: 'colombo', nameTa: 'கொழும்பு', nameEn: 'Colombo', latitude: 6.9271, longitude: 79.8612, zoneId: 'Asia/Colombo', countryCode: 'LK', admin1: 'Western Province', countryName: 'Sri Lanka' },
  { key: 'naperville', nameTa: 'நேப்பர்வில்', nameEn: 'Naperville', latitude: 41.7508, longitude: -88.1535, zoneId: 'America/Chicago', countryCode: 'US', admin1: 'Illinois', countryName: 'United States' },
  { key: 'london', nameTa: 'லண்டன்', nameEn: 'London', latitude: 51.5074, longitude: -0.1278, zoneId: 'Europe/London', countryCode: 'GB', admin1: 'England', countryName: 'United Kingdom' },
];

const isUs = (loc) => loc.countryCode === 'US';
const isTamilNadu = (loc) => loc.countryCode === 'IN' && loc.admin1 === 'Tamil Nadu';

export function localHolidayRegions(loc) {
  const out = new Set();
  if (isUs(loc)) out.add('US_FEDERAL');
  if (isUs(loc) && loc.admin1 === 'Illinois') out.add('ILLINOIS');
  if (isTamilNadu(loc)) out.add('TAMIL_NADU');
  return out;
}

export function schoolDistricts(loc) {
  const out = new Set();
  if (isUs(loc) && loc.admin1 === 'Illinois' && loc.nameEn.toLowerCase() === 'naperville') {
    out.add('SCHOOL_D203');
    out.add('SCHOOL_D204');
  }
  return out;
}

export const timingConvention = (loc) => (loc.countryCode === 'IN' ? 'TRADITIONAL_6AM' : 'SUNRISE_BASED');
export const usesImperialUnits = (loc) => ['US', 'LR', 'MM'].includes(loc.countryCode);

/** Label used across the UI: "சென்னை · Chennai (13.08°N, 80.27°E)". */
export function placeLabel(loc) {
  const coords = `${Math.abs(loc.latitude).toFixed(2)}°${loc.latitude >= 0 ? 'N' : 'S'}, ${Math.abs(loc.longitude).toFixed(2)}°${loc.longitude >= 0 ? 'E' : 'W'}`;
  const name = loc.nameTa && loc.nameTa !== loc.nameEn ? `${loc.nameTa} · ${loc.nameEn}` : loc.nameEn;
  return `${name} (${coords})`;
}
