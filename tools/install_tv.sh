#!/usr/bin/env bash
#
# Build and install Tamil Calendar on a Google TV / Android TV over Wi-Fi.
#
# Usage:
#   tools/install_tv.sh <TV_IP>                       # Android 11–13 TVs (incl. Chromecast with Google TV)
#   tools/install_tv.sh <TV_IP> --pair <IP:PORT> <CODE> --port <PORT>
#                                                     # Android 14+ "Wireless debugging" (pair once, then connect)
#   tools/install_tv.sh --serial <adb-serial>         # an already-connected device or emulator (e.g. emulator-5554)
#
# Options:
#   --port <N>        adb port on the TV (default 5555; Android 14+ shows it under "IP address & Port")
#   --pair <IP:PORT> <CODE>
#                     pair first using the IP:port and 6-digit code from
#                     Developer options → Wireless debugging → Pair device with pairing code
#   --debug           install the debug build (default: release if keystore.properties exists, else debug)
#   --release         install the release build (requires keystore.properties, see README)
#   --no-build        install the APK that is already built
#   --reinstall       uninstall first (needed when switching between debug and release builds;
#                     this clears the app's saved location and settings)
#   -h, --help        show this help
#
# Before running: on the TV, enable Developer options (Settings → System → About → press OK on
# "Android TV OS build" 7 times) and turn on USB debugging (and Wireless debugging on Android 14+).

set -euo pipefail

PACKAGE="com.kamalsoft.tamilcalendar.tv"
ACTIVITY="$PACKAGE/.MainActivity"
MIN_SDK=21
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# ----------------------------------------------------------------------------- output helpers
bold() { printf '\033[1m%s\033[0m\n' "$*"; }
step() { printf '\n\033[1;33m==> %s\033[0m\n' "$*"; }
ok()   { printf '\033[32m✔ %s\033[0m\n' "$*"; }
warn() { printf '\033[33m! %s\033[0m\n' "$*"; }
die()  { printf '\033[31m✘ %s\033[0m\n' "$*" >&2; exit 1; }
usage() { sed -n '3,26p' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }

# Runs a command, killing it after N seconds (macOS has no `timeout`). Returns the command's status.
with_timeout() {
  local secs="$1"; shift
  "$@" & local pid=$!
  ( sleep "$secs"; kill "$pid" 2>/dev/null ) & local killer=$!
  local rc=0; wait "$pid" 2>/dev/null || rc=$?
  kill "$killer" 2>/dev/null; wait "$killer" 2>/dev/null || true
  return "$rc"
}

# True if host:port accepts a TCP connection within a few seconds (bash /dev/tcp, no extra tools needed).
port_open() { with_timeout 5 bash -c "exec 3<>/dev/tcp/$1/$2" 2>/dev/null; }

# ----------------------------------------------------------------------------- arguments
TV_IP="" PORT=5555 PAIR_ADDR="" PAIR_CODE="" SERIAL="" VARIANT="" BUILD=1 REINSTALL=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)   usage 0 ;;
    --port)      PORT="${2:?--port needs a number}"; shift 2 ;;
    --pair)      PAIR_ADDR="${2:?--pair needs IP:PORT}"; PAIR_CODE="${3:?--pair needs the 6-digit code}"; shift 3 ;;
    --serial)    SERIAL="${2:?--serial needs an adb serial}"; shift 2 ;;
    --debug)     VARIANT=debug; shift ;;
    --release)   VARIANT=release; shift ;;
    --no-build)  BUILD=0; shift ;;
    --reinstall) REINSTALL=1; shift ;;
    -*)          warn "Unknown option: $1"; usage 1 ;;
    *)           [[ -z "$TV_IP" ]] || die "Only one TV IP expected (got '$TV_IP' and '$1')"; TV_IP="$1"; shift ;;
  esac
done
[[ -n "$TV_IP" || -n "$SERIAL" ]] || { warn "Give the TV's IP address (Settings → Network & Internet → your Wi-Fi)."; usage 1; }
[[ "$PORT" =~ ^[0-9]+$ ]] || die "--port must be a number"
if [[ -n "$PAIR_CODE" && ! "$PAIR_CODE" =~ ^[0-9]{6}$ ]]; then die "The pairing code must be the 6 digits shown on the TV"; fi

if [[ -z "$VARIANT" ]]; then
  if [[ -f "$REPO_ROOT/keystore.properties" ]]; then VARIANT=release; else VARIANT=debug; fi
fi
if [[ "$VARIANT" == release && ! -f "$REPO_ROOT/keystore.properties" ]]; then
  die "Release builds need keystore.properties (see README → 'Install on your TV'). Use --debug to install a debug build."
fi
APK="$REPO_ROOT/app/build/outputs/apk/$VARIANT/app-$VARIANT.apk"

# ----------------------------------------------------------------------------- tools
step "Checking tools"
ADB="$(command -v adb || true)"
for candidate in "${ANDROID_HOME:-}/platform-tools/adb" "${ANDROID_SDK_ROOT:-}/platform-tools/adb" "$HOME/Library/Android/sdk/platform-tools/adb" "$HOME/Android/Sdk/platform-tools/adb"; do
  [[ -n "$ADB" ]] && break
  [[ -x "$candidate" ]] && ADB="$candidate"
done
[[ -n "$ADB" ]] || die "adb not found. Install Android SDK platform-tools or set ANDROID_HOME."
ok "adb: $ADB"

