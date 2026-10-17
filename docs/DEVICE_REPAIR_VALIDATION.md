# Physical-device repair validation report

**Report state: host validation, general Android instrumentation and dedicated Firefox instrumentation passed at `02c3d60`; physical acceptance remains pending.** The corrected search-action case passed its exact persisted-state and recreation checks. Earlier failures and the limits of their diagnosis remain recorded below; passing automated checks alone does not establish physical acceptance.

Repository: `sorin-202/EmmanueLA`. Baseline: `b3de953`. **Validated implementation checkpoint:** `02c3d606e2659631c8c49f2c2c3580f3c29bf01e`. Production source is identical to `a52c86f`; subsequent commits change fixture setup, test observation/diagnostics and documentation. The required clean command passed locally and in the `02c3d60` CI checkpoint; general instrumentation recorded 45 cases: 44 passed, 0 failures/errors, 1 missing-browser skip. The separate official Firefox job executed that browser case and passed with no skips. This covers all 45 distinct Android test methods across the two jobs; it does not convert the general job's skip into a pass. The final documentation commit SHA is identified in [PR #1](https://github.com/sorin-202/EmmanueLA/pull/1) and repository history; this report does not embed its own commit SHA.

The source-verified roots, corrections, test descriptions, and baseline-relative changed-file list are in [DEVICE_REPAIR_IMPLEMENTATION.md](DEVICE_REPAIR_IMPLEMENTATION.md). That inventory and the commit list below must be refreshed if subsequent changes add paths or checkpoints.

## Acceptance status by requested section

These statuses describe the implementation and outstanding physical acceptance. They do not convert failed or skipped instrumentation into successful verification.

| # | Requested item | Status | Acceptance still needed |
|---|---|---|---|
| 1 | Direct widget dragging on normal Home | IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST | Held drag/release/cancel, smoothness, normal taps, no writes during movement, recreation |
| 2 | Independent widget styles and declarative import | IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST | Both style/import instrumentation routes passed at the `02c3d60` CI checkpoint. Real document providers, all applicable widgets, custom fonts and narrow screens remain physical acceptance work |
| 3 | App icons on the existing Home | IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST | Installed-app selection/launch, moving/removal/restart, hidden/private/blocked/protected-folder rules |
| 4 | `/query` keyboard search and five-item local history | IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST | Exact raw/displayed text, selection, Clear and search regression passed at the `02c3d60` CI checkpoint. Real IMEs/Enter, browser/search handlers, engine pages and history clearing/restart remain physical acceptance work |
| 5 | Compact authenticated folder search results | IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST | Compact presentation and actual password/device authentication |
| 6 | Per-folder RGB/color/image appearance | IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST | Provider access, restart, rotation during editing, revoked/deleted images and readable fallback |
| 7 | Custom folder dimensions | IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST | 3×3/4×4/5×4/5×5, small/large screens, scrolling and manual order preservation |
| 8 | Live the Moment app enforcement | IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST | Already-open apps, Recents/other-launcher/notification/deep-link entry, limits/schedules/Break, warnings/grace, permission loss/reboot |
| 9 | Live the Moment URL enforcement | IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST | Official Firefox 157.0.1 on API35 passed the real address, attached/visible blocking overlay and retained-diagnostic assertions at `02c3d60`, including the exact delayed first-run hint fixture path. Other browser versions/states and physical-device behavior remain outstanding |
| 10 | Notification digester | IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST | Real app notifications, capture/dismissal confirmation, summaries/review/retention, permission loss/rebind/reboot and battery restrictions |
| 11 | Local notification spam filtering | IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST | Real-app categories, promotional/duplicate/frequency behavior, false-positive tuning, reasons/overrides/allowlisting and essential notifications |

## Host verification

The required host command is:

```bash
./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease --stacktrace --max-workers=2
```

