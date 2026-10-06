# Performance and artifact measurements

Status: VERIFIED, 2026-10-06. Before-M6 source checkpoint: `916ed31`. These are emulator and artifact measurements, not physical-device acceptance.

## Reproduction and environment

- JDK17, Gradle8.11.1, AGP8.9.2, SDK35; API35 x86_64 emulator, WHPX/host graphics, 320×640 at160 dpi, nominal60 Hz.
- Build with `clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:assembleDebugAndroidTest`. Then run `:app:connectedDebugAndroidTest`. Final compilation and emulator validation were separated to reduce host memory pressure.
- Run `scripts/Measure-Artifacts.ps1` from the repository root. It records byte sizes, SHA-256, compressed/uncompressed ZIP groups, largest entries and native libraries. AAB bytes are not device-specific download size.
- `SearchMeasurementTest` constructs 500 synthetic apps, warms five query types ten times, then measures 200 searches. This measures the search engine in debug instrumentation, not keyboard-to-screen latency.
- Startup uses `am force-stop` followed by `am start -W`; each process is cold but OS/file caches remain warm. Gradle was idle for startup samples. Memory uses `dumpsys meminfo`; installed storage uses Android's Storage screen.
- The Windows host reports 7,730,744 KiB physical RAM; only 521,876 KiB was free during one concurrent build/emulator check. JIT, host load and emulator graphics affect results. This is not a controlled performance laboratory.

## Search: measured optimization

| Measurement (ms) | Before | Prepared tokens |
|---|---:|---:|
| Median | 22.1075 | 2.6123 |
| p95 | 52.6212 | 13.7897 |
| Maximum | 380.5592 | 28.2069 |
| Index construction | 862.3387 | 521.8667 |

Both runs returned 31,200 matches. Query tokens are prepared once per search and name tokens once per catalog index, eliminating repeated regex splitting for every app/query. Ranking, cross-field matching, alias opt-out, package opt-in, hidden-app exclusion and stable ties retain executed unit coverage. Index construction includes class-loading/JIT variability; no precise causal benefit is claimed for that single sample. Logs: `m6-search-baseline.log`, `m6-search-optimized.log` under ignored `.artifacts/`.

## Startup, memory and installed storage

Five debug TotalTime samples (ms): before 6280/3371/3679/2883/3373; optimized 4949/3398/2958/3153/2994. Medians 3373/3153 ms. This small uncontrolled emulator comparison does not establish a causal startup improvement. Debug PSS snapshots 96,405/98,674 KiB and RSS 222,420/224,952 KiB show no measured memory reduction.

Final minified release process-cold samples: 1042/981/1190/998/1012 ms; median1012 ms. A separate initial smoke start was 780 ms. Post-start release PSS 34,948 KiB and RSS 180,932 KiB. Release/debug builds and host conditions differ; these are observations, not an optimization percentage or physical startup guarantee.

Android's release Storage screen reports app 6.75 MB, user data 270 kB, cache 561 kB, total 7.59 MB after the smoke flows. Values are rounded platform figures for this installation and include OS-dependent compilation/storage accounting. A shell `du` read could not inspect the oat directory, so its partial 2788 KiB result is not used as total installed size. Before-M6 installed total was not measured; no installed-size reduction is claimed. The earlier debug private-data snapshot was 312 KiB, excluding APK/ART/platform libraries.

A short idle Home `top` capture reported process CPU 0.0%, 0.0%, 0.5%, 0.0% (four samples, two-second interval); the raw output is retained in `final-release-idle-cpu.txt`; it is not battery, wakeup or long-duration idle proof. No frame-rate acceptance claim is made.

## Artifact sizes

| Artifact (bytes) | Before M6 | Final clean |
|---|---:|---:|
| Debug APK | 14,748,754 | 14,286,186 |
| Unsigned universal release APK | 3,038,438 | 2,789,120 |
| Release AAB | 5,581,009 | 5,534,410 |

Release APK reduction: 249,318 bytes/8.2%. AAB reduction: 46,599 bytes/0.83%. Before-M6 debug packaging was incremental; its difference from clean output is not attributed entirely to code optimization.

Baseline release compressed DEX contributed 1,766,646 bytes; resources 1,035,938 bytes. Resource shrinking reduced compressed resources to 789,141 bytes. All four ABIs and supported languages are retained. Native libraries total only 60,292 bytes: AndroidX graphics path and DataStore counters. Removing ABIs was unjustified. No heavyweight bundled image/font collection or unnecessary large dependency was identified. Debug DEX and developer tooling explain much of the debug/release difference.

The baseline AAB includes a 3,186,671-byte compressed R8 mapping file for retracing; it is not shipped as app code. AAB size must not be equated with Play download size.

| Final artifact | SHA-256 |
|---|---|
| Debug APK | `5E2BE75DF9AF91A77CF750C707B36164DC9D2F48769316EF7AF5A33EB5C8A5A5` |
| Unsigned release APK | `69B6BB5B1A2ABFE694F26B790103FD6606F52CA482E5E065456DF4987B23F813` |
| Release AAB | `41017FE441B11A1C040BCA6837500E97CF8979039FBA6BEFF4292C47F8DB79D9` |

## Packaging and runtime validation

R8 was already enabled before M6. Resource shrinking was added after measuring resource contribution. Clean lint identified dynamic language selection with default language splitting; bundle language splitting is now disabled so every supported language remains available offline, following [Android guidance](https://developer.android.com/guide/app-bundle/configure-base#handling_language_changes). No Play Core dependency was added. Final `BundleConfig.pb` was parsed with existing AGP bundletool/protobuf classes and confirms LANGUAGE negate=true. Play-distributed split installation itself was not executed.

Final clean build PASSED in 8m 55s: 123 JVM tests, zero failures/errors/skips, lint zero errors/43 warnings, debug/release/AAB/test APK. Full 18-test API35 suite PASSED in 2m 22s with zero failures/errors/skips. New footer-inset regression passed targeted and full runs. No production browser/focus workaround was added for the earlier role-dialog/cold-observer fixture failures.

A copy of the clean minified APK was signed locally with the existing development key solely for emulator installation; unsigned deliverables remain unchanged. Signature verification passes, and Android rejects `run-as` because the release is not debuggable. Home, corrected App List/Folders footers, Settings, actual focused `/weather` entry and Romanian resource switching render correctly. Screenshots are real release captures. A too-early manual search tap was corrected by observing the focused field before typing, with no production change. Crash buffer is empty after smoke; no launcher crash observed.

Evidence is retained under ignored `.artifacts/`: `final-clean-build.log`, `final-android-validation.log`, `final-artifact-sizes.json`, `final-bundle-config.txt`, `final-release-*.xml/txt/json`. No key or generated APK is committed.

## Acceptance limits

Physical battery, haptics, biometrics, OEM background behavior and 60/90/120 Hz acceptance are BLOCKED BY ENVIRONMENT: no physical device. Emulator samples do not certify smoothness or low battery consumption. Partial localization and the existing DataStore cold-subscription limitation remain documented. No further speculative size optimization was applied.
