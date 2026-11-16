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


TAMIL = re.compile(r"[\u0B80-\u0BFF]")
ASCII_WORD = re.compile(r"[A-Za-z]")
# Pairs written next to each other in the source: 'tamil', 'english' (festival rules, holidays, labels) and
# object fields such as titleTa: '...', titleEn: '...'.
STRING = re.compile(r"'((?:[^'\\\n]|\\.)*)'|\"((?:[^\"\\\n]|\\.)*)\"")
BETWEEN = re.compile(r"\s*,\s*$")
OBJECT_PAIR = re.compile(r"\b\w*Ta\s*:\s*'([^'\n]*)'\s*,\s*\w*En\s*:\s*'((?:[^'\\\n]|\\.)*)'")


def source_pairs():
    """Tamil text and its English, as written in the web source files (not the sloka verses).

    Two string literals separated only by a comma are a pair ('tamil', 'english'), whatever quotes they use. Object fields
    such as titleTa: '...', titleEn: '...' are read separately.
    """
    files = [ROOT / "app.js", *sorted((ROOT / "app").glob("*.js")), *sorted((ROOT / "engine").glob("*.js"))]
    found = {}
    for path in files:
        if path.name in ("content.js", "icons.js"):
            continue
        text = path.read_text(encoding="utf-8")
        tokens = [(m.start(), m.end(), m.group(1) if m.group(1) is not None else m.group(2)) for m in STRING.finditer(text)]
        for (_, end_a, ta), (start_b, _, en) in zip(tokens, tokens[1:]):
            if BETWEEN.match(text[end_a:start_b]) and TAMIL.search(ta) and ASCII_WORD.search(en) and not TAMIL.search(en) and "\\n" not in ta:
                found.setdefault(ta, en)
        for ta, en in OBJECT_PAIR.findall(text):
            if TAMIL.search(ta) and ASCII_WORD.search(en) and not TAMIL.search(en) and "\\n" not in ta:
                found.setdefault(ta, en)
    return found


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
    for ta, en in source_pairs().items():
        found.setdefault(ta, en)
    # Words built in code from pieces, so no literal pair exists in the source.
    for ta, en in [("பின்னிரவு", "Late night"), ("காலை", "Morning"), ("மதியம்", "Afternoon"), ("மாலை", "Evening"), ("இரவு", "Night"),
                   ("சுப முகூர்த்தம் (வளர்பிறை)", "Subha Muhurtham (Shukla)"), ("சுப முகூர்த்தம் (தேய்பிறை)", "Subha Muhurtham (Krishna)"),
                   ("கரி நாள்", "Kari Naal"), ("சந்திர ராசியிலிருந்து கோசாரம்", "Rule-based gochara from current planetary transits"),
                   ("தமிழ் · English", "Tamil · English")]:
        found.setdefault(ta, en)
    # Short labels defined as lists or maps in the source: weekdays (Su, Mo ...) and the grahas (Sun, Moon ...).
    app = (ROOT / "app.js").read_text(encoding="utf-8")
    short = re.search(r"const WEEKDAYS_TA_SHORT = \[(.*?)\];", app).group(1)
    for ta, en in zip(re.findall(r"'([^']+)'", short), ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]):
        found.setdefault(ta, en)
    grahas = (ROOT / "engine/rasi.js").read_text(encoding="utf-8")
    for key, ta in re.findall(r"(\w+): '([^']+)'", re.search(r"const GRAHA_LABELS = \{(.*?)\};", grahas).group(1)):
        found.setdefault(ta, key.capitalize())
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
