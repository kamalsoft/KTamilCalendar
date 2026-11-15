// Weather from Open-Meteo (free, no key, CORS enabled). Results are cached for 20 minutes.
const API = 'https://api.open-meteo.com/v1/forecast';
const cache = new Map();
const TTL = 20 * 60 * 1000;

/** Scene for the animated background, from the WMO weather code. */
export function sceneOf(code, isDay) {
  if (code === 0 || code === 1) return isDay ? 'sun' : 'stars';
  if (code === 2 || code === 3) return 'cloud';
  if (code === 45 || code === 48) return 'fog';
  if (code >= 51 && code <= 67 || code >= 80 && code <= 82) return 'rain';
  if (code >= 71 && code <= 77 || code === 85 || code === 86) return 'snow';
  if (code >= 95) return 'storm';
  return 'cloud';
}

const LABELS = {
  0: 'Clear', 1: 'Mostly clear', 2: 'Partly cloudy', 3: 'Overcast', 45: 'Fog', 48: 'Rime fog',
  51: 'Light drizzle', 53: 'Drizzle', 55: 'Heavy drizzle', 61: 'Light rain', 63: 'Rain', 65: 'Heavy rain',
  66: 'Freezing rain', 67: 'Heavy freezing rain', 71: 'Light snow', 73: 'Snow', 75: 'Heavy snow', 77: 'Snow grains',
  80: 'Rain showers', 81: 'Rain showers', 82: 'Violent showers', 85: 'Snow showers', 86: 'Heavy snow showers',
  95: 'Thunderstorm', 96: 'Thunderstorm with hail', 99: 'Thunderstorm with heavy hail',
};
export const labelOf = (code) => LABELS[code] || 'Cloudy';

/** Current conditions, the next 12 hours and 7 days, for a point. Imperial units when [imperial] is true. */
export async function fetchWeather(lat, lon, imperial) {
  const key = `${lat.toFixed(3)},${lon.toFixed(3)},${imperial}`;
  const hit = cache.get(key);
  if (hit && Date.now() - hit.at < TTL) return hit.data;
  const params = new URLSearchParams({
    latitude: lat, longitude: lon, timezone: 'auto', forecast_hours: '12', forecast_days: '7',
    current: 'temperature_2m,weather_code,is_day,relative_humidity_2m,wind_speed_10m',
    hourly: 'temperature_2m,weather_code,precipitation_probability',
    daily: 'weather_code,temperature_2m_max,temperature_2m_min',
    temperature_unit: imperial ? 'fahrenheit' : 'celsius',
    wind_speed_unit: imperial ? 'mph' : 'kmh',
  });
  const res = await fetch(`${API}?${params}`);
  if (!res.ok) throw new Error(`weather HTTP ${res.status}`);
  const j = await res.json();
  const data = {
    current: { temp: j.current.temperature_2m, code: j.current.weather_code, isDay: !!j.current.is_day, humidity: j.current.relative_humidity_2m, wind: j.current.wind_speed_10m },
    hourly: j.hourly.time.slice(0, 12).map((t, i) => ({ time: t, temp: j.hourly.temperature_2m[i], code: j.hourly.weather_code[i], rain: j.hourly.precipitation_probability[i] })),
    daily: j.daily.time.map((t, i) => ({ date: t, code: j.daily.weather_code[i], max: j.daily.temperature_2m_max[i], min: j.daily.temperature_2m_min[i] })),
    unit: imperial ? '°F' : '°C', windUnit: imperial ? 'mph' : 'km/h',
  };
  cache.set(key, { at: Date.now(), data });
  return data;
}
