# Live the Moment: app, website and URL keyword repair

This report covers the focused repair after `fcd9ecde61a15763aa8e5ceb2b9fd2055f4f9bdd`. It describes application version **2.6.1-beta**, version code **15**, and the implementation checkpoints:

- `092f84b55e58bd1d7fc7c295c8b83a2e200ce47f` — preserve URL paths and validate saved website rules.
- `69fc8eda5a456e102d235539e192b92defe61917` — read complete browser URLs and recover active rule checks.
- `c15b9ed6a82fe33182ba1b1dcfa69997e0c69f46` — synchronize one existing launcher-flow test with the rendered Home Settings control after drawer navigation; no production change.
- `82945d995d31d10536b482a34e88ab357afeeff8` — preserve bracketed URL query parameters discovered in final review.
- `e67b7cefdb2f820bc34d08a748e011f28afe277a` — match production editor IME insets and settle the keyboard before native master-switch toggles; test fixture only.

The final documentation checkpoint is named `docs: record focused Live the Moment repair and browser verification`. Including the editor-fixture correction and documentation checkpoint, the recorded focused work comprises six checkpoints, 21 changed files relative to `fcd9ecd`, and no new production dependencies. The branch contains 38 commits and 92 changed paths relative to the original main baseline.

The final documentation commit SHA is identified by the head of [PR #1](https://github.com/sorin-202/EmmanueLA/pull/1) and the repository history. Production/APK source is `82945d995d31d10536b482a34e88ab357afeeff8`; the final test/build validation checkpoint is `e67b7cefdb2f820bc34d08a748e011f28afe277a`. That test-fixture checkpoint and documentation do not change production source. The [full implementation inventory](DEVICE_REPAIR_IMPLEMENTATION.md) lists baseline-wide files and root causes. The [original validation report](DEVICE_REPAIR_VALIDATION.md) retains the original eleven acceptance statuses and historical commit/validation evidence.

The requested behavior is selective blocking: applications can be blocked as applications; website rules can block an entire domain or a particular path; URL keywords can match the address without inspecting page contents. For example, `youtube.com/shorts` must match Shorts paths while leaving ordinary YouTube `/watch` and search pages eligible to open. This does not control the native YouTube application's internal Shorts screen.

| Requested behavior | Acceptance status |
| --- | --- |
| Application blocking | **IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST** |
| Website and selective path blocking | **IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST** |
| URL keyword blocking | **IMPLEMENTED + NEEDS PHYSICAL DEVICE TEST** |

Final automated validation at `e67b7ce` passed: **198 JVM tests**, all **51 unique Android test methods across the two fixtures**, lint with zero errors, debug/release APKs and release bundle. CI attempt 2 is green after one unchanged fresh-VM rerun of the failed general Android job. The original failed attempt, observed Quickstep system ANR, prior editor timeout and local compiler-daemon OOM remain documented below. These results retain the physical-device acceptance requirement.

## Root causes and corrections

| Path | Observed implementation defect | Correction |
| --- | --- | --- |
| Editor → saved rule | Sites and Keywords shared one pending text field. Pressing Save did not add pending text. Switching tabs could misinterpret the same text. Draft state used ordinary `remember`. | Independent saveable pending fields and draft; Add handles only the current tab; Save validates and includes both pending entries. Validation errors keep the editor open and preserve the saved rule. |
| Mixed app and website groups | Selecting an app could satisfy the old Save condition even when website targets had no selected browser. | UI and `LauncherViewModel.saveFocusGroup` require a supported browser whenever a group contains sites or URL keywords. The list also identifies invalid existing configurations. |
| Website path → persistence → match | Adding a site called the host-only normalizer, discarding `/shorts`. A path supplied directly to the old matcher was not a supported site target. | `FocusWebsiteRules` validates and stores the normalized host plus path. Domain-only entries still match their subdomains. A path matches itself and child path segments, rather than `/shorts-music`, query text containing `/shorts`, or an unrelated domain. |
| Keyword → URL normalization | Whole-address form decoding changed literal path/fragment `+` into a space. Unicode equivalents were not normalized consistently. | Match raw and decoded HTTP(S) URL forms with NFC normalization. A literal `+` stays literal in paths and fragments; query form-style `+` can represent a space. Matching remains case insensitive. Invalid schemes, credentials and malformed hosts are rejected. |
| Accessibility address control → URL | The first valid field value could be a domain-only text label even when the same control's description contained the full path/query. Description tokenization could split legal commas/semicolons. | Reconcile compatible text/description candidates and prefer the detailed address. Preserve valid punctuation and encoded aliases. Treat contradictory hosts or URL details as `AMBIGUOUS_ADDRESS`; do not choose an arbitrary page. The Firefox-specific navigation-hint separator is removed only from a hint label, preserving a real trailing period in a plain URL. |
| Final-review bracketed query | Description tokenization treated `[` and `]` as separators, truncating a valid address such as `https://example.com/search?filters[category]=shorts` before its keyword. | Preserve query brackets. Strip brackets only when they form a matching outer `[URL]` label wrapper; keep a legitimate trailing bracket inside query data. |
| Rule update → active browser read | A previous-generation browser read could hold the read slot while a replacement rule requested a current read. | Cancel and clear the stale browser-read job and its pending flag when rule configuration changes. The service evaluates the updated state and requests a fresh address. |
| Android callback → foreground recovery | A transient system/unavailable window could clear the recorded foreground. Subsequent content callbacks were accepted only for the already-recorded browser. Scrolling was not subscribed. | Relevant content, focus, text and scroll callbacks can reconcile the actual application window for a rule-targeted app/browser. Address inspection remains coalesced and callback driven; no busy polling loop is added. |
| Address abbreviation → block state | A domain-only observation could incorrectly replace a previously verified path URL and uncover a blocked page. | Report missing path/query coverage explicitly. When the same domain is abbreviated beneath an existing verified block, retain that block until a real detailed navigation or foreground transition can be verified. A verified different allowed URL removes the old overlay. |
| User configuration → actual enforcement | The editor did not expose the master switch, permission state and immediate-block prerequisite together. A configured Strict Block label could be mistaken for evidence of enforcement. | Expose the existing master switch and actual Android Accessibility/service state inside the website editor. Explain that immediate blocking requires Block continuously. The group list says Strict Block configured and separately reports inactive website enforcement. Saving a group does not silently grant permissions or enable the master switch. |

App targets continue through the shared `FocusTargets`/rule evaluation path. An application selected in Apps is a whole-app target, independent of whether its URL matches. The service still checks already-open applications and externally opened applications through Android application windows; it preserves permission, lock-screen and emergency controls. Existing Strict Block, usage/session limits, schedules, Breaks, warnings and grace rules remain in effect.

## Configure selective YouTube Shorts blocking

Use these steps for browser Shorts while retaining ordinary YouTube browsing:

1. Open **Live the Moment**, add or edit a group, and give it a name.
2. In **Sites**, enter **`youtube.com/shorts`**. Add it, or leave it pending and press Save after the other settings are ready. Remove any existing bare `youtube.com` site rule and broad URL keyword such as `youtube` from **all active groups** if ordinary YouTube browsing should remain available.
3. Select the browser you actually use under **Browsers with address adapters**.
4. Enable **Block continuously** for immediate enforcement. A usage allowance or schedule applies its own timing instead.
5. Turn on **Enforce outside launcher** and open **Background rule access** to grant EmmanueLA's Accessibility permission in Android. Return and check that the service is connected.
6. Save. Open `https://www.youtube.com/watch?v=...`, then `https://www.youtube.com/shorts/...`, then an ordinary `/watch` address again. Confirm that only the Shorts address displays the blocking overlay.

For a sideloaded APK on Android 13 or later, Android may require **App info → Allow restricted settings** before Accessibility can be enabled. Availability and wording depend on Android and the manufacturer. This action does not itself grant Accessibility: enable the service in Android settings, then check both the actual permission state and **service connected** in EmmanueLA. Website enforcement must not be assumed active while either required state is unavailable. The app cannot automatically grant that permission.

Site entries, URL keywords and app targets match independently (**OR**), rather than narrowing one another. Adding `youtube.com/shorts` does not limit a separate `youtube.com` site rule or a `youtube` keyword; either broad target can still activate blocking on ordinary YouTube pages. Do not select the browser itself in Apps in any active blocking group when selective browser access is intended: an app target blocks the whole browser. A separate app-blocking group can target the native YouTube application if blocking that entire app is desired.

For keywords, enter a value in **Keywords** and select its browser. A keyword is a case-insensitive substring of the exposed URL, including its query, and can match other websites too. Use a site/path target such as `youtube.com/shorts` when a domain-scoped path is intended. Page text, video titles that are absent from the URL, and encrypted network contents are not inspected.

## Verification evidence

The baseline was tested independently in an isolated worktree at `fcd9ecd`, with only the new `FocusUrlKeywordTest` regression file copied in. Five cases ran and four failed: preserving literal path plus after percent decoding, query-versus-path space semantics, equivalent Unicode keywords, and a YouTube Shorts path target. The invalid/non-web URL eligibility case passed. This reproduces real matcher defects in the previous code; it does not reproduce a physical browser or native YouTube session.

The corrected source contains **198 JVM test definitions** and **51 Android test definitions**, verified from the test source. This increment adds **19 JVM definitions** and **6 Android definitions**:

| Tests | Added coverage |
| --- | --- |
| `FocusUrlKeywordTest` — 5 | Literal plus/percent decoding; query space semantics; Unicode equivalence; non-web/foreign URL rejection; real YouTube `/shorts` versus `/watch`, home, search, similar path and hostile host matching. |
| `FocusWebsiteRulesTest` — 6 | Supported-browser validation for mixed and keyword-only groups; host/path canonicalization and invalid input; codec round-trip preservation; path and domain boundaries; nonempty/control-safe keywords. |
| `BrowserAdapterTest` — 8 additional cases | Detailed address over an abbreviated domain; www elision compatibility; comma/semicolon and bracketed query retention; encoded aliases; contradictory controls; host-only coverage; Firefox hint punctuation; detailed addresses reaching the matcher. The bracket regression extends an existing case rather than adding a test method. |
| `FocusWebsiteEditorTest` — 4 | Real Compose editor → model → DataStore saves and reload; saveable state restoration; independent pending tabs; mixed-group/browser validation; invalid pending edits leaving persistence unchanged; actual permission state and explicit master switch without changing the default allowance. |
| `BrowserDiagnosticsTest` — 2 additional cases, 3 total | Actual Firefox address discovery and visible blocking overlay for URL path and encoded path/query keywords; live rule edits; already-open permission rebind; a path-scoped Shorts-style rule allowing an ordinary `/watch` URL before and after a block. |

The dedicated browser tests use official Firefox **157.0.1** on Android with controlled **`example.com`** addresses. They check the actual address control, the service diagnosis, `OVERLAY_SHOWN`, and visible blocking text. The Shorts-style case uses `/shorts/clip` and `/watch?v=...` with the same path-matching architecture. Actual YouTube URL strings are covered by JVM matching tests; real YouTube navigation, authentication, redirects, video playback and native Shorts UI are not claimed as verified.

Results at implementation checkpoint `69fc8ed`:

| Check | Result |
| --- | --- |
| JVM tests plus Android test APK compilation | **PASS** — 198 JVM tests, zero failures/errors/skips; Android test sources compile. Root verification completed in 28 seconds. |
| Required clean unit/lint/debug APK/release APK/release bundle command | **PASS** — 7 minutes 17 seconds; 108 tasks, 105 executed and 3 up-to-date. JVM: 198 passed, zero failures/errors/skips. Local lint: zero errors/fatal issues, 50 warnings. |
| GitHub Actions [run 39129686300](https://github.com/sorin-202/EmmanueLA/actions/runs/39129686300) | **FAILED overall** — build job `117682117808` passed and Firefox passed; the general Android job failed one existing launcher-flow case. |
| General connected Android tests | **FAILED** — job `117682117887`: 51 cases, 47 passed, 1 failed, 3 expected missing-browser skips. All four new website-editor cases passed. |
| Dedicated Firefox connected tests | **PASS** — 3 tests, zero failures/errors/skips; suite 14.024 seconds. CI job `117682118014`, artifact `12137985653`. |
| Physical device | **NEEDS PHYSICAL DEVICE TEST** — no physical handset is attached to this workspace. |

The four new website-editor tests passed in 15.158, 13.944, 10.312 and 7.416 seconds. The sole general-suite failure was `LauncherFlowTest.appListWebIntentAndControlledBreakWorkflowRender`: Android rejected `ACTION_CLICK` for Launcher settings immediately after returning Home. No ANR or fatal application crash was recorded. The failure establishes the interaction stage, not a proven stale-node or clickable-ancestor root cause.

Checkpoint `c15b9ed` changes only that test method: wait for the outgoing drawer's EditText to disappear, reacquire the current Home Settings control, require stable window/bounds for three samples within the existing five-second settling deadline, inject native finger DOWN/UP at that control, and require the Settings screen before continuing every original Break assertion. No production code, generic helper, test deadline or expected business behavior is changed. Android test compilation passed in 8 seconds. The preceding failed general run is not represented as green.

Preceding correction-checkpoint results, [run 39130495181](https://github.com/sorin-202/EmmanueLA/actions/runs/39130495181):

| Check at `c15b9ed` | Result |
| --- | --- |
| Required clean local command | **PASS** — 6 minutes 30 seconds; 108 tasks, 105 executed and 3 up-to-date. JVM: 198 passed, zero failures/errors/skips. Local lint: zero errors/fatal issues, 50 warnings. |
| CI build | **PASS** — job `117684489670`; required command completed in 5 minutes 30 seconds. Artifact `12137947595`: 198 JVM tests, zero failures or ignored tests, 100% success. CI lint: zero errors, 51 warnings. |
| General connected Android tests | **PASS** — job `117684489458`, artifact `12138087032`: 51 cases, 48 passed, zero failures/errors, 3 expected missing-browser prerequisite skips; suite 187.836 seconds. |
| Dedicated Firefox connected tests | **PASS** — job `117684489599`, artifact `12138196151`: 3 tests, zero failures/errors/skips; suite 14.395 seconds. Connected Gradle command succeeded in 27 seconds. |
| CI result | **PASS** — all three validation jobs green. Signed-beta publication skipped by branch policy. |

The `c15b9ed` general suite's four new editor cases passed in 12.230, 13.322, 9.730 and 6.489 seconds. The existing application-blocking service test passed in 6.151 seconds. The corrected launcher workflow passed in 5.998 seconds; its log recorded three stable Settings geometry samples with the outgoing drawer gone before the native touch. All original Break assertions ran. The three browser skips in this general fixture are covered by the three dedicated Firefox passes, yielding all **51 unique Android test methods passed across the two fixtures**; skipped cases are not counted as passes in the general job.

The `c15b9ed` Firefox case durations were domain 3.240 seconds, keywords 6.958 seconds and selective path 2.305 seconds. Instrumentation reported `OK (3 tests)`. The five Python fixture tests passed, and the pinned Firefox checksum matched. Firefox 157.0.1 on API 35 resolved as the default browser and preflight verified the full address bar after observed Continue and Dismiss controls. Logs recorded zero ANR recoveries, ANRs, fatal crashes or instrumentation hint dismissals. These results confirm the dedicated fixture at the correction checkpoint; physical-device acceptance remains pending.

Validated local `c15b9ed` artifacts were 14,515,873 bytes for the debug APK, 2,862,884 bytes for the unsigned release APK and 5,714,938 bytes for the release AAB. The signed debug APK is the installation artifact; the release APK remains unsigned.

### Final-review query-bracket correction

Review after the green `c15b9ed` run found that a valid query such as `filters[category]=shorts` was truncated by the browser-description tokenizer. Extending the existing punctuation test reproduced this exact loss: **1 test failed in 3 seconds** before the correction. Checkpoint `82945d9` changes only `BrowserAdapters.kt` and `BrowserAdapterTest.kt`: preserve query brackets and remove only paired outer label brackets. The regression includes Chrome and Firefox descriptions, an outer `[URL]` wrapper and legitimate query data ending in `]`. All **15 BrowserAdapter tests passed in 4 seconds** after the correction. JVM/Android method counts remain **198/51**.

| Check at `82945d9` | Result |
| --- | --- |
| Targeted BrowserAdapter tests | **PASS** — 15 cases; the newly extended bracket regression first failed against the prior tokenizer. |
| Required clean local command | **PASS** — 3 minutes 52 seconds; 108 tasks, 105 executed and 3 up-to-date. JVM: 198 passed, zero failures/errors/skips. Local lint: zero errors/fatal issues, 50 warnings. Log `live-moment-82945d9-clean-validation.log`. |
| CI [run 39131507586](https://github.com/sorin-202/EmmanueLA/actions/runs/39131507586) | **FAILED overall** — build job `117687462599` passed in 5 minutes 2 seconds; artifact `12138705331`: 198 JVM tests, zero failures or ignored tests, 100% success. CI lint: zero errors, 51 warnings. The general Android job failed one case. |
| General connected Android tests | **FAILED** — job `117687462897`, artifact `12138585345`: 51 cases, 47 passed, 1 failed, zero errors, 3 expected missing-browser prerequisite skips. |
| Dedicated Firefox connected tests | **PASS** — job `117687462837`, artifact `12137799626`: 3 tests, zero failures/errors/skips; suite 15.823 seconds. |
| Physical device | **NEEDS PHYSICAL DEVICE TEST**. |

The final-checkpoint Firefox case durations were domain 4.111 seconds, keywords 6.656 seconds and selective path 2.869 seconds. Preflight verified Firefox 157.0.1 as the default browser with a full readable path. There were zero ANR recoveries, ANRs or fatal crashes. The existing narrowly scoped late Firefox onboarding-hint guard was exercised once during instrumentation; the three actual enforcement cases passed.

The sole `82945d9` general-suite failure was `FocusWebsiteEditorTest.websiteEditorExposesActualPermissionAndExplicitMasterWithoutChangingRuleDefaults`: a five-second `TimeoutCancellationException` in a 9.783-second case, without a stage or source line establishing which asynchronous transition timed out. No ANR or fatal crash was recorded. The other three editor tests passed, as did the app-blocking service case (4.353 seconds) and launcher workflow (5.487 seconds).

Inspection found that the isolated editor test fixture omitted the IME padding used by production `LauncherUi.kt`; its log also recorded keyboard appearance and an inset animation. These are fixture discrepancies and timing evidence, not proof of a sole cause of the timeout.

### Editor-fixture correction and final validation

Checkpoint `e67b7cefdb2f820bc34d08a748e011f28afe277a` changes only `FocusWebsiteEditorTest.kt`. Its fixture now applies production-equivalent `imePadding`. In the previously failing method, `WindowInsetsControllerCompat` requests keyboard dismissal; the test then requires the actual IME visibility to be false and reacquires three consecutive stable master-switch geometry samples at 100 ms intervals within a new bounded five-second interaction-precondition wait. It performs a Compose `performTouchInput` finger gesture on the current control. The original separate persisted-state and model-state five-second deadlines and every original assertion are retained unchanged. Failure diagnostics identify the stage/current values and capture the state before cleanup. Compilation passed in 2 seconds. No production code or APK-source change accompanies this correction.

| Check at `e67b7ce` | Result |
| --- | --- |
| Android test compilation | **PASS** — 2 seconds. |
| Required clean local command | **PASS on retry** — 6 minutes 42 seconds; 198 JVM tests, zero failures/errors/skips. Local lint: zero errors, 50 warnings. The first compiler-daemon OOM is recorded below. Log `live-moment-e67b7ce-clean-validation.log`. |
| CI [run 39132357055](https://github.com/sorin-202/EmmanueLA/actions/runs/39132357055), attempt 1 | **FAILED overall** — executed build job `117689932727` passed in 5 minutes 36 seconds (108 tasks: 107 executed, 1 up-to-date); artifact `12138473141`: 198 JVM tests all green and 51 lint warnings. Firefox passed; the general Android job failed. |
| General connected Android tests, attempt 1 | **FAILED** — artifact `12138532026`: 51 cases, 27 passed, 21 failed, zero errors, 3 expected missing-browser prerequisite skips. All four editor cases passed. |
| Dedicated Firefox connected tests | **PASS** — artifact `12138388472`: 3 tests, zero failures/errors/skips; suite 17.396 seconds. |
| General connected Android tests, attempt 2 | **PASS** — job `117744555438`, artifact `12145316630`: 51 cases, 48 passed, zero failures/errors, 3 expected missing-browser prerequisite skips; suite 201.344 seconds. One fresh-VM rerun after the observed system ANR; source and assertions unchanged. |
| CI attempt 2 overall | **PASS** — all three validation jobs successful; build/browser carry their prior passing executions. Signed-beta publication skipped by branch policy. |
| Physical device | **NEEDS PHYSICAL DEVICE TEST**. |

The first `e67b7ce` local full command failed after 1 minute 51 seconds in `compileDebugKotlin` with `GC overhead`/`OutOfMemoryError` from a long-lived compiler daemon. The failure is retained in `live-moment-e67b7ce-first-oom.log`. Idle Gradle and Kotlin daemons were stopped, then the exact same command was rerun with the same heap configuration and `--max-workers=2`; no source, configuration, assertion or timeout changed. That rerun passed in 6 minutes 42 seconds.

The first `e67b7ce` general CI run passed all four editor cases, including the corrected master-switch case in 6.328 seconds. Its diagnostics verified actual UI, model and persisted settings transitioning true then false. The first app-service case could not foreground its fixture before permission setup. Later failures explicitly recorded the Android modal `Quickstep isn't responding` with `Close app` and `Wait` controls. This establishes a system ANR in the run; it does not prove all 21 failures have one cause. Only the failed general job was rerun once on a fresh VM, with no weakened assertion, increased timeout or source change. Attempt 1 remains part of the evidence.

Attempt 2's archived JUnit results confirm 48 passing general cases and 3 browser-prerequisite skips. The four editor cases passed in 14.511, 13.614, 11.238 and 7.194 seconds; the app-blocking service case passed in 7.027 seconds and the Home Settings/Break workflow in 5.895 seconds. The master-switch diagnostics recorded the IME changing from visible before preparation to hidden, then three stable control-bound samples; actual UI, model and persisted values all switched on and off and every assertion passed. Inspected logs contained no Quickstep modal, `ANR in` or fatal-crash signature. The connected Gradle command passed in 3 minutes 38 seconds (67 tasks: 1 executed, 66 up-to-date), after a separate 1-minute-54-second test APK compilation step.

Across the 48 general passes and the 3 dedicated Firefox passes, all **51 unique Android test methods passed**. The three skipped browser cases in the general fixture remain skips in that job; their successful dedicated execution supplies the browser evidence.

The `e67b7ce` dedicated Firefox durations were domain 4.658 seconds, keywords 7.778 seconds and selective path 3.056 seconds. Firefox 157.0.1 exposed normal full URLs. Preflight needed zero ANR recoveries; the exact existing late-onboarding hint was dismissed once during instrumentation. No ANR or fatal crash was recorded in that browser fixture.

Attempt 2 metadata carries the already-successful build/browser jobs under new IDs `117744592180` and `117744556809`; these are carried results, not newly executed checks or new artifacts. The original executed build ID and original artifacts above identify their evidence.

The preceding green checkpoint remains historical evidence. Final validation is the verified `e67b7ce` local command and CI attempt 2, with the failed earlier attempts retained above.

### APK delivery

The final-source `82945d9` signed debug APK is available at `/workspace/artifacts/EmmanueLA-2.6.1-beta-live-moment.apk`. It is version **2.6.1-beta**, code **15**, minimum Android API **26**, target API **35**. APK v2 signature verification, ZIP CRC verification and 16 KB ZIP alignment passed. Its SHA-256 is:

```text
eea21c3237f88a5325be75298a5f07f7736ab4e26b7c4555b621587210e5449c
```

Final local artifact sizes: debug APK **14,515,873 bytes**; unsigned release APK **2,862,884 bytes**; release AAB **5,715,014 bytes**. This debug APK is an installation/testing artifact; no signed release publication is claimed.

The successful `e67b7ce` clean rebuild produced these same sizes, and its debug APK is byte-identical to the verified `82945d9` delivery APK with the SHA-256 above. Only test-fixture source changed after the production checkpoint.

The verified `69fc8ed` local artifacts were 14,515,873 bytes for the debug APK, 2,862,884 bytes for the unsigned release APK and 5,714,937 bytes for the release AAB. The release APK grew 16,384 bytes from `fcd9ecd`; no production dependency was added.

The dedicated Firefox artifact contains three passing cases: domain blocking (3.549 seconds), URL keywords/live updates/already-open rebind (6.632 seconds), and selective `/shorts` → `/watch` navigation (1.810 seconds). The negative checks verify the actual native address, `ALLOWED` service outcome and visible browser UI; they do not merely assert that a matching overlay is absent. The fixture ran official Firefox 157.0.1 on API 35. Preflight verified the real address control after its observed Continue and Dismiss controls, with zero Quickstep ANR recoveries. Instrumentation required no delayed-hint dismissal; no ANR or fatal application crash was recorded. This is controlled browser-fixture evidence, not real YouTube video playback or physical-device acceptance.

The required full local command is:

```sh
./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease --stacktrace --max-workers=2
```

Connected Android verification uses `./gradlew :app:connectedDebugAndroidTest --stacktrace --max-workers=2`; the dedicated browser job selects `BrowserDiagnosticsTest` after preparing the official Firefox fixture. A skipped browser prerequisite is not a browser pass.

## Diagnostics and limits

The service exposes permission versus connection state, foreground package, browser package, readable-address state, last action and reason. Address failures distinguish unsupported browser, foreign window, missing field, active editing, unreadable address, contradictory address controls and Android tree-query failure. A visible domain with hidden path/query produces an explicit partial-coverage warning. A reported verified block requires a successful overlay attachment; an accepted Home request is a separate outcome. The service does not claim successful enforcement merely because a rule matches.

Known address adapters are Chrome, Chrome Beta, Brave, Edge, Firefox and Samsung Internet. This is an adapter list, not a guarantee across versions or modes. This repair's connected browser fixture exercises Firefox only. Private tabs, collapsed/hidden address bars, a changed browser UI, another accessibility overlay and unavailable window metadata can prevent reliable full-URL observation. When a previously blocked path remains covered because the browser exposes only its domain, the status explains why; a verified detailed allowed address or leaving the browser releases that block.

This implementation is an Accessibility-based foreground control, not a VPN, DNS filter, network firewall or browser extension. Android and the user control Accessibility availability; revocation, force-stop, reboot reconnection and OEM battery restrictions affect enforcement. Website budgets use browser usage because Android does not expose per-site usage totals. A group that also targets the browser as an app, or another active blocking group, can intentionally block ordinary pages despite a selective path rule. Existing domain-only saved rules remain domain rules; an old rule already reduced to `youtube.com` cannot be inferred to have originally meant `/shorts` and must be edited explicitly.

Before physical acceptance, verify app launches from EmmanueLA, Recents, notifications and another launcher; already-open app blocking; service disable/re-enable; rule changes while a browser stays open; real YouTube `/shorts` → ordinary `/watch` transitions; the chosen browser's private/hidden-bar behavior; limits, schedule and Break boundaries; permission revocation, restart/unlock and OEM battery restrictions. A complete automated run does not close these physical acceptance checks.

## Focused changed-file inventory

These 21 paths are the focused changes relative to `fcd9ecd`, including documentation. The inventory was checked against `git diff --name-only fcd9ecd` plus this newly added report.

```text
README.md
app/build.gradle.kts
app/src/androidTest/java/com/emmanuela/launcher/BrowserDiagnosticsTest.kt
app/src/androidTest/java/com/emmanuela/launcher/FocusWebsiteEditorTest.kt
app/src/androidTest/java/com/emmanuela/launcher/LauncherFlowTest.kt
app/src/main/java/com/emmanuela/launcher/LauncherViewModel.kt
app/src/main/java/com/emmanuela/launcher/data/FocusWebsiteRules.kt
app/src/main/java/com/emmanuela/launcher/data/FocusWindows.kt
app/src/main/java/com/emmanuela/launcher/platform/BrowserAdapters.kt
app/src/main/java/com/emmanuela/launcher/platform/RuleEnforcementService.kt
app/src/main/java/com/emmanuela/launcher/ui/mindful/FocusEnforcementStatus.kt
app/src/main/java/com/emmanuela/launcher/ui/mindful/FocusUi.kt
app/src/main/java/com/emmanuela/launcher/ui/settings/StructuredSettings.kt
app/src/main/res/xml/rule_enforcement.xml
app/src/test/java/com/emmanuela/launcher/BrowserAdapterTest.kt
app/src/test/java/com/emmanuela/launcher/FocusUrlKeywordTest.kt
app/src/test/java/com/emmanuela/launcher/FocusWebsiteRulesTest.kt
docs/BROWSER_COMPATIBILITY.md
docs/DEVICE_REPAIR_IMPLEMENTATION.md
docs/DEVICE_REPAIR_VALIDATION.md
docs/LIVE_MOMENT_URL_REPAIR.md
```
