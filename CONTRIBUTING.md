# Contributing translations (web app)

The web app can show its labels in Telugu, Malayalam, Kannada and Hindi. The translations are in
`web/src/main/resources/web/i18n/<code>.json` (`te`, `ml`, `kn`, `hi`). Each file maps the Tamil text of a label to its
translation. A label without a translation shows English.

## How to translate

1. Open the translator page: `translate.html` on the app's site (linked from Settings → Language → "Help translate").
2. Download the template. It is one file, `panchangam-translations.json`, for every language.
3. Each entry has the Tamil label (`ta`), its English (`en`), and empty fields `te`, `ml`, `kn` and `hi`. Type the translation in the field for your language. Leave a field empty to keep English.
4. Upload the filled file on the same page. It is checked and shown with a count per language. The app uses it on this device.
5. To share it with everyone, send the file in one of two ways:
   - **Issue:** open an issue at https://github.com/kamalsoft/KTamilCalendar/issues and attach the file.
   - **Pull request:** split the file into `web/src/main/resources/web/i18n/<code>.json` (keys are the `ta` values, values are the translations) and open a pull request.

## Guidelines

- Keep the panchangam terms (tithi, nakshatram, yogam, karanam, muhurtham, sraardham) in the form your community uses.
  If unsure, keep the Tamil or the English word in brackets.
- Keep numbers, times and the app's symbols unchanged.
- Short labels matter: buttons and tabs must fit on a small screen.

## Checking coverage

```
python3 web/tools/check_translations.py            # how many labels each language has
python3 web/tools/check_translations.py --list te  # the labels still missing for Telugu
python3 web/tools/check_translations.py --export   # refreshes i18n/template.json, the one file for all languages
```

Run `--export` after changing any label in the app, so the template lists the current labels.

## Reviewers

Each language needs a native speaker who reviews the file before release. Reviewers are credited on the About page.
