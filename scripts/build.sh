#!/bin/bash
# 构建脚本（不需要 gradle wrapper）
# 用法: ./scripts/build.sh
# 需要: JDK 17、Android SDK(platforms;android-34 + build-tools;34.0.0)、Gradle 8.2
set -e
HERE="$(cd "$(dirname "$0")/.." && pwd)"

: "${JAVA_HOME:?请先 export JAVA_HOME 指向 JDK 17}"
: "${ANDROID_SDK_ROOT:?请先 export ANDROID_SDK_ROOT 指向 Android SDK}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
: "${GRADLE_BIN:?请先 export GRADLE_BIN 指向 gradle 可执行文件(8.2)}"

# AGP 8.x 用 user.home 定位 .android（不认 $HOME），这里统一指到仓库内目录
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$HERE/.build/android-user-home}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HERE/.build/gradle-home}"
mkdir -p "$ANDROID_USER_HOME" "$GRADLE_USER_HOME"

printf 'sdk.dir=%s\n' "$ANDROID_SDK_ROOT" > "$HERE/local.properties"

"$GRADLE_BIN" --no-daemon --console=plain -p "$HERE" assembleDebug

APK="$HERE/app/build/outputs/apk/debug/app-debug.apk"
OUT="$HERE/.build/out"
mkdir -p "$OUT"
cp -f "$APK" "$OUT/BiliSplashKiller-1.0-debug.apk"
echo "OK -> $OUT/BiliSplashKiller-1.0-debug.apk"
