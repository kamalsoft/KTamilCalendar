# Store listing (draft)

Text for the Google Play listing for Android TV and phones. The app is named **Panchangam** for now (see
`docs/REBRAND_PLAN.md`). Change the title and the first lines when the final name is chosen.

## Title (30 characters max)

Panchangam · பஞ்சாங்கம்

## Short description (80 characters max)

Tamil panchangam for your place: tithi, Rahu Kalam, Rasi Palan and Muhurtham.

## Full description (4000 characters max)

Panchangam is a Tamil calendar and panchangam for Android TV, Google TV and phones. It works offline, and it works
for any place in the world.

Every day shows:

- The Tamil date, the Tamil month and the Tamil year.
- Tithi, nakshatram, yogam and karanam, with the time each one ends.
- Rahu Kalam, Yamagandam, Kuligai, and the good windows (Nalla Neram and Gowri).
- Abhijit Muhurtham, Soolam and the weekday remedy.
- Sraardham (Aparahna Kala) and the day for it.
- Planetary hours (Hora), sunrise and sunset, and the Lagnam (rising sign) with its times.
- Festivals, Ekadasi, Amavasai, Pournami, and the holidays for your region.

Other pages:

- **Month:** the whole month at a glance, with the moon's shape on each day and the Ayanam (Uttarayanam or Dakshinayanam).
- **Festivals and Important dates:** the year's festivals, observances and holidays, for several faiths.
- **Muhurtham:** good days for weddings and other ceremonies, based on the rules in the About page.
- **Rasi:** the Moon's Rasi, the planets' positions and Rasi Palan.
- **Moon and Sun:** the eight moon shapes, the two ayanams, the year's changes and the times they begin.
- **Ambient:** a full-screen clock with the tithi, the next good window and the weather while the TV is idle.

Settings let you choose:

- Your place. The app can find it when you allow location, or you can choose one from a list.
- Your birth star and Rasi, for personal rules such as Thara Balam and Chandrashtamam.
- The themes and the sounds (a devotional ambience, with optional voice).
- Reminders before Rahu Kalam and when each good window starts.

Accuracy: the astronomy is calculated from the Sun and Moon, and it is checked against the Swiss Ephemeris. Printed
calendars in your area may differ by a few minutes or by a day. The traditional rules (Muhurtham, Lagnam, Thara,
Chandrashtamam and Sraardham) are calculated, not issued by a religious authority. Before a marriage, a ceremony or a
Sraardham, confirm the date and time with a qualified temple priest. The About page explains each formula with a worked
example.

Privacy: there is no account, no ads and no analytics. Your settings and places stay on the device. The app contacts
Open-Meteo for the weather and place search. On Android TV it uses ipapi.co for an approximate location when location is
not allowed. The web app uses api-bdc.net for the town name when you choose to use your location. Full policy:
https://kamalsoft.github.io/KTamilCalendar/privacy.html (address to be confirmed after the Pages deploy).

## What's new (version 1.0.0)

- First release for Android TV, Google TV and phones.
- Panchangam calculated on the device, with the Swiss Ephemeris checks.
- Moon and Sun page, About page with formulas and worked examples.

## Keywords (Play Console)

tamil calendar, panchangam, panchang, tithi, rahu kalam, rasi palan, muhurtham, tamil festivals, hindu calendar,
nakshatram

## Category and contact

- Category: Lifestyle (or Reference, if the store offers it for the TV form factor).
- Contact details: to be added by the developer.
- Privacy policy URL: https://kamalsoft.github.io/KTamilCalendar/privacy.html (drafted in `web/src/main/resources/web/privacy.html`).

## Data safety answers (for the Play Console form)

- Data collected: approximate location. On TV it is read on the device, and an IP-based lookup through ipapi.co is used
  when location is not allowed. On the web it is sent to api-bdc.net when you choose your location. Weather requests
  send the place's coordinates to Open-Meteo.
- Data shared with third parties: only the location and search requests above, to those services.
- Data stored on the device only: settings, places and birth details.
- Data encrypted in transit: yes. `network_security_config.xml` blocks cleartext traffic, so every call is HTTPS.
- Users can request deletion: uninstalling the app deletes its data.

Check these answers against the app before submitting; they describe the current behaviour.
