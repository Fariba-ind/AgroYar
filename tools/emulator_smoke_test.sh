#!/usr/bin/env bash
set -euo pipefail

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
PACKAGE="ir.agroyar.app"
SPLASH="$PACKAGE/.SplashActivity"
MAIN="$PACKAGE/.MainActivity"
SMOKE_DIR="build/smoke"
DEVICE_XML="/sdcard/agroyar-window.xml"
LOCAL_XML="$SMOKE_DIR/window.xml"

mkdir -p "$SMOKE_DIR"

if [[ ! -f "$APK" ]]; then
  echo "APK not found: $APK" >&2
  exit 2
fi

dump_ui() {
  adb shell uiautomator dump "$DEVICE_XML" >/dev/null
  adb pull "$DEVICE_XML" "$LOCAL_XML" >/dev/null
}

assert_ui_contains() {
  local needle="$1"
  python3 - "$LOCAL_XML" "$needle" <<'PY'
import sys
from pathlib import Path
xml = Path(sys.argv[1]).read_text(encoding="utf-8", errors="replace")
needle = sys.argv[2]
if needle not in xml:
    raise SystemExit(f"UI does not contain expected text/content-description: {needle}")
PY
}

tap_ui_match() {
  local needle="$1"
  local coords
  coords="$(python3 - "$LOCAL_XML" "$needle" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, needle = sys.argv[1], sys.argv[2]
root = ET.parse(path).getroot()
for node in root.iter("node"):
    text = node.attrib.get("text", "")
    desc = node.attrib.get("content-desc", "")
    if needle == text or needle == desc or needle in text or needle in desc:
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            raise SystemExit(0)
raise SystemExit(f"Could not find tappable UI node matching: {needle}")
PY
)"
  read -r x y <<<"$coords"
  adb shell input tap "$x" "$y"
  sleep 1
  dump_ui
}

capture_screen() {
  local name="$1"
  adb shell screencap -p "/sdcard/$name.png" >/dev/null
  adb pull "/sdcard/$name.png" "$SMOKE_DIR/$name.png" >/dev/null
}

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

# Exercise rendered Compose UI, not just process/activity lifecycle.
echo "== Dashboard UI =="
dump_ui
assert_ui_contains "اگرویار"
assert_ui_contains "بانک آفت‌کش"
assert_ui_contains "توصیه مصرف"
assert_ui_contains "15"
assert_ui_contains "25"
capture_screen "dashboard"

echo "== About developer UI =="
tap_ui_match "تنظیمات"
assert_ui_contains "درباره سازنده"
assert_ui_contains "فریبا عسگریان"
assert_ui_contains "نسخه فعلی"
assert_ui_contains "0.2.0"
capture_screen "settings-developer"

tap_ui_match "بازگشت"
assert_ui_contains "جست‌وجوی سم"

echo "== Pesticide search UI =="
tap_ui_match "جست‌وجوی سم"
assert_ui_contains "نام سم، ماده مؤثره، EC، SC"
capture_screen "pesticide-search"

echo "== Runtime crash scan =="
LOGS="$(adb logcat -d -v threadtime)"
CRASH_LINES="$(grep -E 'FATAL EXCEPTION|ANR in ir\.agroyar\.app|Process: ir\.agroyar\.app' <<<"$LOGS" || true)"
if [[ -n "$CRASH_LINES" ]]; then
  printf '%s\n' "$CRASH_LINES" >&2
  exit 6
fi

printf '%s\n' "$START_OUTPUT" > "$SMOKE_DIR/start-output.txt"
printf '%s\n' "$ACTIVITY_DUMP" > "$SMOKE_DIR/activity-dump.txt"
printf '%s\n' "$LOGS" > "$SMOKE_DIR/logcat.txt"
cp "$LOCAL_XML" "$SMOKE_DIR/final-window.xml"

echo "AgroYar emulator smoke test passed."
