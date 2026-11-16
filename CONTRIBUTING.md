# Contributing translations (web app)

The web app can show its labels in Telugu, Malayalam, Kannada and Hindi. The translations are in
`web/src/main/resources/web/i18n/<code>.json` (`te`, `ml`, `kn`, `hi`). Each file maps the Tamil text of a label to its
translation. A label without a translation shows English.

## How to translate

1. Open the translator page: `translate.html` on the app's site (linked from Settings → Language → "Help translate").
2. Choose your language. Each row shows the Tamil label, the English meaning and a box for your translation.
3. Fill in as many boxes as you can. Your work is saved in your browser, so you can come back to it.
4. Click **Download JSON**. You get a file such as `te.json`.
5. Send the file in one of two ways:
   - **Issue:** open an issue at https://github.com/kamalsoft/KTamilCalendar/issues, attach the file, and say which language.
   - **Pull request:** replace `web/src/main/resources/web/i18n/<code>.json` with your file and open a pull request.

## Guidelines

- Keep the panchangam terms (tithi, nakshatram, yogam, karanam, muhurtham, sraardham) in the form your community uses.
  If unsure, keep the Tamil or the English word in brackets.
- Keep numbers, times and the app's symbols unchanged.
- Short labels matter: buttons and tabs must fit on a small screen.

## Checking coverage

```
python3 web/tools/check_translations.py            # how many labels each language has
python3 web/tools/check_translations.py --list te  # the labels still missing for Telugu
python3 web/tools/check_translations.py --export   # refreshes i18n/labels.json for the translator page
```

Run `--export` after changing any label in the app, so the translator page lists the current labels.

## Reviewers

Each language needs a native speaker who reviews the file before release. Reviewers are credited on the About page.