| Evidence | Recorded result | Scope |
|---|---|---|
| Original required full command | PASS; `BUILD SUCCESSFUL in 6m 58s` | Before the CI-driven corrections |
| Original JVM suite | 179 tests, 0 failures, 0 errors, 0 skipped | Before the CI-driven corrections |
| Original `lintDebug` | 0 errors, 50 warnings | Warnings remain reported; this is not a warning-free result |
| Original debug APK, release APK and release AAB tasks | PASS | Build artifacts, not runtime acceptance |
| Required full command after initial CI corrections | PASS; `BUILD SUCCESSFUL in 8m 58s` | Before the final retained-group acknowledgement and multiline/seconds-parity edge corrections |
| JVM/lint/builds in that 8m 58s run | 179 JVM tests, 0 failures/errors/skips; lint 0 errors/50 warnings; debug/release APK and release AAB PASS | Does not cover the final two edge corrections as a complete clean build |
| Incremental `testDebugUnitTest` + `compileDebugAndroidTestKotlin` before latest CI corrections | PASS; `BUILD SUCCESSFUL in 16s`; 179 JVM tests, 0 failures/errors/skips | Before the external-composition and style-navigation corrections |
| Incremental `testDebugUnitTest` + `compileDebugAndroidTestKotlin` through `dcb5a6d` | PASS; `BUILD SUCCESSFUL in 40s`; 179 JVM tests, 0 failures/errors/skips | Android execution is separate |
| Required full command at `26ddd2e` | PASS; `BUILD SUCCESSFUL in 6m 5s`; 108 actionable tasks | Includes the native Home navigation correction; precedes the browser-diagnostic guard and later test/CI corrections |
| JVM/lint/builds in that 6m 5s run | 179 JVM tests, 0 failures, 0 errors, 0 skipped; lint 0 errors, 50 warnings; debug/release APK and release AAB PASS | Complete host validation of `26ddd2e`, not final physical or instrumentation acceptance |
| Incremental JVM task and Android-test compilation before publishing `a52c86f` | PASS; `BUILD SUCCESSFUL in 2s` | Compilation/JVM task result; Android execution is separate |
| Required exact clean command at `a52c86f` | PASS; exit 0; `BUILD SUCCESSFUL in 4m 3s`; 108 actionable tasks: 105 executed, 3 up-to-date | Covers that implementation snapshot, including the browser guard and test/CI corrections |
| JVM/lint/builds at `a52c86f` | 179 JVM tests, 0 failures, 0 errors, 0 skipped; lint 0 errors, 0 fatal findings, 50 warnings; debug APK, unsigned release APK and release AAB PASS | Host verification completed; does not establish Android instrumentation or physical acceptance |
| Required exact clean command at `94e5368` | PASS; exit 0; `BUILD SUCCESSFUL in 3m 19s`; 108 tasks: 105 executed, 3 up-to-date | 179 JVM tests with zero failures/errors/skips; lint 0 errors/fatals, 50 warnings; debug/release APK and release AAB PASS |
| Required exact clean command at `392cd53` | PASS; exit 0; `BUILD SUCCESSFUL in 7m 10s`; 108 tasks: 105 executed, 3 up-to-date | 179 JVM tests, 0 failures/errors/skips; lint 0 errors/fatals, 50 warnings; debug APK, unsigned release APK and release AAB PASS |
| First local required clean attempt at `15ea8b2` | FAIL; `BUILD FAILED in 1m 44s`; `:app:lintAnalyzeDebug` | Compose lint raised internal `NoClassDefFoundError: androidx/compose/lint/Names$Ui$Layout` while analyzing unchanged `WallpaperEditor.kt`. No lint checks or dependencies were changed; a fresh-daemon rerun uses the identical required command |
| Required local exact clean command at `15ea8b2`, fresh daemon | PASS; exit 0; `BUILD SUCCESSFUL in 6m 50s`; 108 tasks: 105 executed, 3 up-to-date | 179 JVM tests, 0 failures/errors/skips; host lint 0 errors/fatals, 50 warnings; debug APK, unsigned release APK and release AAB PASS |
| Required CI exact clean command at `15ea8b2` | PASS; `BUILD SUCCESSFUL in 7m 27s`; 108 tasks: 107 executed, 1 up-to-date | CI unit report: 179 tests, 0 failures/ignored, 100% successful; CI lint report: 51 warnings, including `OldTargetApi`. Host and CI warning counts are recorded separately |
| Final local exact clean command at `02c3d60` | PASS; exit 0; `BUILD SUCCESSFUL in 6m 43s`; 108 tasks: 105 executed, 3 up-to-date | 179 JVM tests, 0 failures/errors/skips; host lint 0 errors/fatals, 50 warnings; debug APK, unsigned release APK and release AAB PASS |
| Final CI exact clean command at `02c3d60` | PASS; `BUILD SUCCESSFUL in 7m 13s`; 108 tasks: 107 executed, 1 up-to-date | CI unit report: 179 tests, 0 failures/ignored, 100% successful; CI lint report: 51 warnings. Host lint remains 0 errors/fatals and 50 warnings |

Logs: `/workspace/scratch/full-validation.log` (original), `/workspace/scratch/full-validation-corrections.log` (8m 58s full run), `/workspace/scratch/targeted-corrections-validation.log` (16s incremental run), `/workspace/scratch/final-regression-compile.log` (40s incremental run), `/workspace/scratch/final-clean-native-controls.log` (6m 5s full run at `26ddd2e`), and `/workspace/scratch/final-a52-clean-validation.log` (final 4m 3s full run at `a52c86f`). The preceding `392cd53` complete clean log and metrics are `/workspace/scratch/final-392-clean-validation.log` and `/workspace/scratch/final-392-metrics.json`. The preceding `15ea8b2` successful local log and metrics are `/workspace/scratch/final-15ea-clean-validation-fresh-daemon.log` and `/workspace/scratch/final-15ea-metrics.json`. The final `02c3d60` clean log is `/workspace/scratch/final-02c-clean-validation.log`; parsed JVM XML, lint and size metrics are `/workspace/scratch/final-02c-metrics.json`. Final CI host reports are archived in `/workspace/scratch/ci-02c-validation/`, artifact `12116408197` (preceding `15ea8b2` CI reports: `/workspace/scratch/ci-15ea-validation/`, artifact `12115149414`). The preceding `94e5368` log and metrics are `/workspace/scratch/final-94e-clean-validation.log` and `/workspace/scratch/final-94e-metrics.json` (earlier metrics are in `final-a52-metrics.json`). The first `15ea8b2` attempt is preserved in `/workspace/scratch/final-15ea-clean-validation.log`; `./gradlew --stop` stopped the daemon before the identical fresh-daemon rerun, whose log is `/workspace/scratch/final-15ea-clean-validation-fresh-daemon.log`. Android compilation initially caught a test API receiver mismatch in the new caret regression; the selection and IME actions were split into separate calls before the successful compilation.

