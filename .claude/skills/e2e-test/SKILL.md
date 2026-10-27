---
name: e2e-test
description: Drive the app on the emulator with D-pad key events or touch, take screenshots, and verify screens. Use to check UI changes, focus behaviour, layouts at different resolutions, or record the demo video.
---

# Emulator UI testing

**Write steps to a bash script file and run `bash script.sh </dev/null`.** Inline zsh breaks: `set -- $var` doesn't split, unmatched globs abort, and `adb` swallows heredoc stdin (add `</dev/null` to every adb call).

Helper (`source` it):
```bash
ADB=~/Library/Android/sdk/platform-tools/adb; export ANDROID_SERIAL=emulator-5554
SP=<scratchpad>/e2e
k(){ $ADB shell input keyevent KEYCODE_$1 </dev/null; sleep ${2:-1.4}; }      # DPAD_UP/DOWN/LEFT/RIGHT, DPAD_CENTER, BACK, 1–9 (tabs)
shot(){ $ADB exec-out screencap -p </dev/null > $SP/$1.png; sips -Z 800 $SP/$1.png >/dev/null; }
outside(){ grep -c EV_KEY $SP/getevent.log; }
```
- **The user sometimes presses keys in the emulator window.** Log real key events in the background: `adb -s emulator-5554 shell getevent -lt > $SP/getevent.log` and compare `outside` before/after each step; rerun contaminated steps.
- Look at screenshots (Read the PNG) before claiming a screen works; downscale to 800 px to save tokens.
- Touch (phone): `input tap x y`, `input swipe x1 y1 x2 y2 300` in device pixels.
- Resolutions: `adb shell wm size 3840x2160; adb shell wm density 640` … then `wm size reset; wm density reset`.
- Location: `adb emu geo fix <lon> <lat>` is unreliable on the TV image; use the Location tab instead.
- Settings can be forced via `run-as com.kamalsoft.tamilcalendar.tv sed -i … shared_prefs/app_settings.xml` (keys `s_*`).
- First launch shows the location permission prompt + disclaimer; `pm grant … ACCESS_COARSE_LOCATION` and `pm clear` help.

**Demo video:** `adb shell screenrecord --bit-rate 8000000 --time-limit 120 /sdcard/demo.mp4` in background, run the tour, `pkill -INT screenrecord`, pull. screenrecord only writes frames on change — static frames after a fade look blank (the breathing focus glow prevents this). Encode: `ffmpeg … -c:v libx264 -crf 26 docs/demo.mp4`; GIF via palettegen at 8 fps, 800 px.
