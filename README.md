# KTamilCalendar — Tamil Calendar for Android TV / Google TV

A 10-foot, D-pad-only Tamil calendar and panchangam. It works fully offline: on first launch the app
computes a year of data on the device from an astronomical engine and stores it in Room.

| | |
|---|---|
| minSdk / targetSdk / compileSdk | 21 / 30 / 34 |
| UI | Jetpack Compose + `androidx.tv:tv-material` 1.0.0 |
| Architecture | MVVM + Clean (domain / data / ui), Coroutines + StateFlow, manual DI |
| Storage | Room 2.6.1 (filled by the on-device engine, plus synced data from the server) |
| Network | Retrofit 2.11 + OkHttp 4.12, TLS 1.3/1.2 only, Conscrypt for TLS 1.3 on API 21–28 |

## Project layout

```
app/src/main/java/com/kamalsoft/tamilcalendar/tv/
├── core/astro/          Astro.kt (Sun/Moon/ayanamsa/sunrise), TamilCalendarEngine.kt (rules)
├── domain/model/        TamilDayData, Panchangam, TimingWindow, … + Tamil name tables
├── domain/usecase/      GetDay / GetMonthGrid / GetUpcomingEvents / GetRasiPalan, RasiPalanCalculator
├── domain/repository/   CalendarRepository (interface)
├── data/local/          Room entities, DAO, database, entity↔domain mapper
├── data/remote/         Retrofit API, DTOs, TLS-hardened OkHttp
├── data/repository/     CalendarRepositoryImpl (seeding, offline-first, sync merge)
├── di/AppContainer.kt
└── ui/                  theme, components/TvFocusCard (focus primitive), daily, monthly, events, rasipalan, app
```

## Build

```bash
./gradlew :app:testDebugUnitTest   # engine regression tests against known festival dates
./gradlew assembleDebug            # app/build/outputs/apk/debug/app-debug.apk
```

Use JDK 17 or 21. Gradle 8.9 cannot run on JDK 25. Android Studio's bundled JBR works.

To turn on sync, set `API_BASE_URL` in `app/build.gradle.kts` to an `https://` backend that implements
`data/remote/TamilCalendarApi.kt`. When it is blank, the app makes no network calls.

---

## Running in an Android 11 (API 30) TV emulator

### ⚠️ Apple Silicon Macs
Google publishes **API 30 TV images for x86 only**: `system-images;android-30;google-tv;x86` and
`…;android-tv;x86`. These images **do not boot on Apple Silicon (M-series) Macs**. On those machines:

* For day-to-day work, use **API 31 Google TV arm64** (`system-images;android-31;google-tv;arm64-v8a`).
  It is the closest image that runs. The app's minSdk is 21 and it has no API-31-only code paths.
* To verify on real API 30, use an Intel/AMD host or a physical Android 11 TV device (see below).

### Step 1: Create the AVD (Android Studio)
1. **Tools → Device Manager → Create Virtual Device** (the **+** button).
2. Category **TV** → hardware profile **Android TV (1080p)** → *Next*.
3. On the system image screen, open the **x86 Images** tab and pick **R · API 30 · x86 · Google TV**.
   Use **Android TV** if you don't need Google TV home. Click *Download* if needed, then *Next*.
   (Apple Silicon: choose **S · API 31 · arm64-v8a · Google TV** instead.)
4. AVD name `TV_API30`, orientation Landscape → *Show Advanced Settings*:
   * Graphics: **Hardware** (if the screen renders black, switch to Software).
   * RAM: 2048 MB. Leave the rest as is.
5. *Finish*.

You can do the same from the command line (needs `cmdline-tools` from the SDK Manager):
```bash
sdkmanager "system-images;android-30;google-tv;x86" "emulator" "platform-tools"
avdmanager create avd -n TV_API30 -k "system-images;android-30;google-tv;x86" -d tv_1080p
emulator -avd TV_API30
```

### Step 2: D-pad / keyboard simulation
1. Open `~/.android/avd/TV_API30.avd/config.ini` and make sure these are set:
   ```ini
   hw.dPad=yes
   hw.keyboard=yes
   hw.mainKeys=yes
   ```
