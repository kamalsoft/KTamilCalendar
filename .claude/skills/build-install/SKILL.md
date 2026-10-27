---
name: build-install
description: Build the KTamilCalendar APK, run unit tests, and install it on the emulator or the user's TV. Use for any build, test, or install request in this repo.
---

# Build & install

**JDK:** Gradle 8.9 needs JDK 17–22. Android Studio's JBR is JDK 25 → fails. Use:
`export JAVA_HOME=/Users/kamalsoft/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home`

**Build / test** (quiet output, errors only):
```bash
./gradlew testDebugUnitTest assembleDebug --console=plain -q 2>&1 | grep -E "^e:|FAILED|What went wrong"
grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' app/build/test-results/testDebugUnitTest/*.xml | awk -F'"' '{t+=$2; f+=$6} END {print "tests:", t, "failures:", f}'
```
`compileDebugKotlin` is the fastest check while editing. A `--no-build` install uses the *old* APK.

**Install:** `tools/install_tv.sh --serial emulator-5554 --debug` (emulator) or `tools/install_tv.sh <tv-ip>`.
- The user's TV (`10.0.0.174:5555`) is often connected to adb → **always pass `-s emulator-5554`/`ANDROID_SERIAL`**; never install to the TV unless asked.
- Debug↔release signature switch needs `--reinstall` (wipes app data).

**Emulators** (Apple Silicon: API 30 TV images are x86-only, use API 31):
- TV: `emulator -avd TV_API31 -no-snapshot-save -no-audio -no-boot-anim` (run in background)
- Phone: `emulator -avd Medium_Phone_API_37.0 -read-only -no-snapshot` (user's AVD — keep read-only)
- Wait for boot: poll `adb -s emulator-5554 shell getprop sys.boot_completed` == 1.
