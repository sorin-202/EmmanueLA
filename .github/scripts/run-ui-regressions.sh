#!/usr/bin/env bash
# Keep one shell: android-emulator-runner starts a separate process for each script line.
set -u

ui_test_status=0
./gradlew :app:connectedDebugAndroidTest --stacktrace --max-workers=2 || ui_test_status=$?
mkdir -p app/build/reports/androidTests
if ! adb pull /sdcard/Android/data/com.emmanuela.launcher/files/ux-screenshots app/build/reports/androidTests/; then
    if [ "$ui_test_status" -eq 0 ]; then ui_test_status=1; fi
fi
exit "$ui_test_status"