Final artifact paths and byte sizes are recorded below. General Android and dedicated Firefox CI passed; physical-device acceptance remains outstanding.

## Android execution and CI

The required device command is:

```bash
./gradlew :app:connectedDebugAndroidTest --stacktrace --max-workers=2
```

| Run | Build job | Android result | Interpretation |
|---|---|---|---|
| [CI 38966973647](https://github.com/sorin-202/EmmanueLA/actions/runs/38966973647) | PASS | FAIL: 6 failures, 1 skipped browser case; application process crashed | Full suite did not complete; scheduled 42 tests |
| [CI 38967707957](https://github.com/sorin-202/EmmanueLA/actions/runs/38967707957) | PASS | FAIL: 6 failures, 1 skipped browser case; application process crashed | Full suite did not complete; scheduled 43 tests |
| [CI 38969896800](https://github.com/sorin-202/EmmanueLA/actions/runs/38969896800), `29d567b` | PASS | FAIL: 45 cases; 41 passed, 3 failed, 1 skipped; 0 errors | Entire suite completed without a process crash; failures were the caret/input regression and two style-navigation cases |
| [CI 39045032309](https://github.com/sorin-202/EmmanueLA/actions/runs/39045032309), `dcb5a6d` | PASS | FAIL: 45 cases; 41 passed, 3 failed, 1 skipped; 0 errors | Clear still failed; native style navigation exposed Home controls beneath Android navigation |
| [CI 39045644997](https://github.com/sorin-202/EmmanueLA/actions/runs/39045644997), `7368025` | PASS | General job FAIL; browser job FAIL during Firefox setup | Dedicated browser CI added; a failed setup did not execute or verify URL enforcement |
| [CI 39046117623](https://github.com/sorin-202/EmmanueLA/actions/runs/39046117623), `963f87e` | PASS | General suite FAIL: 45 cases; 42 passed, 2 failed, 1 skipped; 0 errors. Browser setup also FAIL | Native Settings navigation and full preview/Home parity PASSED; exact caret/input and import-dialog cases failed |
| [CI 39046135663](https://github.com/sorin-202/EmmanueLA/actions/runs/39046135663), `26ddd2e`, general suite | PASS | FAIL: 45 cases; 23 passed, 21 failed, 1 skipped; 0 errors | A Quickstep ANR dialog obscured native controls; these are recorded failures, not acceptance passes |
| Same `26ddd2e` run, official Firefox 157.0.1 / API35 | PASS | Firefox preflight PASS; instrumentation FAIL: 1 case, 0 passed, 1 failed, 0 skipped/errors | Actual URL discovery and observable overlay assertions completed; the final retained-diagnostic assertion failed |
| [CI 39047521783](https://github.com/sorin-202/EmmanueLA/actions/runs/39047521783), `a52c86f`, general suite | [PASS: build job](https://github.com/sorin-202/EmmanueLA/actions/runs/39047521783/job/117439144019) | [PASS: Android job](https://github.com/sorin-202/EmmanueLA/actions/runs/39047521783/job/117439143727): 45 cases; 44 passed, 0 failures, 0 errors, 1 skipped | Only skipped case was the supported-browser diagnostic because this general emulator has no browser fixture |
| Same `a52c86f` run, dedicated Firefox job | PASS | [FAIL before instrumentation](https://github.com/sorin-202/EmmanueLA/actions/runs/39047521783/job/117439143873): preflight reached its existing 180-second limit; 0 instrumentation tests executed | UI snapshots showed Android foreground windows and no Firefox controls. The service guard was not exercised in that run |
| [CI 39048376590](https://github.com/sorin-202/EmmanueLA/actions/runs/39048376590), `94e5368` | PASS | General PASS: 45 cases, 44 passed, 0 failures/errors, 1 missing-browser skip. Browser FAIL: 1 case failed at address-readability wait | Firefox preflight PASS with zero ANR recovery; actual instrumentation ran. The failure message captured status before waiting, so it did not establish the failure-time cause. Diagnostic capture was corrected in `392cd53`; the existing wait and predicate were retained |
| [CI 39049201457](https://github.com/sorin-202/EmmanueLA/actions/runs/39049201457), `392cd53` | [PASS: build job](https://github.com/sorin-202/EmmanueLA/actions/runs/39049201457/job/117444150901) | [PASS: general Android](https://github.com/sorin-202/EmmanueLA/actions/runs/39049201457/job/117444151052): 45 cases, 44 passed, 0 failures/errors, 1 missing-browser skip. [PASS: dedicated Firefox](https://github.com/sorin-202/EmmanueLA/actions/runs/39049201457/job/117444151071): 1 case, 1 passed, 0 failures/errors/skips | Official browser preflight and complete address/overlay/retained-diagnostic instrumentation passed; no physical-device acceptance is inferred |
| [CI 39072201499](https://github.com/sorin-202/EmmanueLA/actions/runs/39072201499), `1529d54` (documentation only) | [PASS: build](https://github.com/sorin-202/EmmanueLA/actions/runs/39072201499/job/117511949557), exact clean command in 5m 14s | [PASS: general Android](https://github.com/sorin-202/EmmanueLA/actions/runs/39072201499/job/117511949385): 45 cases, 44 passed, 0 failures/errors, 1 missing-browser skip. [FAIL: Firefox](https://github.com/sorin-202/EmmanueLA/actions/runs/39072201499/job/117511949639): 1 case, 0 passed, 1 failed, 0 errors/skips | Delayed native Firefox first-run hint hid its address accessibility tree after preflight; actual failure-time diagnostics identified the state |
| [CI 39072838524](https://github.com/sorin-202/EmmanueLA/actions/runs/39072838524), `15ea8b2` | [PASS: build](https://github.com/sorin-202/EmmanueLA/actions/runs/39072838524/job/117513827187), exact clean command in 7m 27s | [PASS: general Android](https://github.com/sorin-202/EmmanueLA/actions/runs/39072838524/job/117513827400): 45 cases, 44 passed, 0 failures/errors, 1 missing-browser skip. [PASS: Firefox](https://github.com/sorin-202/EmmanueLA/actions/runs/39072838524/job/117513827386): 1 case, 1 passed, 0 failures/errors/skips | New exact delayed-hint fixture guard executed once; all unchanged real address/overlay/visible-reason/Home-retention assertions passed |
| [CI 39073601199](https://github.com/sorin-202/EmmanueLA/actions/runs/39073601199), `3ca9e84` (documentation only) | [PASS: build](https://github.com/sorin-202/EmmanueLA/actions/runs/39073601199/job/117516090553), exact clean command in 7m 39s | [FAIL: general Android](https://github.com/sorin-202/EmmanueLA/actions/runs/39073601199/job/117516090547): 45 cases, 43 passed, 1 failed, 0 errors, 1 missing-browser skip. [PASS: Firefox](https://github.com/sorin-202/EmmanueLA/actions/runs/39073601199/job/117516090284): 1 case, 1 passed, 0 failures/errors/skips | `configuredSearchActionRequiresTapAndCanBeRemoved` timed out at one of its two 15-second configuration waits. Its captured stack lacked the exact stage/state; per-test log contained no ANR evidence |
| [CI 39074321502](https://github.com/sorin-202/EmmanueLA/actions/runs/39074321502), `02c3d60` | [PASS: build](https://github.com/sorin-202/EmmanueLA/actions/runs/39074321502/job/117518218964), exact clean command in 7m 13s | [PASS: general Android](https://github.com/sorin-202/EmmanueLA/actions/runs/39074321502/job/117518218853): 45 cases, 44 passed, 0 failures/errors, 1 missing-browser skip. [PASS: Firefox](https://github.com/sorin-202/EmmanueLA/actions/runs/39074321502/job/117518218956): 1 case, 1 passed, 0 failures/errors/skips | Corrected search-action observation, exact persisted states and recreation assertions passed; real Firefox address/overlay/visible-reason/Home-retention assertions also passed |
| Local API 35 emulator | Device command attempted | ABORTED: framework/system crash; 0 tests executed | No application-test success or failure can be inferred from this attempt |
| Local API 29 emulator / Firefox 157.0.1 | Browser installation and default resolver established | Preflight FAIL: Android UI dump timed out | No complete local browser-enforcement test or acceptance pass is recorded |

The final implementation workflow at `02c3d60` concluded **SUCCESS**: build, general Android and dedicated Firefox jobs all passed. Earlier `15ea8b2` and `392cd53` workflows also succeeded. The `3ca9e84` documentation rerun failed its single search-action persistence case despite passing build/browser jobs; its failure and subsequent correction are recorded below. The documentation-only `1529d54` rerun concluded **FAILURE** because of its dedicated Firefox test; its build and general jobs passed. Its observed delayed-hint failure is preserved and the corrected fixture path was exercised successfully in `15ea8b2`. The signed-beta job was skipped by branch policy. Earlier `a52c86f` and `94e5368` workflows failed because their dedicated browser setup/test failed despite successful build and general jobs; those failures are preserved above.

The first two CI runs exposed a real application defect: `WebSearchFlowTest.keyboardSearchSubmitsImmediatelyWithoutAResultTap` crashed in `CursorSearchField.getCursorRect` with `offset(21) is out of bounds [0, 0]`. Input state had advanced while caret drawing still used an empty text layout. Checkpoint `5ca6313` skips drawing against stale layout text and bounds offsets against the measured layout; a rapid replacement/selection/clear/Search/custom-caret regression was added. In run `38969896800`, the original keyboard test passed and the entire suite finished without that process crash. The new exact caret/input regression still failed; its remaining cause was not established by fixing the out-of-bounds drawing.

Other failures in the first two runs were both Home drag geometry/cancellation cases, the combined Home shortcut workflow, the notification worker's undeliverable-count assertion, and one LauncherFlow case per run (navigation in the first; temporary-suppression control in the second). All Home drag/shortcut, folder, Focus service, notification, and LauncherFlow cases passed in run `38969896800`. `WebSearchExecutionTest.slashEnterPersistsQueryAndStartsConfiguredEngineWithoutResultTap` also passed, exercising actual AllApps input through persisted history and an intercepted Android search launch. The supported-browser diagnostic remained skipped because its prerequisite was absent; URL enforcement was not verified by that skip.

Checkpoint `a2c512b` checks effective posting permission and confirms a matching Android summary publication before acknowledging counts. It also guards acknowledgement with `retainedAt`, so an old snapshot cannot mark a different cleared/recreated package group as announced. Those notification runtime assertions passed in run `38969896800`. `0b8f559` scales widget line height with font size and preserves clock seconds in preview; its parity test now asserts enlarged multiline spacing and seconds in both Home and preview. `29d567b` synchronizes Android gesture/control fixtures with their current rendered geometry, with the Home cases passing in that run.

The later runs distinguish several independent issues:

- **Caret/input regression, passed at `a52c86f`.** Source review found that external `TextFieldValue.copy(text=…, selection=…)` retained IME composition; `11f7827` defensively ends composition when adopting an external query. That correction alone was not a confirmed explanation for the exact assertion failure: run `39045032309` still failed after Clear. In the healthier `963f87e` run, the exact first-replacement `AnnotatedString` assertion failed even though expected and displayed query text printed identically. `26ddd2e` adds callback/selection/composition traces. Checkpoint `96a5e35` checks exact raw `InputText.text`, displayed `EditableText.text`, and selection, while reporting IME-owned span metadata in diagnostics. The complete regression—including rapid replacement, noncollapsed selection, Clear, Search and visible custom-caret recovery—PASSED in run `39047521783`. Deadlines are unchanged; this passing path does not establish composition as the sole cause of all earlier failures.
- **Home navigation obstruction, corrected with native verification at `963f87e`.** `dcb5a6d` stopped selecting the outgoing Home surface's “Home” description and used settled native touches. Those touches exposed a production defect: the Settings/+App controls lacked navigation-bar insets. CI logcat showed Android's `LAUNCHER_TASKBAR_HOME_BUTTON_TAP` and AOSP Quickstep taking the foreground. `963f87e` adds navigation-bar padding only to the existing controls column; saved widget positions and the placement layer remain unchanged. The extended navigation-bounds/native-launch regression PASSED in run `39046117623`.
- **Widget preview parity, verified on the API35 emulator.** `WidgetStyleFlowTest.customHomeTextScaleHasMatchingPreviewAndHomeHeight` PASSED at both `963f87e` and `a52c86f`, including enlarged multiline line spacing, seconds text, and preview/Home width and height equality. This does not replace physical checks for other widgets, fonts, sizes and devices.
- **Style import, passed at `a52c86f`.** At `963f87e`, the import route reached Widget Style but failed waiting for **Import Clock style** after the first Import JSON style touch. The recorded failure had no picker-request counter, so a missed activation versus document-result handling was not established. Checkpoint `96a5e35` brings the full clickable button into view and records safe bounds and picker requests/results. The complete test PASSED in run `39047521783`: native import taps each issued one Android document contract, an instrumentation interceptor supplied the selected file URI, preview and Cancel did not persist a style, Apply saved only the selected widget, and recreation retained it. Real document-provider selection remains physical acceptance work.

In the `26ddd2e` general job, XML confirms 45 cases, 21 failures, 0 errors and 1 skip. Many failed selectors explicitly reported **Quickstep isn't responding | Close app | Wait**; the first Focus fixture and later native Home/search/style controls could not be observed normally. Checkpoint `a52c86f` compiles/packs the test APK before starting the emulator to reduce concurrent resource contention. Its general job completed all 45 cases with 44 passed and only the missing-browser skip. All native Home, folder, Focus fixture, notification and search paths passed in that fresh run. The earlier failures remain recorded; test deadlines are unchanged.

The official-browser path now has separate evidence. The `26ddd2e` browser job verified Firefox 157.0.1, its pinned APK hash and default HTTPS resolver. Preflight advanced the observed **Continue** onboarding button and **Dismiss** hint, then observed `ADDRESSBAR_URL_BOX` with `example.com/focus-fixture` in its description. The real `BrowserDiagnosticsTest` then passed its waits for the navigated address, actual attached website-rule overlay, and blocking reason in Android's UI. The single test nevertheless **FAILED** at its last assertion: after returning to EmmanueLA, expected retained `addressReadable=true` became false. Published checkpoint `54e74dd` guards `RuleEnforcementService` against stale browser-window reads after foreground ownership changes. The same retention assertion remains in place and passed in the complete `392cd53` rerun. The earlier failure remains part of the record; neither run establishes universal-browser support.

The `a52c86f` dedicated Firefox job failed before starting instrumentation: its existing 180-second preflight limit expired while UI snapshots showed Android foreground windows without Firefox controls. It executed zero browser tests, so it supplies no result for the diagnostic-retention guard. Subsequent fixture work added one exact, bounded recovery for the observed Quickstep system ANR and retained the existing preparation deadline. At `94e5368`, setup passed without needing that recovery, but the real test still failed waiting for a readable address. Checkpoint `392cd53` adds failure-time window/address-control diagnostics without changing the existing predicates, deadlines or production application. Its complete dedicated browser rerun passed. This success does not establish the cause of the preceding address-readability timeout. The successful general Android job's one missing-browser skip supplies no replacement for that separate browser execution.

The documentation-only `1529d54` browser rerun exposed the delayed Firefox hint after preflight had already confirmed a readable toolbar. Its failure-time window dump contained only native Firefox prompt controls, including `cfr.dismiss`; the screenshot showed **Shake your device to summarize this page in seconds. ⚡** above the address bar. The production `MISSING_ADDRESS_FIELD` diagnostic was truthful: that browser window exposed no address node. Checkpoint `15ea8b2` changes only the Android fixture. Within the original 20-second address wait, it may dismiss that exact native Firefox first-run hint once, requiring the official package, exact hint text, `cfr.dismiss`, a visible/enabled/clickable native Button, the Dismiss description and Android ACTION_CLICK. It excludes page-content trees and rejects incomplete or ambiguous matches. It retains every enforcement predicate, the observable overlay/reason assertions and retained diagnostics. Production never clicks this browser prompt. The failed XML/screenshot are in `/workspace/scratch/ci-final-doc-browser/`, artifact `12115309586`; general XML is artifact `12116335860`.

The corrected `15ea8b2` dedicated Firefox job passed in [CI 39072838524](https://github.com/sorin-202/EmmanueLA/actions/runs/39072838524/job/117513827386). XML records 1 test, 1 pass, 0 failures/errors/skips (suite 6.826s; method 5.048s). Logcat confirms that the new, exact delayed-hint fixture guard executed **once** after preflight; this directly exercised the previously failing setup path. Firefox was 157.0.1 on API35, using `ADDRESSBAR_URL_BOX` content-description extraction; no Quickstep recovery was required. The unchanged real Accessibility/address, attached WindowManager overlay, visible rule reason, launcher-return and retained-diagnostic assertions all passed. Evidence is in `/workspace/scratch/ci-coachmark-fix-browser/`, artifact `12116121764`.

The `3ca9e84` general rerun failed only `LauncherFlowTest.configuredSearchActionRequiresTapAndCanBeRemoved`: a 15-second coroutine timeout occurred after the test entered search-action configuration. The test had separate cold repository collectors started after removal and addition clicks; its stack did not identify which wait expired or expose the observed action set. No ANR or application crash was captured in that method's log. Checkpoint `02c3d60` applies the continuous-observer pattern already used by the widget/editor tests for the documented DataStore 1.1.7 collector/write susceptibility. It subscribes before interaction, waits on replayed state, preserves each 15-second deadline, asserts exact removal and exact `{clock}` state, verifies new repository instances after both writes and recreates the Activity after addition. Stage-specific failures now report expected/observed action sets and native foreground labels, so an incorrect click remains a failure with usable evidence. The generic click helper and production application are unchanged. This correction addresses a known susceptibility; it does not establish it as the sole cause of the preceding timeout. General evidence is `/workspace/scratch/ci-final-head-general/`, artifact `12115814055`; build reports are `/workspace/scratch/ci-3ca9-validation/`, artifact `12116023510` (179 JVM tests passed; lint 51 warnings).

At `02c3d60`, the full general Android suite passed: 45 cases, 44 passed, 0 failures/errors, 1 missing-browser skip; the corrected search-action case passed in 7.33s with its exact state and recreation assertions. General XML is `/workspace/scratch/ci-02c-android-pass/`, artifact `12116192521`. The separate official Firefox job also passed: 1 case, 1 passed, 0 failures/errors/skips (method 4.384s; suite 5.965s). It again exercised exactly one guarded delayed-hint dismissal after preflight; no Quickstep recovery was required. All actual service/address, attached overlay, visible reason, Home-return and retained-diagnostic assertions passed. Browser evidence is `/workspace/scratch/ci-02c-browser/`, artifact `12116546272`.

The archived first/second XML files report 35/36 cases respectively, including the skip, before process termination; runner output reports 36/37 finished. Neither is a complete-suite pass count. Logs/artifacts are retained under `/workspace/scratch/ci-first-failed.log`, `/workspace/scratch/ci-second-failed.log`, `/workspace/scratch/ci-first-android/`, and `/workspace/scratch/ci-second-android/`. Later XML and per-test logs are under `/workspace/scratch/ci-third-android/`, `/workspace/scratch/ci-fourth-android/`, `/workspace/scratch/ci-native-first-android/`, and `/workspace/scratch/ci-framework-anr-android/`. The successful general XML under `/workspace/scratch/ci-a52-android-pass/`, `/workspace/scratch/ci-94e-android-pass/`, `/workspace/scratch/ci-392-android-pass/`, `/workspace/scratch/ci-1529-android-pass/`, `/workspace/scratch/ci-15ea-android-pass/` and `/workspace/scratch/ci-02c-android-pass/` confirms 45 tests, 0 failures, 0 errors and 1 skip for each run. The official Firefox preflight and failed retention test are under `/workspace/scratch/ci-second-browser/`. GitHub run links above retain the corresponding reports. Signed-beta jobs were skipped on this pull-request branch by workflow policy; no signed-release installation success is claimed.

The local API 35 run reported `INSTRUMENTATION_ABORTED: System has crashed`; investigation identified the emulator/framework `LostNetworkStack` failure. Its zero-test result is separate from the real caret crash observed in CI. Local logs are `/workspace/scratch/connected-first.log` and `/workspace/scratch/connected-resumed.log`.

The local API29 software emulator installed official Firefox 157.0.1 and resolved HTTPS links to it. Its bounded browser preflight advanced Continue and issued the example navigation, then failed because Android's UI dump exceeded its command limit. `/workspace/scratch/firefox-preflight-final-attempt/result.json` records `verified=false`. No enforcement test success is inferred from installation or this incomplete setup.

The successful general job used the workflow's API35 / Android 15 x86_64 default image, Pixel 2 profile and KVM, with animations enabled. Its only skipped method was `BrowserDiagnosticsTest.supportedBrowserNavigationActuallyDisplaysBlockingOverlay`. **Dedicated Firefox verification passed at `392cd53`:** its setup artifact records Firefox 157.0.1 in a normal tab with `ADDRESSBAR_URL_BOX` exposing `example.com/focus-fixture` through its content description; no Quickstep recovery was needed (`wait_attempts=0`). The real test required Accessibility connection, address readability, `OVERLAY_SHOWN`, the visible exact blocking reason, return to the launcher and retained browser diagnostics without an issue. XML records 1 test, 1 pass, 0 failures/errors/skips (suite 5.342s; method 3.565s). That earlier artifact is `/workspace/scratch/ci-final-browser/`, GitHub artifact `12107595024`; its general XML is artifact `12106689030`. The `15ea8b2` browser and general artifacts are `12116121764` and `12116341081` respectively. Final `02c3d60` browser/general artifacts are `12116546272` and `12116192521`. Physical browser/version/state coverage remains open.

## Test definitions added

Source counts at this snapshot are **179 JVM test methods and 45 Android test methods**. Against `b3de953`, file/method-name comparison finds **56 added JVM definitions** and **28 newly named Android definitions**, including one replacement/renaming of the previous browser diagnostic test; the Android net increase is 27. These are source counts, not execution results. Existing methods also gained assertions and integration coverage.

| JVM class | Added definitions | Coverage |
|---|---:|---|
| `BrowserAdapterTest` | 2 | Address descriptions, ownership, editing and failure distinctions |
| `ConfigurationTest` | 1 | Portable folder appearance/grid data without device image grants |
| `FocusTargetsTest` | 5 | Mixed app/browser accounting, live rules, Break/schedules, URL matching/normalization |
| `FolderAppearanceTest` | 8 | RGB, independent appearance, local URIs, grants and bounded decoding |
| `FolderGridDimensionsTest` | 6 | 4×4 migration, custom grids, packing, adaptation and invalid limits |
| `HomePlacementTest` | 3 | Snapping and measured bounds |
| `HomeShortcutTest` | 7 | Persistence, policy-aware selection, placement and invalid data |
| `NotificationRulesTest` | 1 | Default/explicit per-app summary intervals |
| `NotificationSpamTest` | 11 | Modes, frequency/similarity, replay, allowlists, essential guards and persistence |
| `WebSearchHistoryTest` | 4 | Slash parsing, bounded history/deduplication, corrupt storage and engine choices |
| `WidgetStyleImportTest` | 8 | Strict schema/input validation, independent styles, defaults and presets |

| Android class | Newly named definitions | Coverage |
|---|---:|---|
| `BrowserDiagnosticsTest` | 1 replacement | Real supported-browser address and blocking overlay; browser required |
| `FocusServiceTest` | 1 | Already-open/external app, live rules, permissions and real address-control fixture |
| `FolderAppearancePersistenceTest` | 3 | Repository recreation, Android decoding, deletion/revocation/fallback |
| `FolderUiTest` | 2 | Solid backgrounds, adaptive grid/scroll/order |
| `HomeDragTest` | 3 | Held release/cancel, no persistence during movement, existing tap action |
| `HomeShortcutFlowTest` | 3 | Add/move/cancel/remove, policy checks, launch and hidden-app exclusion |
| `NotificationDetailsFlowTest` | 1 | Quick Digest selection through persisted settings and real capture/dismissal |
| `NotificationDigestWorkerTest` | 1 | Real summary delivery and unannounced counts under posting denial |
| `NotificationListenerTest` | 2 additional | Confirmed-dismissal accounting/persistence and old-publication acknowledgement after clearing/recreation; existing listener case expanded |
| `WebSearchExecutionTest` | 1 | Real AllApps Enter → persistence → intercepted Android search launch |
| `WebSearchFlowTest` | 8 | IME/Enter, history, engine choice/encoding, folder result and caret regression |
| `WidgetStyleFlowTest` | 2 | Import preview/Apply/persistence and preview/Home typography, multiline spacing and seconds parity |

Five Python host tests in `.github/scripts/test_prepare_firefox_fixture.py` were rerun at `02c3d60` and also pass. They verify exact system-dialog recognition, rejection of other/repeated ANRs, rejection of browser-content impersonation, and refusal to tap a disabled Wait control. Command: `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v`. These fixture checks do not verify Android application behavior.

## APK size

| Artifact snapshot | Bytes |
|---|---:|
| Baseline release APK | 2,789,120 |
| Final unsigned release APK at `02c3d60` | 2,846,500 |
| Release difference | +57,380 (+2.057%) |
| Final debug APK at `02c3d60` | 14,483,105 |
| Final release AAB at `02c3d60` | 5,699,437 |
| Preceding release AAB at `15ea8b2` | 5,699,436 |
| Preceding release AAB at `392cd53` | 5,699,437 |
| Preceding release AAB at `94e5368` | 5,699,438 |

Artifacts from the required clean build are `app/build/outputs/apk/debug/app-debug.apk`, `app/build/outputs/apk/release/app-release-unsigned.apk`, and `app/build/outputs/bundle/release/app-release.aab`. These are comparable release APK snapshots, not a claim about installed size or all bundle splits. Production dependencies were not expanded for the style importer, web history, folder appearance or spam classifier; added test tooling is identified in the implementation inventory.

## Platform limitations

- **Accessibility:** Android controls enabling, binding and revocation. Force-stop, locked/unavailable windows, reboot/unlock rebinding and manufacturer power management can interrupt enforcement. System permission, lock-screen and known emergency controls remain reachable. Focused-window behavior does not establish multiwindow enforcement.
- **URLs:** This is an Accessibility address-control/overlay mechanism, not a network firewall. Only implemented browser adapters are eligible; browser versions, private modes, hidden bars and native browser prompts may make addresses unreadable. No universal browser support is claimed. Website usage budgets count browser usage because Android does not expose per-site usage totals.
- **Search defaults:** Android exposes a web-search handler, not a browser's internal search-engine preference. The configured in-app engine/default-handler fallback is documented in [SEARCH.md](SEARCH.md).
- **Notification originals:** Filtering happens after posting and cannot undo a sound or heads-up alert. Android supplies no general listener API to restore an original cancelled notification with its source identity/actions. The app keeps counts/reasons, offers the source-app path, and allows future notifications; original restoration is not promised.
- **Notification content and essential messages:** Android can redact content; applications may miscategorize notifications or use unrecognized security wording. Category/ongoing/foreground-service guards and allowlisting are implemented, but real critical-app coverage requires device testing. Stronger local heuristics can produce false positives.
- **WorkManager:** Periodic summaries have a 15-minute minimum interval and approximate scheduling. Posting permission, channel settings and battery restrictions can prevent or delay presentation; unavailable delivery must retain unannounced counts.
- **Documents:** Providers control image availability and URI-grant behavior. Deleted/revoked sources require fallback, and local references do not transfer through portable backups.

## Physical-device acceptance remaining

No physical-device acceptance result is recorded in this report. For each requested section, record the device/Android version, exact interaction or launch route, relevant permission/rule state, persisted state after restart, and observable result. Include at least:

- Widget held drag/release/cancel and ordinary taps; style preview/Home parity; shortcut launch/movement/removal with hidden/private/blocked/protected-folder rules.
- Slash search through real keyboards and configured browsers; five-entry history/restart/clear; folder authentication.
- Real image-provider selection and grant lifecycle; RGB validation, custom grids, large font/small screen adaptation, scrolling and ordering.
- App blocking while already open and through Recents, another launcher, notifications and deep links; daily/session allowances, schedules, Break expiry, warnings/grace, permission revocation/rebind and reboot.
- Supported browser versions in normal/private modes with visible/hidden bars, redirects/back/forward, matching and unrelated domains, encoded keywords, and actual overlay visibility.
- Real notification app categories, schedules, keywords, suppression expiry, digest review/clearing/retention, worker delivery under denied permission/channel/battery restrictions, spam reasons/overrides and essential-message preservation.

**Final acceptance sign-off: PENDING.**

## Commits created

This list records 31 implementation/test/CI/documentation checkpoints relative to `b3de953` through `02c3d60`. The final validation-documentation checkpoint, `docs: record final persistence regression and validation results`, follows these commits, bringing the repair series to 32 commits, and is identified in the PR/repository history. The `02c3d60` checkpoint passed host, general Android and dedicated Firefox validation. Historical failures and corrections are reported above.

```text
55f90fb fix(home): drag widgets directly after a held press
935b421 feat(widgets): add independent presets and safe local style imports
60af073 Implement per-folder backgrounds and adaptive custom grid sizes
63b7ec4 fix: retain confirmed notification digests and expose review controls
4f238dd feat(home): persist movable app icons with protected launch routing
89f2d84 fix(focus): repair callback-driven app and browser enforcement
f0240db fix(search): execute keyboard web searches and show compact folder results
9769c47 fix(integration): retain digest scheduling and manage folder image grants
3e33c58 fix(home): keep tap and held drag ownership in one gesture
4f4f444 feat: filter notification spam locally with reviewable reasons and overrides
678cec2 fix(widgets): match preview to custom Home typography
2a81112 fix(notifications): enable digest when selected in app details
366f108 test(notifications): verify quick digest selection through real capture
5ca6313 fix(search): draw custom carets only against the current text layout
a2c512b fix(notifications): acknowledge only confirmed matching digest publications
0b8f559 fix(widgets): preserve scaled line spacing and preview clock seconds
29d567b test(android): synchronize real gestures with current rendered controls
11f7827 fix(search): end stale IME composition on external query changes
dcb5a6d test(widgets): target settings rows after Home navigation settles
7368025 test(browser): verify official Firefox URL enforcement on KVM
963f87e fix(home): keep Settings and app placement controls above system navigation
26ddd2e test(android): trace Clear callbacks and dismiss the observed Firefox hint
54e74dd fix(focus): retain verified browser diagnostics across window transitions
96a5e35 test(android): verify exact search text and native document import requests
a52c86f ci: compile test APKs before booting Android fixtures
94e5368 ci: recover once from the observed Quickstep startup dialog
392cd53 test(browser): capture actual failure-time chrome and window state
1529d54 docs: record device repairs and validation evidence
15ea8b2 test(browser): finish the observed delayed Firefox onboarding hint
3ca9e84 docs: finalize browser fixture validation and acceptance report
02c3d60 test(search): observe action changes continuously through persistence
```

**Automated verification: PASSED. Physical-device acceptance: PENDING.** Host clean results, artifact sizes, general Android and dedicated Firefox results are recorded above. The current changed-file inventory is maintained in [DEVICE_REPAIR_IMPLEMENTATION.md](DEVICE_REPAIR_IMPLEMENTATION.md); final correction/reporting commits are identified in [PR #1](https://github.com/sorin-202/EmmanueLA/pull/1) and repository history.
