# Settings and page appearance — 2.7.0-beta

Version **2.7.0-beta**, version code **16**. APK source checkpoint: **`4fc868903ee048749325027fbf6b8d323c471173`**. The [comparison with `0f2b98d`](https://github.com/sorin-202/EmmanueLA/compare/0f2b98d...4fc868903ee048749325027fbf6b8d323c471173) lists **50 changed source/test/workflow paths**; this report and the README update bring the refresh to **52 changed paths**. The documentation checkpoint follows the tested source and does not change the APK.

The settings refresh passes the required local command and final CI **on its first attempt, without reruns**: **217 JVM tests**, all **67 unique Android methods** across the general and dedicated browser fixtures, and **13 host regression tests**. All **10 actual PNG captures** were retained and reviewed. The rebuilt APK is verified below. Earlier failed runs and their corrections remain documented; **physical-device acceptance is still required**.

| Implementation checkpoint | Change |
| --- | --- |
| [`7b6fccd`](https://github.com/sorin-202/EmmanueLA/commit/7b6fccd) | Persisted Home modes, page wallpapers and folder cover visibility. |
| [`6909a8c`](https://github.com/sorin-202/EmmanueLA/commit/6909a8c) | Connected settings, Home modes, page appearance and tests. |
| [`38f98bf`](https://github.com/sorin-202/EmmanueLA/commit/38f98bf) | CI exit-status handling and unavailable Firefox snapshots. |
| [`d45f953`](https://github.com/sorin-202/EmmanueLA/commit/d45f9538c6e072275ff76f1713e28b7f50b5a4e8) | Actual wallpaper contrast and settled test interaction corrections. |
| [`22d8f11`](https://github.com/sorin-202/EmmanueLA/commit/22d8f11cee7ea86e616dcf7e37309cb3db2547c2) | Precise settings targets and UTP screenshot archival. |
| [`4fc8689`](https://github.com/sorin-202/EmmanueLA/commit/4fc868903ee048749325027fbf6b8d323c471173) | Navigation-safe settings viewport, compact focus status and final fixture/capture preparation. |

## Menus and visual identity

The previous settings overview placed many destinations and adjustments in a long sequence. Home exposed overlapping control settings, and folder and focus editors showed detailed controls together with the essential choices. The correction changes grouping and disclosure, while keeping the existing monochrome palette, typography, page navigation, Home widgets and blank/start fallback.

The overview now puts **Home**, **App List** and **Folders** under **Your pages**; **Live the Moment**, **Mindful Use** and **Private Space** remain directly reachable under **Focus & privacy**. **Personalize** contains appearance and gestures. **App & system** contains apps, system and about. The latter two sections expand on demand. Section headers have summaries and explicit expanded/collapsed accessibility state.

The settings header gives the page title its own line and exposes a labeled **More options** control with shown/hidden state. It retains the existing persisted advanced setting. Closing an individual section changes presentation; it does not clear saved values or editor drafts. Home alignment, spacing and typography are grouped under **Layout & text**. Bottom actions are configured through **Bottom controls**, replacing duplicate Home entries. App List layout, search and alphabet settings have separate sections; search actions remain inside Search. Detailed manual geometry remains available under **Fine tuning**. Folder editing separates appearance, selected apps, layout/size and privacy, with Save and Cancel outside those sections.

The new settings captions have English, Romanian, German, Spanish, French, Italian and Polish resources. This increment adds no dependency or second launcher surface.

## Home: three modes from one slider

Open **Settings → Home → Home layout**. The discrete slider and its three labeled choices select:

| Mode | Actual Home behavior |
| --- | --- |
| **Normal** | Favorite applications shown with icons and labels, using the existing application launch and long-press actions. |
| **Minimalist** | The existing favorite applications as a text list. |
| **Alphabet** | An application list with an alphabet rail, initially showing All, with favorite and tag shortcuts retained. |

The preview follows the slider while it moves. Selection is persisted when the gesture finishes or a labeled choice is tapped, rather than on every drag frame. The real Home selects its renderer from the stored mode. Widgets, positioned application shortcuts, Home wallpaper and existing page gestures continue through their existing paths.

The new optional `ExperiencePreferences.homeMode` accepts only those three values. An older configuration without it derives Alphabet from the previous `homeAlphabet=true` flag, otherwise Minimalist. Selecting a mode also synchronizes that legacy flag. Existing users therefore keep their prior text/alphabet Home until they choose a different mode. Favorite order is preserved; hidden favorites are filtered before applying the visible count, and unavailable favorites retain editable references. Launches continue through existing blocked/private-app checks.

Primary implementation: [`HomeModes.kt`](../app/src/main/java/com/emmanuela/launcher/data/HomeModes.kt), [`HomeModeSelector.kt`](../app/src/main/java/com/emmanuela/launcher/ui/settings/HomeModeSelector.kt), [`HomeScreen.kt`](../app/src/main/java/com/emmanuela/launcher/ui/home/HomeScreen.kt), [`HomeAlphabet.kt`](../app/src/main/java/com/emmanuela/launcher/ui/home/HomeAlphabet.kt).

## Independent App List and Folders wallpapers

Open **Settings → App List → Wallpaper** or **Settings → Folders → Wallpaper**. **Choose photo** opens Android's document picker for images. A bounded decoder checks that the document can be read before it becomes the draft. External document providers must support a retained read permission; app-owned documents already have durable access. Permission or decoding failures produce an error and keep the saved wallpaper.

The editor previews the draft and supports pinch/drag positioning, Fill/Fit, dimming and optional blur/reset. **Save wallpaper** persists that page's URI and crop/effects through the view model and configuration repository; the actual page renders that saved selection. **Cancel** restores the previously saved draft. Leaving an unsaved editor does not save its changes. **Remove wallpaper** clears only the current page's entry.

`UiPreferences.pageWallpapers` contains only the `apps` and `folders` keys, each with its own `PageWallpaper(uri, PhotoStyle)`. Validation rejects unsupported pages, non-local/malformed document URIs, out-of-range crop/effect values and unknown fit modes. Configurations without this field keep the original page backgrounds. Home's existing photo selection and crop settings are separate, and the blank/start page is unchanged.

Both pages reuse the folder decoder's 12 MiB image cache, bounded image dimensions and EXIF handling. Access is checked again on resume even for cached images. A deleted or inaccessible photo falls back to the page theme while retaining the saved choice for replacement/removal. Switching between decoded photo and fallback keeps the page content at the same composition location, preserving ordinary in-memory page state such as search. A loaded photo uses readable foreground colors and dark Fit letterboxes, including in the light theme.

Discarded draft/replaced-image permissions are released only after checking all persisted references: both new page wallpapers, folder covers, Home photos and the Home album. A shared image is retained while another surface still references it. Rotation preserves the draft and its grant. Portable backup removes device-specific page image references, as it already does for other local images; users choose those images again on another device.

Primary implementation: [`PageWallpapers.kt`](../app/src/main/java/com/emmanuela/launcher/data/PageWallpapers.kt), [`PageWallpaperSettings.kt`](../app/src/main/java/com/emmanuela/launcher/ui/appearance/PageWallpaperSettings.kt), [`FolderImageGrants.kt`](../app/src/main/java/com/emmanuela/launcher/data/FolderImageGrants.kt), the two page wrappers in [`LauncherUi.kt`](../app/src/main/java/com/emmanuela/launcher/LauncherUi.kt), and `savePageWallpaper` in [`LauncherViewModel.kt`](../app/src/main/java/com/emmanuela/launcher/LauncherViewModel.kt).

## Folder photo covers: optional icon and name

In the folder editor's **Appearance** section, selecting a cover photo exposes separate **Show folder icon on cover** and **Show folder name on cover** toggles. The preview updates immediately. Save persists the choices on that individual folder; grid cards and freely positioned folder cards both use them.

`AppFolder.showCoverIcon` and `showCoverName` default to true when absent, preserving older folders. Hiding them affects a successfully decoded cover only. If its photo becomes inaccessible, the visible icon and name return, while the folder's choices remain saved. A photo-only card removes the cover's dark scrim; the opened folder keeps its readable backdrop. The underlying folder name, accessible identity, protected-folder lock indicator and authentication/opening path remain intact. Folder contents, order and custom grid dimensions are retained when appearance is saved.

Primary implementation: [`Folders.kt`](../app/src/main/java/com/emmanuela/launcher/data/Folders.kt), [`Editors.kt`](../app/src/main/java/com/emmanuela/launcher/ui/folders/Editors.kt), [`FolderAppearanceEditor.kt`](../app/src/main/java/com/emmanuela/launcher/ui/folders/FolderAppearanceEditor.kt), [`FolderAppearance.kt`](../app/src/main/java/com/emmanuela/launcher/ui/folders/FolderAppearance.kt), [`FolderGrid.kt`](../app/src/main/java/com/emmanuela/launcher/ui/folders/FolderGrid.kt), [`DenseFolderCanvas.kt`](../app/src/main/java/com/emmanuela/launcher/ui/folders/DenseFolderCanvas.kt).

## Live the Moment: clearer editing and actual permission state

The editor starts with the group name and **What to block**, using Apps/Sites/Keywords tabs. Apps can be filtered before selection. Website targets keep their browser choices, actual Accessibility permission/service state and **Enforce outside launcher** control visible. **Block continuously** and an existing group's Break control remain direct actions. A visible allowance summary explains the existing default when continuous blocking is off.

Optional rules are grouped into **Usage limits**, **Pause & intention**, **Schedules** and **Protection**. These retain the same rule fields, validation, authentication and Save behavior. Drafts survive collapsing a section and saveable-state restoration. Pending site and keyword entries still save from either tab. The background-access section shows one current-state line distinguishing enforcement disabled, missing permission, waiting for service and connected service. Current issues remain visible; explanations, browser compatibility and diagnostics expand separately. **Create group** sits beside **Groups**, and unprotected groups omit the redundant unprotected-status summary. Enforcement semantics, permission checks and authentication requirements are preserved.

This refresh changes the editor and status presentation, **not the Accessibility enforcement architecture or rule semantics**. It does not automatically grant permission, enable background enforcement or replace the prior default allowance with an immediate block. For selective Shorts blocking, the existing `youtube.com/shorts` path target and browser selection remain applicable. A whole-browser application target, bare `youtube.com` target or broad `youtube` keyword can independently block ordinary browsing; the menu does not change that OR matching behavior. Full readable browser addresses are still needed for path/query matching. Browser updates, hidden address bars, private states, Android/OEM service restrictions and physical-device behavior remain platform-dependent. Prior repair evidence is recorded in [`LIVE_MOMENT_URL_REPAIR.md`](LIVE_MOMENT_URL_REPAIR.md).

Primary implementation: [`FocusUi.kt`](../app/src/main/java/com/emmanuela/launcher/ui/mindful/FocusUi.kt), [`FocusEnforcementStatus.kt`](../app/src/main/java/com/emmanuela/launcher/ui/mindful/FocusEnforcementStatus.kt), [`SettingsSections.kt`](../app/src/main/java/com/emmanuela/launcher/ui/settings/SettingsSections.kt), [`StructuredSettings.kt`](../app/src/main/java/com/emmanuela/launcher/ui/settings/StructuredSettings.kt).

## Verification at the implementation checkpoints

The suite contains **217 JVM methods** and **67 Android methods**: **19 new JVM** and **16 new Android** methods relative to `0f2b98d`. Test definitions are coverage inventory, not evidence that connected tests passed.

| Added tests | Coverage |
| --- | --- |
| `HomeModesTest` — 5 JVM | Legacy migration, persistence with favorites/shortcuts/widget positions, explicit mode precedence, invalid backup/selection rejection. |
| `HomeFavoritesTest` — 4 JVM | Manual order, hidden filtering before limits, unavailable references and secure-launch eligibility, zero visible favorites. |
| `PageWallpaperTest` — 8 JVM | Independent changes/removal, full configuration round trip, legacy defaults, page/URI/effect validation, portable export and shared grant references. |
| `FolderCoverVisibilityTest` — 2 JVM | Legacy visible labels and independent per-folder configuration round trip. |
| `HomeModesFlowTest` — 3 Android | Real settings slider → DataStore → Home → recreation; hidden/blocked launch paths in all modes; an icon favorite launching an installed app. |
| `PageWallpaperFlowTest` — 4 Android | Independent actual rendered/persisted pages; document-picker draft/Cancel/Save/Remove; pinch/pan after saving; deleted-photo resume fallback and content-state retention; light-theme Fit letterboxes/readability and restored dark text after removal. |
| `FolderCoverUiTest` — 5 Android | Independent visible labels, photo-only grid/canvas cards and protected accessible identity, preview toggles, deleted-image fallback, real editor draft restoration and persistence preserving contents/order/dimensions. |
| `SettingsNavigationTest` — 3 Android | Grouped routes and More options persistence; search disclosure and edited preference restoration; representative Home/focus navigation. |
| `FocusWebsiteEditorTest` — 1 added Android | Optional section drafts across collapse/restoration, actual Save/DataStore and editor reopening. |

Existing `FolderAppearancePersistenceTest` also checks cover flags. Existing launcher workflow tests follow the new grouped routes while retaining their business assertions. The existing focus editor, service, URL matcher and dedicated browser tests remain part of regression verification.

The Android workflow retrieves actual UI screenshots from **10 capture points**: eight representative settings/Home states plus light-theme Fit and removed-wallpaper states. This is capture infrastructure; it does not by itself establish that every capture was produced or visually reviewed.

Local command:

```sh
./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease --stacktrace --max-workers=2
```

The workspace used JDK 17 and its installed Android SDK, with the local Kotlin daemon heap supplied as the environment property `ORG_GRADLE_PROJECT_kotlin.daemon.jvmargs=-Xmx2048m`; no project heap setting was changed.

General connected checks ran on the CI emulator with `./gradlew :app:connectedDebugAndroidTest --stacktrace --max-workers=2`. No physical handset is attached to this workspace.

| Check | Observed result |
| --- | --- |
| Required local clean command at `6909a8c` | **PASS** — 5 minutes 59 seconds; 108 tasks, 105 executed and 3 up-to-date. Log: `ux-27-clean-build.log`. Debug APK, release APK and release bundle tasks completed. |
| JVM JUnit XML at that checkpoint | **PASS** — 217 tests, zero failures/errors/skips. |
| Local lint XML at that checkpoint | **PASS with warnings** — zero errors, 50 warnings. |
| Android test source compilation | **PASS** locally; compilation is not a connected test result. |
| CI [39163346708](https://github.com/sorin-202/EmmanueLA/actions/runs/39163346708), build job | **PASS**. |
| Same CI run, general Android job | **FAILED** — 67 methods: 58 passed, 6 failed, 3 expected browser-prerequisite skips. |
| Same CI run, dedicated Firefox job | **FAILED during preflight**, before browser tests, when a UI snapshot was unavailable. No browser enforcement pass is claimed for this run. |
| Host-side CI regression tests at the correction checkpoint | **PASS** — 13 Python tests in 0.024 seconds, including 8 new tests; separate from the 217 JVM/67 Android inventory. |
| Required local clean command after `d45f953` | **PASS** — 6 minutes 47 seconds; 108 tasks, 105 executed and 3 up-to-date. Log: `ux-27-final-clean-build.log`. JVM: 217 tests, zero failures/errors/skips. Lint: zero errors, 50 warnings. |
| Correction-checkpoint CI [39192476515](https://github.com/sorin-202/EmmanueLA/actions/runs/39192476515), build job | **PASS** — 5 minutes 46 seconds; actual report contains 217 passing JVM tests, zero failures. |
| Same correction run, dedicated Firefox job, first attempt | **PASS** — 3 Android tests, zero failures/errors/skips; artifact `12160294557`. |
| Same correction run, general Android job | **FAILED** — 67 tests: 61 passed, 3 failed, zero errors, 3 expected browser-prerequisite skips. All three failures are in `SettingsNavigationTest`; the run is **FAILED overall**. |
| Third CI [39193583822](https://github.com/sorin-202/EmmanueLA/actions/runs/39193583822), source [`22d8f11`](https://github.com/sorin-202/EmmanueLA/commit/22d8f11cee7ea86e616dcf7e37309cb3db2547c2), build | **PASS**. This fifth checkpoint connects precise settings targets and UTP screenshot archival. |
| Same third run, Firefox and host regressions, first attempt | **PASS** — 3 Android browser tests and 13 Python tests. |
| Same third run, general Android job | **FAILED** — 67 tests: 60 passed, 4 failed, zero errors, 3 expected browser-prerequisite skips. The run is **FAILED overall**. |
| Third-run screenshot retention and review | **VERIFIED for 6 PNGs** — all six archived files were retrieved and reviewed; the other four capture points were not reached. |
| Required local clean command at final source `4fc8689` | **PASS** — 4 minutes 19 seconds; 108 tasks, 105 executed and 3 up-to-date. Log: `ux-27-final-viewport-clean.log`. JVM: 217 tests, zero failures/errors/skips. Lint: zero errors, 50 warnings. |
| Final-source CI [39194683146](https://github.com/sorin-202/EmmanueLA/actions/runs/39194683146) | **PASS overall, first attempt, no reruns**. |
| Same final-source run, build | **PASS** — required full clean command: 5 minutes 37 seconds; report verifies 217 JVM tests, zero failures/errors/ignored tests. |
| Same final-source run, general Android | **PASS** — 67 tests: **64 passed**, zero failures/errors, **3 expected browser-prerequisite skips**. |
| Same final-source run, dedicated Firefox | **PASS** — **3 tests**, zero failures/errors/skips; 16.818 seconds. Official Firefox 157.0.1 on API 35, actual `ADDRESSBAR_URL_BOX` verified, no Quickstep intervention. |
| Same final-source run, host regression tests | **PASS** — 13 tests in 0.030 seconds. |
| Unique Android coverage across both fixtures | **67 unique methods passed**: 64 general passes plus 3 dedicated Firefox passes. The general job's three skips are not counted as passes in that job. |
| Final-source visual review | **PASS for all 10 retained PNGs**, inspected at original resolution; details below. |
| Physical device | **NEEDS PHYSICAL DEVICE TEST** — no physical handset attached to this workspace. |

### Earlier failures and verified corrections

The six general-suite failures in `39163346708` comprised the light-theme wallpaper contrast case, all three new settings-navigation cases, the focus permission/master workflow at its Save stage, and the existing web-search/Break workflow. Their failed results remain recorded above; successful compilation did not establish those paths worked.

The wallpaper failure exposed a **production defect**: changing `MaterialTheme.colorScheme` does not replace an inherited `LocalContentColor`. Text without an explicit color could remain black over the photo's dark letterbox in light mode. `d45f953` explicitly provides the photo surface's white content color and restores the inherited content color when the image is removed or unavailable. The original white-glyph assertion remains, and the existing method additionally requires dark glyphs after wallpaper removal. No test count or assertion was removed.

The native settings helper could tap the center of a semantic row that extended beyond its visible scroll viewport. Original logs recorded `LAUNCHER_TASKBAR_HOME_BUTTON_TAP` before any activity recreation in the affected workflow: that interaction had reached Android Home rather than its intended settings destination. The correction intersects control bounds with ancestor viewports and the application frame, scrolls until the complete control is visible, then requires stable bounds before a native tap. It changes the test helper; production navigation is unchanged. The existing destinations, state-restoration checks and persisted-value assertions remain required.

The focus Save timeout and web-search/Break timeout did not establish a root cause in the original logs. The correction prepares settled visible touches for Save and Clear history, subscribes to web-history state before the interaction, and adds stage/current-state diagnostics. The business assertions and existing asynchronous deadlines are retained. Both methods passed in `39192476515`, as did the production wallpaper contrast regression. Those passes verify their corrected test executions; they do not prove a sole cause for the original timeouts.

The same run still failed all three `SettingsNavigationTest` methods: native text/content-description queries could choose ambiguous nodes. Checkpoint `22d8f11` uses precise Compose action/text targets in the real `MainActivity`, while retaining native touch interactions, original assertions and five-second deadlines. Its third CI run, `39193583822`, still failed all three settings-navigation cases while waiting for complete control bounds to settle, plus the Home slider case while observing a write through a cold flow. The general result was 60 passed, 4 failed and 3 browser-prerequisite skips. Checkpoint `4fc8689` adds the native swipe fallback and uses a hot settings observer with stable slider interaction; original assertions and deadlines remain required. All four methods passed in the final run, alongside every other general-suite method with satisfied prerequisites.

The Firefox preflight encountered UIAutomator's exact `ERROR: null root node returned by UiTestAutomationBridge.` on stderr while ADB returned status zero. Previously the fixture then treated the missing dump as a usable snapshot attempt. `38f98bf` handles only that identified transient state by requesting a new current snapshot within the **unchanged 180-second preflight deadline**, and validates returned XML and its `hierarchy` root. Missing/unrelated/error output cannot verify an address or browser readiness. Five new Python regressions cover null-root stderr, deadline/readiness preservation, missing dump, unrelated errors and invalid XML hierarchy.

The emulator action launches each multiline script line in a separate shell. Therefore the initial inline `ui_test_status` variable could not reliably carry the Gradle exit code through screenshot retrieval. A single [`run-ui-regressions.sh`](../.github/scripts/run-ui-regressions.sh) process retains that exit status through evidence collection. Missing captures also fail an otherwise successful suite.

The original screenshot retrieval still lost app external files: the general run's UTP configuration had `uninstall_after_test: true` for both APKs, so normal post-test uninstall removed those files before `adb pull`. Checkpoint `22d8f11` writes screenshots under the instrumentation runner's `additionalTestOutputDir`; UTP copies that output to the host before uninstall. The script archives actual PNG files from `app/build/outputs/connected_android_test_additional_output` instead of reading deleted app data. Its three host regressions verify archived output bytes, missing-output failure and preservation of a failed suite status while retaining evidence. Actual retention is verified: six PNGs were retrieved from the failed third run and **all 10** from the successful final run. The final host suite passes all **13 tests**.

The correction checkpoint's dedicated browser fixture used official Firefox **157.0.1** on **API 35**. Its preflight verified the actual `ADDRESSBAR_URL_BOX` address for `example.com/focus-fixture`; it needed zero Quickstep Wait recoveries and did not exercise the null-root recovery. The three actual browser enforcement methods passed on their first attempt. This confirms that browser fixture; it does not establish physical-device, other-browser or real YouTube navigation acceptance.

### Visual review

The third run supplied six inspected images: `settings-overview.png`, `settings-home.png`, `settings-app-list.png`, `settings-live-moment.png`, `page-wallpaper-fit-light.png` and `page-wallpaper-removed-light.png`. It did not reach the Home mode captures.

Review found redundant nested permission cards consuming excessive space in Live the Moment, and the actual settings scroll viewport extending beneath Android's three-button navigation area. Production checkpoint `4fc8689` compacts the permission presentation and puts navigation-bar padding **before** the scrolling modifier so the scrollable viewport ends above system navigation. The APK below includes these corrections; final connected tests pass.

The final run retained and supplied all **10 actual screenshots** for direct review: five settings captures (`settings-overview.png`, `settings-home.png`, `settings-app-list.png`, `settings-live-moment.png`, `settings-focus-editor.png`), three Home captures (`home-normal.png`, `home-minimalist.png`, `home-alphabet.png`), and the two wallpaper component captures. Direct and independent inspection of original-resolution PNGs confirms complete headings and Back/More options controls, the clock with each Home mode, readable slider/section controls, compact Live the Moment group controls with Create group visible above the fold, the editor heading/introduction, and settings controls above system navigation. The wallpaper components verify a dark Fit letterbox with readable white text and restored light-theme black text/counter state after removal. They are not complete App List screenshots and do not establish that page's system-bar contrast with a photo. Visual review is emulator evidence for those states, not a substitute for physical-device use or large-font testing.

The supplied **`EmmanueLA-2.7.0-beta-screenshots.zip`** contains all 10 original PNGs and a manifest identifying the source and CI run; archive size **615,614 bytes**.

The capture helper also waits for an actual committed frame through `registerFrameCommitCallback` on hardware-accelerated API 29+ devices before taking the real PNG. Earlier platforms use animation-frame callbacks. This prepares stable captures of the route and system insets; the helper retains its bounded wait and does not reconstruct the UI.

## Installation artifact

The rebuilt installation artifact is **`EmmanueLA-2.7.0-beta-settings.apk`**, built from source checkpoint **`4fc868903ee048749325027fbf6b8d323c471173`**. It replaces the earlier intermediate APK from `d45f953`; its new hash is below. Later documentation-only commits do not change the APK's source or hash. Its metadata is version **2.7.0-beta**, version code **16**. It is a **debug-signed APK**; the release APK is not presented as signed for installation.

| Artifact check | Result |
| --- | --- |
| Size | **14,709,109 bytes** |
| SHA-256 | `fb1a1e54d9b87a78320ef9e690fffb133ecd2d0ac9f72285de533487c0af6526` |
| APK signature | **PASS**, v2 signature verified |
| Signing certificate SHA-256 | `3c75b94362adeac14aa6337ae805d507dfe02d9ebfd6cb38d440c34b190f01f1`, matching the previously supplied 2.6.1-beta APK |
| ZIP integrity | **PASS**, CRC validation |
| Alignment | **PASS**, 16 KiB ZIP alignment check |

Artifact integrity and signature checks establish the packaged file's identity. The source also passes the tests and screenshot checks above; physical-device acceptance remains separate.

## Remaining acceptance checks

| Requested area | Status |
| --- | --- |
| Shorter settings and clearer Live the Moment editor | **IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST** |
| Normal / Minimalist / Alphabet Home slider and persisted rendering | **IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST** |
| Independent App List and Folders wallpapers | **IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST** |
| Per-folder icon/name visibility with photo covers | **IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST** |

On a handset, check the three Home modes after restart, ordinary tapping/scrolling/long-press and widget coexistence; menu reachability with large fonts; document selection and retained access after process death/reboot; independent wallpaper cropping and readable text on actual photos; **actual App List system-bar contrast in light theme with a photo**; inaccessible-image fallbacks; and protected photo-only folder opening. Recheck existing app and selective URL blocking on the supported browser used on that device. Emulator/build success does not establish those physical-device acceptance requirements.
