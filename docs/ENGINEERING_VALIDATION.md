# Engineering validation record — 2026-10-04

Original source checkpoint: 6b676c5.

Baseline clean command reached Kotlin compilation using installed JDK 17 and SDK 35. Both variants failed on isProtectedShortcuts, HomeAlphabet selected shadowing, and missing NotificationMode import. No original APK was generated.

After the three minimal compilation fixes, all 83 existing JUnit tests executed with zero failures/errors. Lint and debug/release packaging are still running; do not treat this record as final validation.

Local Java NIO workaround: set JAVA_TOOL_OPTIONS to -Djdk.net.unixdomain.tmpdir pointing at the workspace, because the default socket directory failed with Invalid argument: connect. No source/build setting was changed for this host-specific issue.

adb devices -l returned no attached devices. No AVD or SDK system image is installed; emulator-check accel reports WHPX available. Device validation is not executed. SDK command-line manager is absent. Frame rates, cold-start time, installed size, battery and browser compatibility have not been measured.

Compile-fixed baseline debug APK: 14,164,142 bytes (13.508 MiB), assembleDebug PASSED. Artifact retained locally in ignored .artifacts/.

Repair batch: testDebugUnitTest, lintDebug and assembleDebug PASSED (BUILD SUCCESSFUL in 2m49s). 83 tests, zero failures/errors; lint zero errors and 43 warnings. No suppressions or lint baseline introduced. Final clean/release/device validation remains pending. Official command-line tools ZIP SHA256 verified against published 90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a.


Search/navigation batch: testDebugUnitTest PASSED 87/87; lintDebug PASSED zero errors; assembleDebug PASSED, BUILD SUCCESSFUL in 7m41s. First new accent regression failed and was corrected by search-only diacritic normalization. Added DataStore instrumentation test remains NOT EXECUTED. API35 emulator booted and baseline installed/rendered, but Android System UI ANR prevented reliable UI validation; retry with host graphics pending.

