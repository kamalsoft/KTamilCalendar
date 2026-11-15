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

/** Abhijit Muhurtham: 48 minutes centred on solar noon. Sunrise and sunset are seconds since midnight. */
export const abhijit = (sunriseSec, sunsetSec) => {
  const noon = sunriseSec + Math.floor((sunsetSec - sunriseSec) / 2);
  return [noon - 24 * 60, noon + 24 * 60];
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
