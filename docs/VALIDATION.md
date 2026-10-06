# Current validation

Updated 2026-10-06. Production code checkpoint **056f629** is **VERIFIED locally**. Release classification: **BETA READY** for controlled testing, subject to the limits below.

| Executed check | Result |
|---|---|
| Clean JVM tests | PASSED: 123, zero failures/errors/skips |
| Complete API35 x86_64 Android suite | PASSED: 18, zero failures/errors/skips |
| lintDebug | PASSED: 0 errors, 43 warnings retained |
| assembleDebug / assembleRelease / bundleRelease | PASSED |
| Minified release runtime smoke | PASSED: Home, App List, Folders, Settings, explicit search, Romanian resources; empty crash buffer |
| Release APK signature / debuggability | Local development signature verifies; package is non-debuggable |
| AAB offline language packaging | Generated BundleConfig confirms language splitting disabled |

Final clean build ran in 8m55s; the separate full Android run took 2m22s. Exact commands:

```sh
./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:assembleDebugAndroidTest --max-workers=2 --console=plain
./gradlew :app:connectedDebugAndroidTest --max-workers=2 --console=plain
```

Local logs are `.artifacts/final-clean-build.log` and `.artifacts/final-android-validation.log`; generated XML counts were independently checked. These local artifacts are intentionally untracked. Durable evidence and hashes are in [PERFORMANCE](PERFORMANCE.md) and [ENGINEERING_VALIDATION](ENGINEERING_VALIDATION.md).

The Android suite covers persistence/recreation, typography/startup, Strict Block/Break/intention, actual notification-listener lifecycle, actual Firefox accessibility diagnostics, secure search actions, protected folder search/reorder, consecutive widget drags/cancellation, navigation cancellation, manual folder-app ordering, notification-editor persistence, fixed footer bounds and a search measurement workload.

Official Firefox 157 was installed for the final local suite: its integration test executed. Its normal address source was found UNSUPPORTED, not reliable blocking. CI omits this external fixture and reports the corresponding skip; that does not certify browser behavior. See [browser evidence](BROWSER_COMPATIBILITY.md). Production browser behavior was not changed to satisfy startup timing.

Workflow and issue-template YAML parse and current documentation links pass local checks. Remote GitHub Actions execution is NOT EXECUTED. Release smoke uses an existing development key on a copy of the unsigned release; production signing and publication are NOT EXECUTED.

Physical biometric sensors, haptics, OEM lifecycle, battery and 60/90/120Hz acceptance are **BLOCKED BY ENVIRONMENT** because no physical hardware is available. Emulator results are not physical performance certification. Existing lint warnings, partial localization and the documented DataStore cold-subscription limitation remain. Broad TalkBack, keyboard and multi-device acceptance is not exhaustive.

Earlier unavailable-toolchain statements are historical; see [delivered validation notes](history/VALIDATION-delivered.md).
