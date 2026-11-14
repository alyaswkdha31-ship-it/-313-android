#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

PROJECT_DIR="$HOME/313-android"
ZIP_PATH="/sdcard/Download/313-android-source.zip"

if [ ! -d "$PROJECT_DIR" ]; then
  mkdir -p "$HOME"
  if [ -f "$ZIP_PATH" ]; then
    unzip -oq "$ZIP_PATH" -d "$HOME"
  else
    echo "ERROR: Copy 313-android-source.zip to /sdcard/Download first."
    exit 1
  fi
fi

if [ -x "$HOME/gradle-8.7/bin/gradle" ]; then
  GRADLE="$HOME/gradle-8.7/bin/gradle"
elif command -v gradle >/dev/null 2>&1; then
  GRADLE="$(command -v gradle)"
else
  echo "ERROR: Gradle 8.7 was not found in Termux."
  exit 1
fi

if [ -f "$HOME/OnyxSmartTV/local.properties" ]; then
  SDK_LINE="$(grep '^sdk.dir=' "$HOME/OnyxSmartTV/local.properties" | head -n1 || true)"
  if [ -n "$SDK_LINE" ]; then
    printf '%s\n' "$SDK_LINE" > "$PROJECT_DIR/local.properties"
  fi
fi

if [ -z "${ANDROID_HOME:-}" ] && [ -f "$PROJECT_DIR/local.properties" ]; then
  SDK_PATH="$(sed -n 's/^sdk.dir=//p' "$PROJECT_DIR/local.properties" | head -n1)"
  [ -n "$SDK_PATH" ] && export ANDROID_HOME="$SDK_PATH"
fi

cd "$PROJECT_DIR"
echo "Building 313 APK..."
if command -v aapt2 >/dev/null 2>&1; then
  "$GRADLE" --no-daemon assembleDebug -Pandroid.aapt2FromMavenOverride="$(command -v aapt2)"
else
  "$GRADLE" --no-daemon assembleDebug
fi

APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -s "$APK" ]; then
  echo "ERROR: Build finished but APK was not found."
  exit 1
fi

mkdir -p /sdcard/Download
cp "$APK" /sdcard/Download/313.apk
echo "APK ready: /sdcard/Download/313.apk"
if command -v termux-open >/dev/null 2>&1; then
  termux-open /sdcard/Download/313.apk
  echo "Android package installer opened. Confirm Install on the device."
else
  echo "Open /sdcard/Download/313.apk and confirm Install."
fi
