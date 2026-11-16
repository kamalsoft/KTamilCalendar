# Rebrand plan

A plan for changing the app's name, logo, colours and wording on Android TV, the web app and the documents. It is
written to be done in phases, and each phase can be committed on its own.

## 1. What carries the brand today

Counts are from the repository at the time of writing.

| Where | What | Notes |
|---|---|---|
| `app/src/main/res/values/strings.xml` | App label, "Tamizh Panchangam" | Shown under the launcher icon |
| `app/src/main/java/.../ui/about/AboutScreen.kt` | "தமிழ் நாள்காட்டி" and "Tamil Calendar for Google TV" | Hero on the About screen |
| `app/src/main/res/drawable` (`gopuram`) | Launcher and header icon | One vector |
| `web/src/main/resources/web/index.html` | Page title, description, header text, iOS home-screen title | |
| `web/src/main/resources/web/manifest.webmanifest` | Name, short name, description | Used when the web app is installed |
| `web/src/main/resources/web/logo.svg`, `icon-192.png`, `icon-512.png`, `apple-touch-icon.png` | Web icons | |
| `web/src/main/resources/web/app.js` | Header, About hero, tile labels | |
| `README.md`, `CLAUDE.md`, `docs/*.md` | Project name in text | Docs only |
| `.claude/skills/*` | Skill descriptions | Internal |
| `app/build.gradle.kts`, `AndroidManifest.xml` | `namespace` and `applicationId` `com.kamalsoft.tamilcalendar.tv` | About 97 Kotlin and config files use the package name |
| `web/src/main/resources/web/sw.js`, `app/store.js`, `app.js` | Cache names `ktamilcalendar-v6`, `ktc-packs-v5`, settings key `ktc.settings.v2` | Changing them resets saved state |
| Room database `tamil_calendar.db` | Local cache | Rebuilt from the engine, so safe to rename |

## 2. Decisions

Decided on the rebrand:

1. **Name: பஞ்சாங்கம் · Panchangam** (temporary; to be changed later). Short name: `Panchangam`. "Nalla Neram" stays as
   the name of the good-time windows, not the app.
2. **Tagline:** "Tamil panchangam for your place."
3. **Logo:** keep the gopuram vector. A new logo is out of scope for this round.
4. **Default colour theme:** keep Graphite. Brand colour unchanged.
5. **Package name:** keep `com.kamalsoft.tamilcalendar.tv`, so existing installs update in place. Phase P4 is not
   planned.
6. **Web address:** no change. The GitHub Pages address and the repository name stay.

## 3. Layout

### 3.1 Android TV (960 × 540 dp canvas)

```
+--------------------------------------------------------------------------------+
| [logo]  NEW NAME  tagline              [tabs: Today Month ... Learn]   [icons] |
+--------------------------------------------------------------------------------+
|  hero card (date, Tamil year)  |  panchangam column  |  timings column         |
|                                |                     |                         |
+--------------------------------------------------------------------------------+
```

- Logo and name sit at the top left. The name uses the Tamil line with the English line beneath it.
- The launcher icon uses the same logo on the theme colour, with an adaptive icon background.
- About: a hero card with logo, name, tagline and version, as on the web.

### 3.2 Phone (compact, 412 dp)

```
+----------------------------+
| [logo] NEW NAME    [loc]   |
+----------------------------+
| hero                        |
| panchangam cards           |
+----------------------------+
| Today  Month  Festivals ...|  bottom nav
+----------------------------+
```

### 3.3 Web (desktop and phone)

```
+-------------------------------------------------------------------+
| [logo] NEW NAME / Tamil name             [place chip]            |
| tabs: Today Month Festivals Important Muhurtham Rasi Moon&Sun ... |
+-------------------------------------------------------------------+
```

- The browser title and the manifest use the new name. The home-screen icon uses `icon-192`, `icon-512` and
  `apple-touch-icon`.

## 4. Phases

| Phase | Work | Verification |
|---|---|---|
| P0 | Decide the items in section 2 | Done |
| P1 | Strings: `strings.xml`, About hero, web `index.html` title and description, `manifest.webmanifest`, README header, install script | Done. Build, unit tests, web render of the header and About |
| P2 | Icons: Android adaptive icon and launcher icon, `logo.svg`, `icon-192`, `icon-512`, `apple-touch-icon` | Install on the TV emulator; render the web header; check the iOS icon |
| P3 | Default colour theme and the brand colour in `Theme.kt` and the web `engine/content.js` (regenerate with `gen_web_data.py`) | Theme screen; parity check |
| P4 | Optional identifiers: package name, `applicationId`, cache names, storage key. Only if decision 5 says so | Full upgrade test: install over the old app and check settings and data |
| P5 | Docs and skills: `CLAUDE.md`, `README.md`, `docs/*.md`, `.claude/skills/*` | Read-through |
| P6 | Release: version bump, store listing text, screenshots (`docs/screens`) | Release skill |

P1 to P3 do not change any saved data and can ship on their own. P4 is the only phase that can lose data, and it
needs a migration step.

## 5. Risks

- **Changing `applicationId`** installs a new app next to the old one. Users lose settings and reminders. Avoid it unless
  there is a reason.
- **Changing the storage key** (`ktc.settings.v2`) resets the web settings and the birth details.
- **Changing cache names** clears the offline years. Users have to save them again.
- **Trademarks and store names.** Check that the new name is free on the Play Store, the App Store and in the
  trademark registers before the logo is final.
- **Screenshots and the demo video** (`docs/screens`, `docs/demo.mp4`) show the old name and must be redone.

## 6. Status

- **P0 and P1:** done. The launcher label, the TV About hero, the web title, header, home-screen title, manifest,
  About hero and README now use the new name and tagline. The package name, storage keys and cache names are unchanged.
- **P2 (icons):** not started. The gopuram stays for now.
- **P3 (theme):** not needed, since the default theme is unchanged.
- **P4:** not planned (see decision 5).
- **P5 (docs and skills):** done. The README demo caption, the install script messages and a brand line in
  `CLAUDE.md` now use the new name. The skills and `docs/CALCULATIONS.md` / `docs/BACKLOG.md` have no brand wording to
  change, and the repository and package names stay.
- **P6 (release):** not started. The screenshots in `docs/screens` and the demo in `docs/demo.gif` and `docs/demo.mp4`
  still show the old name and need retaking on the emulator. The store listing text also needs updating.

## 7. Done when

- The new name, tagline and logo appear on the TV launcher, the TV header, the About screen, the web header, the
  browser title, the home-screen icon and the README.
- The app builds, the unit tests pass, the parity check passes, and the TV emulator install works.
- A user who updates the app keeps their settings (unless P4 was chosen, in which case the migration is tested).
