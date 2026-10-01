@echo off
cd /d "%~dp0"
call gradlew.bat :app:testDebugUnitTest :app:assembleDebug
if errorlevel 1 (
  echo Build failed. Read the error above.
  pause
  exit /b 1
)
echo APK: %CD%\app\build\outputs\apk\debug\app-debug.apk
explorer app\build\outputs\apk\debug
pause
