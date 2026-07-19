#!/usr/bin/env bash
# 一鍵：設環境 → 裝 SDK 套件 → build debug APK → 裝到 MuMu 模擬器。
# 前置：brew install openjdk@17 gradle；brew install --cask android-commandlinetools
# 用法：cd android && ./build-and-install.sh [adb序號，預設 127.0.0.1:5555]
set -euo pipefail
cd "$(dirname "$0")"

DEVICE="${1:-127.0.0.1:5555}"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

echo "JAVA_HOME=$JAVA_HOME"
echo "ANDROID_HOME=$ANDROID_HOME"

# local.properties 指向 SDK（gradle 需要）
echo "sdk.dir=$ANDROID_HOME" > local.properties

# 裝所需 SDK 套件（首次會下載）
if [ -x "$SDKMANAGER" ]; then
  yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true
  "$SDKMANAGER" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
fi

# build（優先用 gradle wrapper，沒有就用系統 gradle）
if [ -x ./gradlew ]; then
  ./gradlew assembleDebug
else
  gradle assembleDebug
fi

APK="app/build/outputs/apk/debug/app-debug.apk"
echo "APK: $APK"
adb connect "$DEVICE" >/dev/null 2>&1 || true
adb -s "$DEVICE" install -r "$APK"
echo "✅ 已裝到 $DEVICE。到模擬器開 app、依序給三個權限，點浮動球。"
