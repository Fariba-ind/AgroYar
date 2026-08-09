#!/usr/bin/env bash
set -euo pipefail

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
PACKAGE="ir.agroyar.app"
SPLASH="$PACKAGE/.SplashActivity"
MAIN="$PACKAGE/.MainActivity"

if [[ ! -f "$APK" ]]; then
  echo "APK not found: $APK" >&2
  exit 2
fi

echo "== Device =="
adb devices -l
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk

echo "== Install =="
adb uninstall "$PACKAGE" >/dev/null 2>&1 || true
adb install "$APK"

# Verify replacement/updating works with the same signing identity.
adb install -r "$APK"

echo "== Package metadata =="
adb shell dumpsys package "$PACKAGE" | grep -E 'versionCode=|versionName=|minSdk=|targetSdk=' | head -10 || true

echo "== Launch =="
adb logcat -c
START_OUTPUT="$(adb shell am start -W -n "$SPLASH")"
echo "$START_OUTPUT"
if [[ "$START_OUTPUT" != *"Status: ok"* ]]; then
  echo "SplashActivity did not launch successfully." >&2
  exit 3
fi

# Splash animation lasts about 2.7 seconds; allow MainActivity to settle.
sleep 5

PID="$(adb shell pidof "$PACKAGE" | tr -d '\r' || true)"
if [[ -z "$PID" ]]; then
  echo "AgroYar process is not running after launch." >&2
  adb logcat -d -v threadtime | tail -300 >&2
  exit 4
fi

echo "PID=$PID"

ACTIVITY_DUMP="$(adb shell dumpsys activity activities)"
printf '%s\n' "$ACTIVITY_DUMP" | grep -E "mResumedActivity|topResumedActivity|$PACKAGE" | head -40 || true
if [[ "$ACTIVITY_DUMP" != *"$MAIN"* ]]; then
  echo "MainActivity was not found after the splash transition." >&2
  adb logcat -d -v threadtime | tail -300 >&2
  exit 5
fi

echo "== Runtime crash scan =="
LOGS="$(adb logcat -d -v threadtime)"
CRASH_LINES="$(grep -E 'FATAL EXCEPTION|ANR in ir\.agroyar\.app|Process: ir\.agroyar\.app' <<<"$LOGS" || true)"
if [[ -n "$CRASH_LINES" ]]; then
  printf '%s\n' "$CRASH_LINES" >&2
  exit 6
fi

mkdir -p build/smoke
printf '%s\n' "$START_OUTPUT" > build/smoke/start-output.txt
printf '%s\n' "$ACTIVITY_DUMP" > build/smoke/activity-dump.txt
printf '%s\n' "$LOGS" > build/smoke/logcat.txt
adb shell uiautomator dump /sdcard/agroyar-window.xml >/dev/null 2>&1 || true
adb pull /sdcard/agroyar-window.xml build/smoke/window.xml >/dev/null 2>&1 || true
adb shell screencap -p /sdcard/agroyar-screen.png >/dev/null 2>&1 || true
adb pull /sdcard/agroyar-screen.png build/smoke/screen.png >/dev/null 2>&1 || true

echo "AgroYar emulator smoke test passed."
