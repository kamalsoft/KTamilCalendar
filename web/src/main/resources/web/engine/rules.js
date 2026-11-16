// Day rules for the day view: a port of shared/.../domain/model/DayRules.kt. Keep the tables in step with it.
// Weekdays are 0 = Sunday (as civil.js dayOfWeek). Directions: [Tamil, English].

export const DIRECTIONS = {
  EAST: ['கிழக்கு', 'East'],
  SOUTH: ['தெற்கு', 'South'],
  WEST: ['மேற்கு', 'West'],
  NORTH: ['வடக்கு', 'North'],
};

// Vaara Soolam by weekday, with its remedy (sources in docs/BACKLOG.md).
const SOOLAM = [
  ['WEST', 'நெய் (அ) வெற்றிலை', 'Ghee or betel leaf'],
  ['EAST', 'பால் (அ) தயிர்', 'Milk or curd'],
  ['NORTH', 'வெல்லம் (அ) பால்', 'Jaggery or milk'],
  ['NORTH', 'பால் (அ) கொத்தமல்லி / எள்', 'Milk, coriander or sesame'],
  ['SOUTH', 'தயிர் (அ) சீரகம்', 'Curd or cumin'],
  ['WEST', 'பார்லி (அ) நெய் / வெல்லம்', 'Barley, ghee or jaggery'],
  ['EAST', 'உளுந்து (அ) எள் / இஞ்சி', 'Urad dal, sesame or ginger'],
];

const WEEKDAY_REMEDY = [
  'Offer water to the Sun; donate wheat or jaggery',
  'Offer raw rice or milk; wear white',
  'Light a ghee lamp for Murugan or Durga; jaggery or red lentils',
  'Donate green gram; feed cows and birds',
  'Offer yellow flowers or chickpeas; sandalwood or turmeric tilak',
  'Light a ghee lamp at home; offer white flowers, curd or sugar',
  'Light a sesame oil lamp; donate black sesame or urad dal',
];

export const soolam = (weekday) => {
  const [dir, remedyTa, remedyEn] = SOOLAM[weekday];
  return { direction: DIRECTIONS[dir], remedyTa, remedyEn };
};

export const weekdayRemedy = (weekday) => WEEKDAY_REMEDY[weekday];

/** Abhijit Muhurtham: solar noon ± daytime/30. Sunrise and sunset are seconds since midnight. */
export const abhijit = (sunriseSec, sunsetSec) => {
  const daytime = sunsetSec - sunriseSec;
  const noon = sunriseSec + Math.floor(daytime / 2);
  const half = Math.floor(daytime / 30);
  return [noon - half, noon + half];
};

const MOVABLE = [['பவம்', 'Bava'], ['பாலவம்', 'Balava'], ['கௌலவம்', 'Kaulava'], ['தைதுலை', 'Taitila'], ['கரசை', 'Gara'], ['வணிசை', 'Vanija'], ['பத்திரை (விஷ்டி)', 'Vishti']];
const FIXED = [['சகுனி', 'Shakuni'], ['சதுஷ்பாதம்', 'Chatushpada'], ['நாகவம்', 'Naga']];

/** Karanam (half a tithi), index 0..59. Index 0 is Kimstughna; 1..56 repeat the seven movable karanams. */
export const karanaName = (index) => {
  if (index === 0) return ['கிம்ஸ்துக்னம்', 'Kimstughna'];
  if (index <= 56) return MOVABLE[(index - 1) % 7];
  return FIXED[index - 57];
};

/** Vishti (Bhadra) karanam: avoided for auspicious starts. */
export const isVishti = (index) => index >= 1 && index <= 56 && (index - 1) % 7 === 6;

// Kari Naal: solar days in each Tamil month (0 = Chithirai) when no Subakariyam is held.
const KARI_NAAL = [
  [6, 15], [7, 16, 17], [1, 6], [2, 10, 20], [2, 9, 28], [16, 29],
  [6, 20], [1, 10, 17], [6, 9, 11], [1, 2, 3, 11, 17], [15, 16, 17], [6, 15, 19],
];

export const isKariNaal = (monthIndex, day) => KARI_NAAL[monthIndex].includes(day);

// Thara Balam (validator section F): count inclusively from the birth star to the day's star; take mod 9 (0 -> 9).
export const tharaOf = (dayStar, janmaStar) => {
  const count = (((dayStar - janmaStar) % 27) + 27) % 27 + 1;
  const t = count % 9;
  return t === 0 ? 9 : t;
};
/** Passing Tharas: Sampath 2, Kshema 4, Sadhaka 6, Mitra 8, Parama Mitra 9. */
export const tharaPasses = (t) => [2, 4, 6, 8, 9].includes(t);

/** Offset from the Moon's Rasi to the Chandrashtamam Rasi, as used by the engine. The validator's inclusive count is 7;
 *  this is an open question (see docs/BACKLOG.md). */
export const CHANDRASHTAMA_OFFSET = 5;
/** True when the Moon's Rasi is the Chandrashtamam Rasi of the person's birth Rasi. */
export const personalChandrashtamam = (moonRasi, janmaRasi) => moonRasi === (janmaRasi + CHANDRASHTAMA_OFFSET) % 12;

// Hora (validator Step 3B.6): 24 horas, 12 of daytime from sunrise and 12 of night from sunset. The first hora of a
// weekday belongs to that weekday's lord, then Sun, Venus, Mercury, Moon, Saturn, Jupiter, Mars repeats.
// Times are seconds since midnight; the night wraps past midnight.
export const PLANETS = {
  SUN: ['சூரியன்', 'Sun', false], VENUS: ['சுக்கிரன்', 'Venus', true], MERCURY: ['புதன்', 'Mercury', true],
  MOON: ['சந்திரன்', 'Moon', true], SATURN: ['சனி', 'Saturn', false], JUPITER: ['குரு', 'Jupiter', true], MARS: ['செவ்வாய்', 'Mars', false],
};
const CHALDEAN = ['SUN', 'VENUS', 'MERCURY', 'MOON', 'SATURN', 'JUPITER', 'MARS'];
const WEEKDAY_LORD = ['SUN', 'MOON', 'MARS', 'MERCURY', 'JUPITER', 'VENUS', 'SATURN']; // index 0 = Sunday

export const hora = (weekday, sunriseSec, sunsetSec, nextSunriseSec) => {
  const first = CHALDEAN.indexOf(WEEKDAY_LORD[weekday]);
  const nextRise = nextSunriseSec <= sunsetSec ? nextSunriseSec + 86400 : nextSunriseSec;
  const dayPart = (sunsetSec - sunriseSec) / 12;
  const nightPart = (nextRise - sunsetSec) / 12;
  return Array.from({ length: 24 }, (_, k) => {
    const daytime = k < 12;
    const from = daytime ? sunriseSec + k * dayPart : sunsetSec + (k - 12) * nightPart;
    const to = daytime ? sunriseSec + (k + 1) * dayPart : sunsetSec + (k - 11) * nightPart;
    const lord = CHALDEAN[(first + k) % 7];
    return { lord: PLANETS[lord], benefic: PLANETS[lord][2], start: Math.round(from) % 86400, end: Math.round(to) % 86400, daytime };
  });
};