if [[ $BUILD -eq 1 ]]; then
  # Gradle 8.9 runs on JDK 17–22. Prefer an explicit JAVA_HOME, then a JDK 21/17 found on macOS.
  if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home -v 17 2>/dev/null || true)"
    [[ -n "$JAVA_HOME" ]] && export JAVA_HOME
  fi
  [[ -n "${JAVA_HOME:-}" ]] && ok "JDK: $JAVA_HOME" || warn "JAVA_HOME not set; Gradle will use the default java (needs JDK 17–22)"
fi

# ----------------------------------------------------------------------------- build
if [[ $BUILD -eq 1 ]]; then
  step "Building the $VARIANT APK"
  TASK="assemble$(tr '[:lower:]' '[:upper:]' <<< "${VARIANT:0:1}")${VARIANT:1}"
  (cd "$REPO_ROOT" && ./gradlew "$TASK" --console=plain -q) || die "Build failed (see the Gradle output above)"
fi
[[ -f "$APK" ]] || die "APK not found: $APK (run without --no-build)"
ok "APK: ${APK#"$REPO_ROOT"/} ($(du -h "$APK" | cut -f1 | tr -d ' '))"

# ----------------------------------------------------------------------------- connect
if [[ -z "$SERIAL" ]]; then
  if [[ -n "$PAIR_ADDR" ]]; then
    step "Pairing with $PAIR_ADDR (Android 14+ wireless debugging)"
    "$ADB" pair "$PAIR_ADDR" "$PAIR_CODE" | tee /dev/stderr | grep -qi "successfully paired" \
      || die "Pairing failed. Open 'Pair device with pairing code' again on the TV and use the new IP:port and code."
    ok "Paired"
    [[ "$PORT" == 5555 ]] && warn "Android 14+ uses a different connect port: pass --port <port shown under 'IP address & Port'"
  fi

  SERIAL="$TV_IP:$PORT"
  step "Connecting to $SERIAL"
  port_open "$TV_IP" "$PORT" || die "Nothing answers at $SERIAL. Check: the TV is on and on the same Wi-Fi, the IP address \
(Settings → Network & Internet), USB debugging is on, and on Android 14+ use --port with the port shown under 'IP address & Port'."
  with_timeout 15 "$ADB" connect "$SERIAL" >/dev/null 2>&1 || true
  printf 'If the TV shows "Allow USB debugging?", choose "Always allow from this computer" → OK (waiting up to 60 s).\n'
fi

state=""
deadline=$((SECONDS + 60))
while (( SECONDS < deadline )); do
  state="$(with_timeout 5 "$ADB" -s "$SERIAL" get-state 2>/dev/null || true)"
  [[ "$state" == device ]] && break
  if [[ -n "$TV_IP" && -z "$state" ]]; then with_timeout 10 "$ADB" connect "$SERIAL" >/dev/null 2>&1 || true; fi
  sleep 2
done
case "$state" in
  device) ok "Connected to $SERIAL" ;;
  unauthorized) die "The TV hasn't allowed this computer yet. Accept the prompt on the TV, then run this again." ;;
  *) die "Can't reach $SERIAL. Check: same Wi-Fi network, the TV's IP address, USB/Wireless debugging is on, and the port (Android 14+ shows it under 'IP address & Port')." ;;
esac

# ----------------------------------------------------------------------------- device checks
step "Checking the device"
sh_() { "$ADB" -s "$SERIAL" shell "$@" 2>/dev/null | tr -d '\r'; }
MODEL="$(sh_ getprop ro.product.model)"
RELEASE="$(sh_ getprop ro.build.version.release)"
SDK="$(sh_ getprop ro.build.version.sdk)"
ok "$MODEL · Android $RELEASE (API $SDK)"
[[ "${SDK:-0}" -ge $MIN_SDK ]] || die "This app needs Android 5.0 (API $MIN_SDK) or newer."
if sh_ pm list features | grep -q "android.software.leanback"; then
  ok "Android TV / Google TV device"
else
  warn "This doesn't look like an Android TV. The app is built for TV (it requires the 'leanback' feature) and may refuse to install."
fi

# ----------------------------------------------------------------------------- install
if [[ $REINSTALL -eq 1 ]]; then
  step "Removing the existing app (--reinstall)"
  "$ADB" -s "$SERIAL" uninstall "$PACKAGE" >/dev/null 2>&1 && ok "Uninstalled" || ok "Not installed yet"
fi

step "Installing"
if ! out="$("$ADB" -s "$SERIAL" install -r "$APK" 2>&1)"; then
  printf '%s\n' "$out"
  case "$out" in
    *INSTALL_FAILED_UPDATE_INCOMPATIBLE*|*signatures\ do\ not\ match*)
      die "The installed copy was signed with a different key (debug vs release). Re-run with --reinstall (this clears the app's settings)." ;;
    *INSTALL_FAILED_INSUFFICIENT_STORAGE*) die "Not enough storage on the TV. Free some space and try again." ;;
    *INSTALL_FAILED_MISSING_FEATURE*) die "This device isn't an Android TV, so the TV-only app can't be installed." ;;
    *) die "Install failed (see the message above)." ;;
  esac
fi
ok "Installed $(sh_ dumpsys package "$PACKAGE" | grep -m1 versionName | sed 's/^ *//')"

step "Launching"
sh_ am start -n "$ACTIVITY" >/dev/null && ok "Tamil Calendar is open on the TV"

printf '\n'
bold "Done. Find the app later under Apps on the Google TV home screen (தமிழ் நாள்காட்டி)."
printf 'Tip: to update later, run this script again; settings and the chosen location are kept.\n'
