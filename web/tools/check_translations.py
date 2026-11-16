#!/usr/bin/env python3
"""Lists the Tamil labels in the web app that have no translation yet, per language.

The keys are the Tamil text of every lb('<tamil>', '<english>') call, plus the tab names and month names, which are
looked up the same way. A label missing from a language file shows English in that language.

Usage:  python3 web/tools/check_translations.py          # counts per language
        python3 web/tools/check_translations.py --list te  # the missing labels for one language
        python3 web/tools/check_translations.py --export   # writes i18n/labels.json for the translator page
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1] / "src/main/resources/web"
FILES = {"te": "i18n/te.json", "ml": "i18n/ml.json", "kn": "i18n/kn.json", "hi": "i18n/hi.json"}


def tamil_labels():
    """Every Tamil key the app looks up, with the English it is paired with."""
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
    return {ta: en for ta, en in found.items() if any(ord(c) > 127 for c in ta)}


def main():
    labels = tamil_labels()
    if len(sys.argv) == 2 and sys.argv[1] == "--export":
        # The label list the translator page reads: Tamil key -> English.
        (ROOT / "i18n/labels.json").write_text(json.dumps(labels, ensure_ascii=False, indent=1, sort_keys=True) + "\n", encoding="utf-8")
        print(f"wrote {len(labels)} labels to i18n/labels.json")
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
