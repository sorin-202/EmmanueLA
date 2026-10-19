#!/usr/bin/env bash
# Keep one shell: android-emulator-runner starts a separate process for each script line.
set -u

ui_test_status=0
./gradlew :app:connectedDebugAndroidTest --stacktrace --max-workers=2 || ui_test_status=$?
mkdir -p app/build/reports/androidTests
ui_capture_root=app/build/outputs/connected_android_test_additional_output
ui_capture=""
if [ -d "$ui_capture_root" ]; then
    ui_capture=$(find "$ui_capture_root" -type f -path '*/ux-screenshots/*.png' -print -quit)
fi
if [ -z "$ui_capture" ] || ! cp -R "$ui_capture_root" app/build/reports/androidTests/ux-artifacts; then
    echo 'UTP screenshot output is missing or could not be archived.' >&2
    if [ "$ui_test_status" -eq 0 ]; then ui_test_status=1; fi
fi
exit "$ui_test_status"
