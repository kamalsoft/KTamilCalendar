#!/usr/bin/env python3
"""Lists the Tamil labels in the web app that have no translation yet, per language.

The keys are the Tamil text of every lb('<tamil>', '<english>') call, plus the tab names and month names, which are
looked up the same way. A label missing from a language file shows English in that language.

Usage:  python3 web/tools/check_translations.py          # counts per language
        python3 web/tools/check_translations.py --list te  # the missing labels for one language
        python3 web/tools/check_translations.py --export   # writes i18n/template.json: one file, one placeholder per language
"""
import subprocess
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1] / "src/main/resources/web"
FILES = {"te": "i18n/te.json", "ml": "i18n/ml.json", "kn": "i18n/kn.json", "hi": "i18n/hi.json"}
LANGUAGE_NAMES = {"te": "Telugu", "ml": "Malayalam", "kn": "Kannada", "hi": "Hindi"}


def content_pairs():
    """Tamil and English names in the generated content: tithis, nakshatrams, rasis, weekdays, months, themes and so on."""
    out = subprocess.run(["node", str(pathlib.Path(__file__).resolve().parent / "dump_content_pairs.mjs")], capture_output=True, text=True, check=True).stdout
    return dict(json.loads(out))


def tamil_labels():
    """Every Tamil key the app looks up, with the English it is paired with: labels in the code, then the content names."""
    sources = [ROOT / "app.js", *sorted((ROOT / "app").glob("*.js"))]
    found = {}
    for path in sources:
        text = path.read_text(encoding="utf-8")
        for ta, en in re.findall(r"lb\(\s*'([^']+)'\s*,\s*'((?:[^'\\]|\\.)*)'\s*\)", text):
            found.setdefault(ta, en)
        for _, ta, en in re.findall(r"\['(\w+)', '([^']+)', '([^']+)'\]", text):
            found.setdefault(ta, en)
        for ta in re.findall(r"const MONTHS_TA = \[(.*?)\];", text, re.S):
            for word in re.findall(r"'([^']+)'", ta):
                found.setdefault(word, word)
    for ta, en in content_pairs().items():
        found.setdefault(ta, en)
    return {ta: en for ta, en in found.items() if any(ord(c) > 127 for c in ta)}


def main():
    labels = tamil_labels()
    if len(sys.argv) == 2 and sys.argv[1] == "--export":
        # One file for every language: each entry has the Tamil key, the English, and an empty placeholder per language.
        # Existing translations are filled in, so the file can be sent to a translator as it stands.
        have = {code: json.loads((ROOT / rel).read_text(encoding="utf-8")) for code, rel in FILES.items()}
        entries = [{"ta": ta, "en": en, **{code: have[code].get(ta, "") for code in FILES}} for ta, en in labels.items()]
        template = {"format": "panchangam-translations-v1", "languages": LANGUAGE_NAMES, "entries": entries}
        (ROOT / "i18n/template.json").write_text(json.dumps(template, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
        print(f"wrote {len(entries)} entries to i18n/template.json")
        return
    if len(sys.argv) == 3 and sys.argv[1] == "--list":
        code = sys.argv[2]
        have = json.loads((ROOT / FILES[code]).read_text(encoding="utf-8"))
        for ta, en in sorted(labels.items()):
            if ta not in have:
                print(f"{ta}\t{en}")
        return
    print(f"Tamil labels in the app: {len(labels)}")
    for code, rel in FILES.items():
        have = json.loads((ROOT / rel).read_text(encoding="utf-8"))
        missing = [ta for ta in labels if ta not in have]
        extra = [ta for ta in have if ta not in labels]
        print(f"{code}: translated {len(labels) - len(missing)}, missing {len(missing)}"
              + (f", unused keys {len(extra)}" if extra else ""))


if __name__ == "__main__":
    main()
