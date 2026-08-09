#!/usr/bin/env bash
set -euo pipefail

APK="${1:-app/build/outputs/apk/release/app-release.apk}"
PACKAGE="ir.agroyar.mobile"
MAIN="ir.agroyar.app.MainActivity"
SMOKE_DIR="build/smoke"
LOCAL_XML="$SMOKE_DIR/window.xml"
mkdir -p "$SMOKE_DIR"

test -s "$APK" || { echo "APK not found: $APK" >&2; exit 2; }

SDK="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"

dump_ui() {
  local output detected candidate
  output="$(adb shell uiautomator dump 2>&1 || true)"
  printf '%s\n' "$output"
  detected="$(printf '%s\n' "$output" | sed -n 's/.*dumped to: //p' | tr -d '\r' | tail -1)"
  for candidate in "$detected" /sdcard/window_dump.xml /storage/sdcard/window_dump.xml /storage/emulated/0/window_dump.xml; do
    [[ -n "$candidate" ]] || continue
    if adb pull "$candidate" "$LOCAL_XML" >/dev/null 2>&1; then
      return 0
    fi
  done
  return 1
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
  adb exec-out screencap -p > "$SMOKE_DIR/$1.png" || true
}

get_pid() {
  adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' || adb shell ps 2>/dev/null | grep "$PACKAGE" | awk '{print $2}' | head -1 | tr -d '\r' || true
}

assert_running() {
  local pid dump
  pid="$(get_pid)"
  [[ -n "$pid" ]] || { echo "Process not running" >&2; adb logcat -d | tail -250 >&2; exit 3; }
  dump="$(adb shell dumpsys activity activities)"
  [[ "$dump" == *"$MAIN"* ]] || { echo "MainActivity not active" >&2; echo "$dump" | tail -120 >&2; exit 4; }
}

launch_from_launcher() {
  adb shell am force-stop "$PACKAGE" || true
  adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/tmp/agroyar-monkey.txt 2>&1 || true
  cat /tmp/agroyar-monkey.txt
  sleep 4
  assert_running
}

scan_crashes() {
  local logs crash_lines
  logs="$(adb logcat -d -v threadtime)"
  crash_lines="$(grep -E "FATAL EXCEPTION|ANR in ${PACKAGE//./\\.}|Process: ${PACKAGE//./\\.}|UnsatisfiedLinkError|VerifyError" <<<"$logs" || true)"
  if [[ -n "$crash_lines" ]]; then
    printf '%s\n' "$crash_lines" >&2
    exit 6
  fi
  printf '%s\n' "$logs" > "$SMOKE_DIR/logcat.txt"
}

echo "== Device =="
adb devices -l
adb shell getprop ro.build.version.release
echo "$SDK"
adb shell getprop ro.product.cpu.abi

echo "== Install and replace =="
adb uninstall "$PACKAGE" >/dev/null 2>&1 || true
adb install "$APK"
adb install -r "$APK"
adb shell dumpsys package "$PACKAGE" | grep -E 'versionCode=|versionName=|minSdk=|targetSdk=' | head -10 || true

adb logcat -c

echo "== Cold launch 1 =="
launch_from_launcher
capture dashboard

if [[ "$SDK" -gt 21 ]]; then
  echo "== Full rendered UI interaction =="
  dump_ui
  assert_ui_contains "اگرویار"
  assert_ui_contains "بانک آفت‌کش"
  assert_ui_contains "توصیه مصرف"
  assert_ui_contains "15"
  assert_ui_contains "25"

  tap_ui_match "تنظیمات"
  scroll_until_visible "درباره سازنده"
  assert_ui_contains "فریبا عسگریان"
  capture settings-developer

  tap_ui_match "بازگشت"
  assert_ui_contains "جست‌وجوی سم"
  tap_ui_match "جست‌وجوی سم"
  assert_ui_contains "نام سم، ماده مؤثره، EC، SC"
  capture pesticide-search
else
  echo "== Android 5 lifecycle compatibility path =="
  # UIAutomator on API 21 writes its hierarchy into a legacy storage alias that
  # is unreliable to pull on current platform-tools. Runtime validation below
  # intentionally uses launcher/activity/process/logcat instead.
  assert_running
fi

scan_crashes

echo "== Background / foreground =="
adb shell input keyevent KEYCODE_HOME
sleep 1
launch_from_launcher
scan_crashes

echo "== Rotation recreation =="
adb shell settings put system accelerometer_rotation 0 || true
adb shell settings put system user_rotation 1 || true
sleep 2
assert_running
scan_crashes
adb shell settings put system user_rotation 0 || true
sleep 2
assert_running

echo "== Cold launch 2 =="
launch_from_launcher
scan_crashes

echo "== Cold launch 3 =="
launch_from_launcher
scan_crashes

adb shell dumpsys activity activities > "$SMOKE_DIR/activity-dump.txt"
if [[ "$SDK" -gt 21 ]] && dump_ui; then
  cp "$LOCAL_XML" "$SMOKE_DIR/final-window.xml"
fi
capture final-screen

echo "AgroYar compatibility smoke test passed on API $SDK."
