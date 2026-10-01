#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
./gradlew :app:testDebugUnitTest :app:assembleDebug
printf '\nAPK: %s/app/build/outputs/apk/debug/app-debug.apk\n' "$PWD"