2. Restart the AVD. Your computer keyboard then maps to the remote:

   | Remote | Keyboard / tool |
   |---|---|
   | D-pad ▲▼◀▶ | Arrow keys |
   | OK / Select | Enter |
   | Back | Emulator toolbar ◁, or `adb shell input keyevent KEYCODE_BACK` |
   | Home | Emulator toolbar ○ |
   | Virtual remote | Toolbar **⋯ → Extended controls → Directional pad** |
   | CH ▲ / CH ▼ (next/prev day) | `adb shell input keyevent KEYCODE_CHANNEL_UP` / `KEYCODE_CHANNEL_DOWN` (PageUp/PageDown on a physical keyboard also work) |
   | Jump to section | keys **1–5** |

3. To script a focus walk (useful for regressions):
   ```bash
   for k in DPAD_RIGHT DPAD_DOWN DPAD_DOWN DPAD_CENTER BACK; do adb shell input keyevent KEYCODE_$k; sleep 0.4; done
   ```

### Step 3: Run and debug
1. Pick **TV_API30** in the toolbar device selector, then **Run ▶** (configuration `app`).
   Or: `./gradlew installDebug && adb shell am start -n com.kamalsoft.tamilcalendar.tv/.MainActivity`
2. The app appears in the TV launcher's **Apps** row through the `LEANBACK_LAUNCHER` intent filter.
   On first launch it shows "பஞ்சாங்கம் கணிக்கப்படுகிறது…" for about a second while it computes the year.
3. Useful tools:
   * **Logcat**: filter `package:com.kamalsoft.tamilcalendar.tv`. Sync failures log under tag `CalendarRepository`.
   * **App Inspection → Database Inspector**: browse `calendar_day`, `timing_window`, `calendar_event`.
   * **Layout Inspector**: shows Compose recompositions. Use it to check that focus animations don't
     recompose whole screens.
   * Force re-seeding: `adb shell pm clear com.kamalsoft.tamilcalendar.tv`.

### Physical Android TV / Google TV (recommended for API 30)
Settings → System → About → tap **Android TV OS build** 7× → Developer options → enable **USB debugging**
(and **Network debugging** if available). Then `adb connect <tv-ip>:5555` and run as usual.

---

## Install on your TV (Google TV / Android TV)

You need a computer with the Android SDK platform-tools (`adb`) on the same Wi-Fi network as the TV.

