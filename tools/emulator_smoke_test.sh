#!/usr/bin/env bash
set -euo pipefail

APK="${1:-app/build/outputs/apk/release/app-release.apk}"
PACKAGE="ir.agroyar.mobile"
MAIN="ir.agroyar.app.MainActivity"
SMOKE_DIR="build/smoke"
DEVICE_XML="/sdcard/window_dump.xml"
LOCAL_XML="$SMOKE_DIR/window.xml"
mkdir -p "$SMOKE_DIR"

test -s "$APK" || { echo "APK not found: $APK" >&2; exit 2; }

dump_ui() {
  adb shell rm -f "$DEVICE_XML" >/dev/null 2>&1 || true
  adb shell uiautomator dump >/tmp/uia-dump.txt 2>&1 || true
  cat /tmp/uia-dump.txt || true
  for _ in 1 2 3 4 5; do
    if adb shell test -f "$DEVICE_XML" >/dev/null 2>&1; then break; fi
    sleep 1
  done
  adb pull "$DEVICE_XML" "$LOCAL_XML" >/dev/null
}

ui_contains() {
  python3 - "$LOCAL_XML" "$1" <<'PY'
import sys
from pathlib import Path
xml = Path(sys.argv[1]).read_text(encoding="utf-8", errors="replace")
raise SystemExit(0 if sys.argv[2] in xml else 1)
PY
}

assert_ui_contains() {
  ui_contains "$1" || { echo "Missing UI text: $1" >&2; exit 7; }
}

tap_ui_match() {
  local coords
  coords="$(python3 - "$LOCAL_XML" "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot(); needle = sys.argv[2]
for node in root.iter("node"):
    text=node.attrib.get("text",""); desc=node.attrib.get("content-desc","")
    if needle in text or needle in desc:
        m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds",""))
        if m:
            x1,y1,x2,y2=map(int,m.groups()); print((x1+x2)//2,(y1+y2)//2); raise SystemExit(0)
raise SystemExit(1)
PY
)"
  read -r x y <<<"$coords"
  adb shell input tap "$x" "$y"
  sleep 1
  dump_ui
}

scroll_until_visible() {
  local needle="$1"
  for _ in 1 2 3 4 5 6; do
    ui_contains "$needle" && return 0
    adb shell input swipe 160 540 160 180 400
    sleep 1
    dump_ui
  done
  echo "UI item not visible after scrolling: $needle" >&2
  exit 8
}

capture() {
  adb shell screencap -p "/sdcard/$1.png" >/dev/null
  adb pull "/sdcard/$1.png" "$SMOKE_DIR/$1.png" >/dev/null
}

get_pid() {
  adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' || adb shell ps 2>/dev/null | grep "$PACKAGE" | awk '{print $2}' | head -1 | tr -d '\r' || true
}

launch_from_launcher() {
  adb shell am force-stop "$PACKAGE" || true
  adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/tmp/agroyar-monkey.txt 2>&1 || true
  cat /tmp/agroyar-monkey.txt
  sleep 4
  local pid
  pid="$(get_pid)"
  [[ -n "$pid" ]] || { echo "Process not running after launcher start" >&2; adb logcat -d | tail -250 >&2; exit 3; }
  local dump
  dump="$(adb shell dumpsys activity activities)"
  [[ "$dump" == *"$MAIN"* ]] || { echo "MainActivity not active after launcher start" >&2; echo "$dump" | tail -120 >&2; exit 4; }
}

echo "== Device =="
adb devices -l
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.product.cpu.abi

echo "== Install and replace =="
adb uninstall "$PACKAGE" >/dev/null 2>&1 || true
adb install "$APK"
adb install -r "$APK"
adb shell dumpsys package "$PACKAGE" | grep -E 'versionCode=|versionName=|minSdk=|targetSdk=' | head -10 || true

adb logcat -c

echo "== Cold launch 1 =="
launch_from_launcher
dump_ui
assert_ui_contains "اگرویار"
assert_ui_contains "بانک آفت‌کش"
assert_ui_contains "توصیه مصرف"
assert_ui_contains "15"
assert_ui_contains "25"
capture dashboard

echo "== Settings and developer =="
tap_ui_match "تنظیمات"
scroll_until_visible "درباره سازنده"
assert_ui_contains "فریبا عسگریان"
capture settings-developer

tap_ui_match "بازگشت"
assert_ui_contains "جست‌وجوی سم"
tap_ui_match "جست‌وجوی سم"
assert_ui_contains "نام سم، ماده مؤثره، EC، SC"
capture pesticide-search

echo "== Home/background/foreground =="
adb shell input keyevent KEYCODE_HOME
sleep 1
launch_from_launcher
dump_ui
assert_ui_contains "اگرویار"

echo "== Rotation recreation =="
adb shell settings put system accelerometer_rotation 0 || true
adb shell settings put system user_rotation 1 || true
sleep 2
dump_ui
assert_ui_contains "اگرویار"
adb shell settings put system user_rotation 0 || true
sleep 2

echo "== Cold launch 2 =="
launch_from_launcher
echo "== Cold launch 3 =="
launch_from_launcher

LOGS="$(adb logcat -d -v threadtime)"
CRASH_LINES="$(grep -E "FATAL EXCEPTION|ANR in ${PACKAGE//./\\.}|Process: ${PACKAGE//./\\.}|UnsatisfiedLinkError|VerifyError" <<<"$LOGS" || true)"
if [[ -n "$CRASH_LINES" ]]; then
  printf '%s\n' "$CRASH_LINES" >&2
  exit 6
fi

printf '%s\n' "$LOGS" > "$SMOKE_DIR/logcat.txt"
adb shell dumpsys activity activities > "$SMOKE_DIR/activity-dump.txt"
dump_ui
cp "$LOCAL_XML" "$SMOKE_DIR/final-window.xml"
echo "AgroYar compatibility smoke test passed."