### 1. Build the app
* **Quick (debug build):** `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
* **Recommended (signed release build, smaller and faster):**
  1. Create a signing key once. Keep the `.jks` file and passwords safe: future updates must be signed with the same key.
     ```bash
     keytool -genkeypair -v -keystore tamilcalendar.jks -alias tamilcalendar \
       -keyalg RSA -keysize 2048 -validity 10000
     ```
  2. Create `keystore.properties` in the repo root (it is git-ignored):
     ```properties
     storeFile=tamilcalendar.jks
     storePassword=<your store password>
     keyAlias=tamilcalendar
     keyPassword=<your key password>
     ```
  3. `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`

### 2. Turn on developer mode on the TV
1. **Settings → System → About** and scroll to **Android TV OS build**. Press **OK on it 7 times** until it says
   "You are now a developer". (On some TVs: Settings → Device Preferences → About → Build.)
2. Go back to **Settings → System → Developer options** and turn on **USB debugging**.
   * On Google TV with Android 14 or later, also turn on **Wireless debugging**.
3. Find the TV's IP address: **Settings → Network & Internet →** your Wi-Fi network (e.g. `192.168.1.50`).

### 3. Install (one command)
`tools/install_tv.sh` builds the app, connects to the TV, checks it, installs and opens the app.
Run it from the project folder (in zsh/bash a script in the current folder needs `./` in front, e.g. `./install_tv.sh` inside `tools/`):

* **Android 11–13 TVs (incl. Chromecast with Google TV):**
  ```bash
  tools/install_tv.sh 192.168.1.50
  ```
  When the TV asks **"Allow USB debugging?"**, choose **Always allow from this computer → OK** (the script waits up to 60 s).
* **Android 14+ Google TV (Wireless debugging):** on the TV open Developer options → Wireless debugging →
  **Pair device with pairing code**. It shows a pairing IP:port and a 6-digit code; the main Wireless debugging screen
  shows the **IP address & Port** used to connect:
  ```bash
  tools/install_tv.sh 192.168.1.50 --pair 192.168.1.50:37123 123456 --port 41234
  ```
  Pairing is only needed once per computer; later updates are just `tools/install_tv.sh 192.168.1.50 --port <port>`.
* **Options:** `--debug` / `--release` (default: release when `keystore.properties` exists, otherwise debug),
  `--no-build` (install the existing APK), `--reinstall` (uninstall first; needed when switching between debug and
  release builds, and clears the app's settings), `--serial <adb-serial>` (an already-connected device or emulator).
  `tools/install_tv.sh --help` lists everything.
* **If it fails** it says why: TV not reachable (wrong IP/port, different Wi-Fi, debugging off), the "Allow" prompt not
  accepted, not an Android TV, or a debug/release signature mismatch (use `--reinstall`).
* **Without a computer:** install **Send Files to TV** on both your phone and the TV (or **Downloader** on the TV),
  send the APK, open it on the TV and allow "Install unknown apps" for that app when asked.

<details><summary>Manual adb commands (what the script does)</summary>

```bash
adb pair 192.168.1.50:37123        # Android 14+ only; enter the 6-digit code
adb connect 192.168.1.50:5555      # Android 14+: use the port under "IP address & Port"
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.kamalsoft.tamilcalendar.tv/.MainActivity
```
</details>

### 4. Open it
The app appears in **Apps** on the Google TV home screen ("தமிழ் நாள்காட்டி"). To keep it on the home row, highlight it,
long-press **OK** and choose **Move** / **Add to favorites**. On first launch the remote help appears once. It's
always available again under the **About (ⓘ)** tab.

**Updating:** run the same `adb install -r …` with a newer APK. Settings and data are kept.
When you're done, you can turn **USB/Wireless debugging** off again. The app keeps working.

## Using it with the remote
Only the **D-pad, OK and BACK** are needed, which matches the standard Google TV remote (it has no number,
channel or menu keys):

| Remote | What it does |
|---|---|
| ▲ ▼ ◀ ▶ | Move between cards. ▲ from the top row goes to the tab bar, ◀ ▶ there switches sections |
| OK | Open details for the focused card, or select |
| ▼ from the bottom row of Today | Slide down to the **weather** page. ▲ slides back |
| ◀ / இன்று / ▶ buttons | Previous day / today / next day |
| BACK | Content → tab bar → Today → **press twice to exit** (a single stray press never closes the app) |
| CH ▲▼, 1–5, MENU | Optional shortcuts if your remote has them (next/previous day, jump to a section, help) |

The app follows the date at midnight and when the TV wakes from standby, so it can stay open as a wall calendar.

## Look & feel, navigation, location and themes
The UI follows **Apple TV (tvOS)** conventions:
* **Top tab bar:** இன்று Today · மாதம் Month · முக்கியம் Important Dates · பண்டிகை Festivals · முகூர்த்தம் Muhurtham ·
  ராசி Rasi, plus round icons for **Location** (📍), **Theme** (🎨) and **About** (ⓘ). Moving focus along the tabs
  switches the screen immediately. ▼ or OK enters the content, and ▲ from the top row returns to the tabs.
* **Focus:** the focused card lifts (scale and deep shadow) with a soft sheen. Temple Night and High Contrast add an
  accent ring for extra 10-foot visibility.
* **Themes:** Graphite (Apple TV greys, the default), Temple Night, Peacock, Ocean, Jasmine (light) and High
  Contrast. Each card previews the theme. OK applies it to every screen immediately, and it's remembered.
* **About:** version, data sources and accuracy, remote help, and the GitHub link with a **QR code** to scan
  from your phone.

## Weather
The **Today** screen shows the current temperature in its header. Press **▼** for a full weather page: current
conditions, the next 12 hours, and 7 days (°F, Naperville).
* Source: [Open-Meteo](https://open-meteo.com) (NOAA/NWS models for the US). No API key or account needed.
  It's **free for non-commercial use**. If you distribute the app commercially, take an Open-Meteo API plan or switch
  `data/remote/WeatherApi.kt` to the NWS API (api.weather.gov).
* Refreshes every 30 minutes while visible. The last forecast is cached, so the screen still shows weather
  (marked "சேமித்த தகவல்", saved data) when the internet is down. A weather failure never affects the calendar.

## Important dates & religious observances
The **month grid** marks each day's observances with icons, and shows the day's main festival or observance in the cell.
The **முக்கியம் · Important Dates** tab lists the selected month (◀ ▶ or CH▲▼ to change month).
* **Hindu monthly observances, by deity:**
  * Murugan: Sashti (Valarpirai, Sashti at sunset), Krithigai
  * Shiva: Pradosham, Masa Sivaratri
  * Perumal: Ekadasi, Thiruvonam, Purattasi Saturdays
  * Vinayagar: Sankatahara Chathurthi
  * Amman: Aadi Fridays, Pournami
  * Also Amavasai
* **Hindu festivals:** Skanda Sashti, Arudra Darshanam, Masi Magam, Aadi Pooram, Chitra Pournami, Hanuman Jayanthi,
  Navaratri, Avani Avittam, Onam, Akshaya Tritiya, Holi, Raksha Bandhan, plus the existing list.
* **Jain:** Mahavir Jayanti, Paryushan (Shvetambara), Samvatsari, Das Lakshana (Digambara), Mahavir Nirvana.
  **Sikh:** Vaisakhi, Guru Nanak Gurpurab. **Buddhist:** Vesak. All of these come from the same panchangam engine.
* **Christian:** Easter computus (Ash Wednesday → Pentecost, Orthodox Easter) plus fixed feasts (Epiphany, Annunciation,
  Assumption, All Saints/Souls, Advent, Christmas).
* **Jewish:** exact Hebrew-calendar arithmetic (Purim, Passover, Shavuot, Rosh Hashanah, Yom Kippur, Sukkot,
  Hanukkah). Holidays begin at sundown the evening before.
* **Islamic:** the month starts the day after the astronomical new moon if it occurs before sunset in Mecca
  (Umm al-Qura style). This matches the Fiqh Council of North America and Saudi dates for 2025–2026 (Ramadan
  18 Feb 2026, Eid al-Fitr 20 Mar 2026). Local moon-sighting communities may observe a day later, and the app says so.

Tested reference dates (`TamilCalendarEngineTest`, `WorldReligionsTest`):
* Islamic (2025): Ramadan 1 Mar, Eid al-Fitr 30 Mar, Eid al-Adha 6 Jun.
* Jewish: Rosh Hashanah, Yom Kippur, Passover and Hanukkah for 2025 and 2026.
* Christian: Easter 2025/2026 and Orthodox Easter 2026.
* Jain (2026): Mahavir Jayanti 31 Mar, Paryushan 8–15 Sep.
* Tamil: Soorasamharam 27 Oct 2025 (Chennai).

## Is the Panchangam real data?
Yes, in the same sense as Drik Panchang or a printed Tamil calendar. **All** panchangams are computed from Sun/Moon
positions; nothing is "observed". This app computes them on the device. It was checked against the
**Swiss Ephemeris** (the library most panchangam publishers use), for Naperville, over 365 days:

| | Result |
|---|---|
| Tithi / Nakshatram / Yogam at sunrise | **0 mismatches** in 365 days |
| Tamil month & date | **0 mismatches** |
| End times (tithi, nakshatram, yogam) | within **1.5 min** (typically < 1 min) |
| Sunrise / sunset | within **1.3 min** |

It was also spot-checked against Drik Panchang's Naperville page (26 Oct 2026): sunrise/sunset, tithi, nakshatram,
yogam end times and Rahu Kalam/Yamagandam/Kuligai all agree to within one minute. Times are rounded to the
nearest minute, as published panchangams do. Re-run the check any time with `tools/verify_panchangam.py`
(instructions at the top of that file).
What is **not** astronomy, and so can legitimately differ between calendars, is *convention*: which day a
festival is observed on, Nalla Neram tables, and Muhurtham rules. For temple-specific festival dates, follow your
local temple's calendar.

## Focus and remote behaviour
All focusable elements use `ui/components/TvFocusCard.kt`:
* Focused state: 1.05× scale, 2 dp gold ring, 8 dp elevation, and a drawn gold halo. Colored shadows only
  render on API 28+, so the halo keeps focus visible on API 21–27 boxes.
* OK/Enter fires on key-up (long-press fires on the first repeat). Arrow keys go to an optional
  `onDirectionalKey` handler first, then to Compose's spatial focus search.
* BACK: content → tab bar → Today → exit. Entering the tab bar always lands on the current tab.
* Month grid: ▶ on the Saturday column moves to the next month. Each day card lists that day's important dates
  (two lines in 5-week months, one plus a "+N" count in 6-week months).

## Calendar conventions (read before comparing with printed calendars)
* **Location**: Naperville, IL (41.75°N, 88.15°W, `America/Chicago`). All dates and times, including
  "today", sunrise, tithi end times and which civil day a festival lands on, are in Naperville local time, with
  CST/CDT handled. Change it in `di/AppContainer.kt` (`GeoLocation.CHENNAI` is also defined). Changing the
  location regenerates the offline database automatically.
* Because days are local, some festivals fall one day earlier in Naperville than in India (e.g. Deepavali 2026:
  7 Nov in Naperville, 8 Nov in Chennai). This is expected, and US temples observe these local dates.
* **Tamil month**: starts on the day of the sidereal (Lahiri) sankranti if it falls before sunset, otherwise the next day.
* **Tithi / Nakshatram / Yogam**: the one in force at sunrise, with its end time. Pradosham uses tithi at sunset.
  Deepavali and Ekadasi use sunrise. Vinayagar Chathurthi and Rama Navami use midday. Ayutha Pooja and
  Vijayadasami use the afternoon (aparahna). Krishna Jayanthi and Shivaratri use midnight.
  Skipped and repeated tithis are handled.
* **Rahu / Yama / Kuligai / Gowri**: each is 1/8 of the actual local sunrise-to-sunset span
  (`TimingConvention.SUNRISE_BASED`). Indian printed calendars use a fixed 06:00–18:00 day
  (`TRADITIONAL_6AM`), but that breaks with US latitudes and DST.
* **Nalla Neram** = good Gowri periods minus Rahu Kalam and Yamagandam. Some printed calendars use their
  own per-weekday Nalla Neram tables, so their times may differ.
* **Subha Muhurtham**: rule-based candidates (tithi, nakshatram, weekday, month, no adhika masa). The UI says to
  confirm with a priest.
* **Rasi Palan** (offline): Chandra Gochara, i.e. the Moon's house from each rasi. Predictions synced from the
  API replace it.
* **Holidays** (set up in `di/AppContainer.kt`):
  * **US federal + Illinois**: calculated from the legal rules (`core/holidays/UsHolidays.kt`) for any year, including
    weekend-observed days. Illinois adds Lincoln's Birthday, the day after Thanksgiving and general-election day
    (even years). These are the "local" holidays: they turn the day red.
  * **School: Naperville 203 and Indian Prairie 204**: transcribed from each district's official 2026–27 calendar
    into `app/src/main/assets/school_calendars.json` (source URLs and revision are inside the file). School boards adopt
    calendars yearly, so **add the 2027–28 blocks when they're published**. To show one district only, edit
    `schoolDistricts` in `AppContainer`. Multi-day breaks show as one row in the list and as a school badge on each day.
  * **Tamil Nadu**: still listed (Pongal, Deepavali, …) with a "தமிழ்நாடு" label. They are not days off here, so they
    don't turn the day red. When the same holiday appears in two lists (e.g. Christmas), it's shown once.
  * Islamic holidays depend on moon sighting and can't be computed. They can come from sync.

## Deliberate deviations from the brief
* **No `androidx.leanback`**: the UI is all Compose. Mixing Leanback fragments with Compose gives two focus
  engines that disagree at the boundaries. Leanback is in maintenance mode, and Google recommends Compose for TV.
* **No `androidx.tv:tv-foundation`**: its `TvLazy*` lists are deprecated. Standard Compose lazy layouts
  (1.7+) handle TV focus and scrolling themselves.
* **Pinned to the last minSdk-21 releases**: Compose 1.7.x, Room 2.6.1, Lifecycle 2.8.x, Core 1.13.x. Newer
  AndroidX releases need minSdk 23. See the note in `gradle/libs.versions.toml`.
* **targetSdk 30 blocks Play publishing**: Play requires a much newer targetSdk. Raise it before submitting
  and remove the `ExpiredTargetSdkVersion` lint suppression.
* The brief's example "சித்திரை 1, குரோதி வருடம் / Thiruvalluvar Aandu 2057" mixes two years.
  Krodhi ran from Apr 2024 to Apr 2025 (TVA 2055–2056). TVA 2057 belongs to **Parabhava** (பராபவ) from Apr 2026.
