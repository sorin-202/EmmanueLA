# Physical-device repair implementation inventory

This inventory describes source changes from baseline `b3de953` through checkpoint `02c3d60`, including the quick AppDetails Digest integration, caret and digest-publication repairs, widget typography, the native Home navigation correction, official-browser test setup, browser diagnostic transition repair, and continuous search-action persistence observation in instrumentation. It also includes the current reporting additions. It was written after inspecting the implementation, baseline diffs, and test definitions. The physical-device observations in the request remain the acceptance requirements.

This document records implementation evidence, not completion certification. Test names below identify assertions present in the repository; they do not assert that those tests ran successfully. Build, JVM, Android instrumentation, CI, and physical-device results belong in the separate `DEVICE_REPAIR_VALIDATION.md` report.

## 1. Home: direct widget dragging

**Root cause.** The normal Home path attached `homeClick`; pointer movement was implemented only when `arranging` was true. Holding an ordinary widget could invoke Home's hold action instead of acquiring that widget for movement. Independent tap and held-drag ownership also needed reconciliation.

**Correction.** `HomeItemDrag.kt` gives each Home item one gesture owner. A tap keeps the existing `HomeTapRouter` path. Movement before the long-press timeout, competing consumption, and additional pointers cannot acquire placement. A held press acquires movement, cancels the pending tap, produces haptic feedback, and displays an outline/shadow. `HomeWidgets.kt` keeps the starting position and transient movement in Compose state, constrains movement to measured bounds, and saves a changed final snapped position only on drop. Cancellation and pointer-input disposal restore the origin. Holding and releasing without movement opens the item's existing options; the arrangement screen remains available for its existing controls.

**Test definitions.** `HomePlacementTest` covers snapping, bounds, and invalid grid values. Android `HomeDragTest` injects long-press → drag → release and long-press → drag → `ACTION_CANCEL`, checks that movement does not emit persisted changes, checks recreation, and asserts the configured widget tap action still runs.

**Additional Home access defect.** Native widget-style navigation exposed Home's **Settings** and **+ App** controls beneath Android's navigation bar. `MainActivity` enables edge-to-edge drawing, but Home's controls column omitted navigation-bar insets. CI logcat recorded `LAUNCHER_TASKBAR_HOME_BUTTON_TAP`, followed by AOSP Quickstep taking the foreground, when the test touched Settings. Earlier accessibility `ACTION_CLICK` calls could invoke that covered control without detecting the physical obstruction. Checkpoint `963f87e` adds `navigationBarsPadding()` only to the existing Home controls column. The separate widget/shortcut placement layer, saved coordinates, and placement bounds remain unchanged. `LauncherFlowTest.fixedNavigationControlsStayAboveSystemNavigation` now checks Settings and +App bounds against actual platform navigation insets, opens Settings with a native touch and verifies its destination, and retains the original Apps/Folders navigation checks. The native bounds/navigation regression passed at `963f87e` and again in the complete `a52c86f` general API35 suite.

## 2. Home: independent widget styles and safe import

**Root cause.** The existing per-widget style model was incomplete, had no declarative import or meaningful preset collection, and widget-list navigation bypassed the main options page. Preview typography did not apply the same Home override as the actual widget. Later source review found that widget scaling omitted line-height scaling and that clock preview disabled the configured seconds format.

**Correction.** Each of the six built-ins—clock, date, battery, usage, weather, and location—has its own `widgetStyles[id]` value. The main widget options now expose **Style**. `SurfaceStyles.kt`, `WidgetStyleControls.kt`, and the shared Home rendering helpers support presets, font family, weight, italic, size, alignment, foreground/background color, background opacity, border, corners, padding, letter spacing, and optional width. Preview and Home use the same frame/background/padding/text helpers and inherited Home typography. Font size and line height scale together so enlarged wrapped text retains line spacing; clock preview preserves the configured seconds format. Typography controls apply to digital clock formats; the Hands clock also uses the applicable color/background/border/spacing controls.

`WidgetStyleImport.kt` accepts only the documented `emmanuela.widget-style` version-1 JSON document for the selected widget. It bounds input to 32 KiB, decodes strict UTF-8, rejects duplicate keys, non-JSON syntax, unknown properties, wrong types, nested payloads, and invalid ranges. The Android document picker reads appearance data; import shows a candidate preview and requires **Apply style** before persistence. The format cannot supply code, intents, paths, resource names, or URLs. Import adds no network request or executable-code facility. See [WIDGET_STYLE_SCHEMA.md](WIDGET_STYLE_SCHEMA.md).

**Test definitions.** `WidgetStyleImportTest` covers all whitelisted fields, independence and persistence, defaults/migration, presets, malformed/oversized input, unknown or executable-looking properties, schema/version/widget mismatch, duplicates, and invalid types/ranges. Android `WidgetStyleFlowTest` covers import preview before Apply, persistence of only the selected widget, and matching preview/Home dimensions under a custom Home scale. The parity method additionally asserts readable enlarged multiline line spacing and the configured seconds text in both views.

CI run `38969896800` failed before those style assertions were reached. The outgoing Home surface also described itself as “Home”, and retained accessibility nodes could change during navigation. Checkpoint `dcb5a6d` selects actual settings-row text in the target package, waits for stable current control bounds within the original deadline, and injects a real touch. The following CI run then exposed the production navigation-bar obstruction described above; `963f87e` corrects that obstruction. The import fixture also resolves the full clickable button after requesting it on-screen, verifies its native touch center against system-bar insets, and records the exact Android document request/result. At `a52c86f`, both complete style methods passed: document-contract invocation, preview, Cancel, Apply, selected-widget persistence/recreation, and preview/Home parity. Behavior assertions and timeout limits remain unchanged; real providers and physical displays still require acceptance.

## 3. Home: installed application icons

**Root cause.** Home had favorites and widgets, but no separate persisted activity-shortcut collection rendered in its existing placement surface.

**Correction.** `HomeShortcut.kt` defines bounded, validated launcher-activity references and dp positions. `V2Preferences.kt` persists these alongside widget positions; older configurations default to no Home shortcuts. **+ App** on Home opens the installed-app picker. `HomeShortcuts.kt` renders the application's existing icon/label within Home's current bounds and uses the same held-drag helper, completed-drop persistence, and cancellation behavior as widgets. Holding without movement opens **Remove from Home**, which only removes the saved shortcut. Unavailable activities have a removable placeholder. Hidden apps are neither offered nor rendered; private, protected-folder, and blocked apps retain the existing authenticated/policy-checked launch callback. The picker and persisted list are bounded to 64 shortcuts.

**Test definitions.** `HomeShortcutTest` covers configuration round trips, old defaults, hidden/private selection, default placement, duplicates, invalid activity references, nonfinite coordinates, and count limits. Android `HomeShortcutFlowTest` covers adding, movement/cancellation, recreation, blocked launch, removal without uninstall, launching an installed activity, and hidden-app exclusion.

## 4. App List: `/query` submission and web history

**Root cause.** `CursorSearchField.kt` supplied no Search keyboard action. `AppsScreen.kt` exposed only a separate “Search the web” result button, with no history repository.

CI subsequently exposed a separate runtime caret defect: input/selection could advance before a new `TextLayoutResult` arrived, causing `getCursorRect` to receive an offset beyond the cached layout. Caret drawing now snapshots the current field value, skips stale layout text, and clamps offsets against the measured layout's own text length. Typing, IME handling, selection, and configured caret rendering retain their existing paths.

Source review also identified that synchronizing an external query change through `TextFieldValue.copy(text=…, selection=…)` retained the previous IME composition. Checkpoint `11f7827` explicitly sets `composition = null` when adopting an external query change. This source correction is not a confirmed explanation for the Clear regression: the fourth CI run still failed with the old query text present after Clear. Checkpoint `26ddd2e` adds timestamped replacement, input-change, submission and Clear traces to distinguish a missed touch from a later callback restoring the query. AndroidX source inspection additionally confirmed that `EditableText` includes IME-owned composition underline annotations; a plain `AnnotatedString` equality check can fail while the exact characters match. Checkpoint `96a5e35` compares both raw `InputText.text` and displayed `EditableText.text` exactly, retains zero-selection and submission checks, and adds collapsed-selection checks after replacement. It still rejects any old text after Clear. The full rapid replacement/selection/Clear/Search/custom-caret regression passed in the `a52c86f` API35 suite. No deadline was increased; the composition correction alone is not claimed to explain every historical failure.

**Correction.** IME Search and hardware Enter submit the current slash-prefixed query through the normal `LauncherViewModel.launchPlatform` policy gate. The redundant result is replaced by five local recent searches, newest first; whitespace/case variants are deduplicated, tapping an entry executes it, and **Clear history** persists removal. A private DataStore stores history and the selected Android-default/DuckDuckGo/Google/Bing engine. Factory reset clears both; clearing history retains the engine. Query normalization, bounded storage, safe URL encoding, I/O error handling, and package resolution off the UI thread are explicit code paths. Merely displaying history performs no search or network request.

Android-default mode prefers a concrete `ACTION_WEB_SEARCH` handler and otherwise sends a DuckDuckGo HTTPS search to the default browser. Explicit engines use that browser. Android does not expose a browser's internal engine preference; the app does not claim to discover it. See [SEARCH.md](SEARCH.md).

**Test definitions.** `WebSearchHistoryTest` covers slash parsing, five-item order/deduplication, corrupt/invalid storage, and known engine choices. Android `WebSearchFlowTest` covers IME and hardware submission, history actions, DataStore recreation/reset, URL encoding, and engine choice without execution. Its caret regression exercises immediate long/short/Unicode replacement, noncollapsed selection, clear, Search, and visible custom-caret recovery. `WebSearchExecutionTest` exercises real AllApps input → Enter → persisted history → ViewModel → intercepted Android `startActivity`; it requires a configured HTTPS browser. Existing `LauncherFlowTest` assertions now inspect the history list and persisted clearing.

**Search-action persistence observation.** The documentation-head `3ca9e84` regression run timed out in the existing `configuredSearchActionRequiresTapAndCanBeRemoved` test. Its old error did not identify which saved-state wait failed, and there was no application crash or ANR. That test started new cold DataStore collectors concurrently with writes, a susceptibility already addressed by continuous observers in neighboring persistence tests. Checkpoint `02c3d60` starts one observer before interaction, retains each original 15-second deadline, and requires the exact removed/added action sets. It adds stage, observed-state and native-window diagnostics on failure, plus independent repository and activity-recreation assertions. This corrects the test's observation path and strengthens persistence checks; the preceding timeout alone does not prove the collector race was its sole cause. Runtime results are recorded separately in the validation report.

## 5. Compact folder search results

**Root cause.** `TreeBranchList` automatically expanded child rows whenever the query was nonblank, producing a verbose folder/app tree during search.

**Correction.** Empty-query browsing retains the tree. Nonblank searches use the existing indexed app list plus `CompactFolderSearchResult`: a small folder icon, name, and folder/protection indicator, without rendering that folder's children as a branch. Folder opening delegates to the existing `openFolder` authentication path; app results keep their launch checks.

**Test definitions.** `WebSearchFlowTest` asserts the compact protected-folder callback and absence of child content. `LauncherFlowTest.folderSearchRequiresPasswordBeforeOpening` retains the real password-before-opening assertions with the new result description.

## 6. Per-folder color, RGB, and image appearance

**Root cause.** Folder editing exposed palette colors and global fill behavior, without per-folder image persistence, arbitrary validated RGB entry, or the required image decode/access lifecycle.

**Correction.** `AppFolder` now persists `solidBackground` and a validated local `imageUri` alongside its color. `FolderAppearanceEditor.kt` provides a live draft preview, individual 0–255 RGB fields, and Android `OpenDocument` image selection. Successful selection retains read access after validating decodability. `FolderSurface` is shared by previews, folder cards, drag presentations, and the opened folder. Images are center cropped for display and shaded for readable text; source documents are not rewritten.

Decoding checks known file sizes against 40 MiB, bounds source dimensions to 100 megapixels, limits output to 1024 pixels per edge, and uses a 12 MiB shared bitmap cache. The older Android decoder handles EXIF orientation. Access is checked even for cached images, and resume retries permit a fallback after deletion/revocation. `FolderImageGrants.kt` releases abandoned/replaced/deleted-folder grants only when no saved folder, wallpaper, or wallpaper album still references the document. Portable exports omit device-specific folder image URIs. See [folder-appearance-and-size.md](folder-appearance-and-size.md).

**Test definitions.** `FolderAppearanceTest` covers RGB validation, per-folder round trips, local-URI restrictions, shared-grant references, decode bounds, and invalid dimensions. `ConfigurationTest` adds portable folder appearance/grid coverage with device-bound image references removed. Android `FolderAppearancePersistenceTest` covers repository recreation, actual image decoding, deletion, invalid images, and revoked-provider fallback. `FolderUiTest` checks per-folder solid-color rendering independently of global fill.

## 7. Custom folder grid dimensions

**Root cause.** Folder canvas spans were capped at four, and opened-folder content lacked separately persisted, user-configurable rows/columns.

**Correction.** `gridColumns` and `gridRows` persist 1–8 choices independently for each folder and default older records to 4×4. The editor exposes these dimensions and selects Grid layout when changed. `FolderContentGrid` reduces rendered columns when needed to retain at least 56 dp cell widths; row cells stay within 88–144 dp, with scrolling for additional apps. Rendering/resizing does not reorder membership. Canvas spans also accept up to eight cells, constrain to available columns, and use the existing packing/order model.

**Test definitions.** `FolderGridDimensionsTest` covers legacy 4×4 migration/order, independent 5×4/5×5 persistence, spans above four, nonoverlapping packing, screen adaptation, and invalid limits. Android `FolderUiTest` asserts five-column rendering, scrolling, and adaptation without changing app order; folder persistence tests include custom dimensions.

## 8. Live the Moment: app enforcement outside the launcher

**Root cause.** The service depended on a new window-state event to discover the foreground package. Connection, enabling a rule, or restarting while an app was already open could leave nothing to evaluate. Permission changes, rule/session transitions, and overlay lifecycle also needed repair.

**Correction.** `RuleEnforcementService.kt` reconciles actual focused/active application windows on service connection, rule enable, window changes, and unlock. It can inspect the application beneath its own Accessibility overlay. Usage Access has an AppOps change callback. Configuration changes invalidate pending evaluations; disconnect/destroy cleanup removes overlays and cancels jobs. Persisted configuration supplies app policies and focus groups after a new binding.

The service evaluates Strict Block, app/group usage and session limits, schedules, Break, pause/intention requirements, warnings, and existing grace budgets through the shared policy/FocusWindows logic. `FocusTargets.kt` centralizes package/site targeting and mixed-group usage. Per-group monotonic session anchors avoid double-counting transitions; a single next-boundary timer schedules reevaluation instead of a foreground polling loop. The launcher admission handoff prevents counting a launch twice. `BackgroundRuleAccess` and `FocusEnforcementStatus` show Android permission separately from current service connection, expose settings, and report enforcement outcomes.

**Test definitions.** `FocusTargetsTest` covers mixed accounting, rule changes, schedules, Break, and configured-browser targeting. Existing Focus allowance, Break, session, and deadline suites exercise the reused semantics. Android `FocusServiceTest` defines a separate-APK fixture scenario for an already-open app, live rule/Break/schedule changes, Usage Access revocation/restoration, Accessibility revoke/rebind, and external task launches.

## 9. Live the Moment: URL extraction and enforcement

**Root cause.** The old reader relied on legacy address resource IDs and text values; it missed Firefox display descriptions and Compose toolbar tags. Address-state ambiguity, ownership, normalization, and enforcement-result diagnostics were incomplete.

**Correction.** The event path reads only configured browsers' address controls, checks owning package/visibility, and waits while a focused address editor is still being typed. Adapters cover named Chrome/Chrome Beta, Brave, Edge, Firefox, and Samsung Internet controls. Firefox's `ADDRESSBAR_URL_BOX` fallback performs a bounded scan of chrome metadata and skips WebView/GeckoView page content. It accepts one unambiguous address token. `FocusWindows.host` normalizes HTTP(S) domains, IDNs, case and trailing dots, rejects credentials/unsafe forms, and preserves domain/subdomain boundaries; keyword matching also handles encoded URL text.

Distinct diagnostics identify unsupported browsers, foreign windows, missing controls, editing, unreadable addresses, and tree-query failures. The service reports an overlay only after WindowManager accepts it; successful Home API calls are reported as requests, and failures remain visible. It does not store or transmit visited URLs/page contents. See [BROWSER_COMPATIBILITY.md](BROWSER_COMPATIBILITY.md).

**Browser diagnostic transition defect.** The official Firefox test exposed an additional race after successful blocking. Foreground reconciliation and address extraction query application roots independently on IO. During a switch back to Home, an address read could receive the launcher's root before the foreground job invalidated the browser generation. An already-attached overlay could also obscure the application root. The old code published an unreadable-browser diagnostic before its existing overlay guard retained the matched URL. Checkpoint `54e74dd` captures the sampled root owner with the address result, discards a different application's root and requests foreground reconciliation, and moves the verified-overlay guard before diagnostic publication. An attached overlay enforcing a known URL now retains the last browser result when its toolbar is obscured. Editing remains diagnosed; unavailable address states without that verified blocking overlay still have explicit diagnostics. The Android test's original assertions and timeouts are unchanged.

**Test definitions.** Extended `BrowserAdapterTest` and `FocusTargetsTest` cover extraction/normalization/failure cases. `FocusServiceTest` reads real Android address-control fixtures through Accessibility. `BrowserDiagnosticsTest` requires a supported installed browser and asserts a readable navigated address and actual blocking overlay; absent prerequisites are not successful browser verification.

**Official-browser CI fixture.** Checkpoint `7368025` adds a separate API35/x86_64 KVM `browser-tests` job in `.github/workflows/android.yml`. It downloads Mozilla's official Firefox 157.0.1 APK and verifies SHA-256 `a6f397305508603cb0548dca8c321c412bc6eda7765f417fd142f61c9918e29c` before installation; no browser APK is bundled into EmmanueLA. `.github/scripts/prepare-firefox-fixture.py` verifies the installed version and default HTTPS resolver, advances only observed enabled browser controls, and requires an unedited `example.com` address control. The first execution tapped the observed **Continue** onboarding button, then stalled on Firefox's “Shake your device to summarize this page in seconds” hint with an accessible **Dismiss** button. Checkpoint `26ddd2e` adds that exact observed dismissal to the bounded setup path. Screenshots, UI XML, command transcripts and setup results are uploaded as artifacts.

Browser setup does not enable EmmanueLA Accessibility, change Firefox preferences, alter the Android Home role, or establish enforcement. After setup succeeds, the job runs the actual `BrowserDiagnosticsTest.supportedBrowserNavigationActuallyDisplaysBlockingOverlay`: Android navigation → Accessibility address discovery → active domain rule → attached overlay → observable blocking text. In the `26ddd2e` execution, setup returned `verified: true` with Firefox's real `ADDRESSBAR_URL_BOX` and description `example.com/focus-fixture. Search or enter address`. The test then passed readable-address, attached-overlay, and observable blocking-text assertions. It failed later, after returning Home, because the retained `addressReadable` diagnostic changed from true to false. That is a failed test with demonstrated URL/overlay behavior, not a full passing browser result. Later runs distinguished fixture failures from application behavior: `a52c86f` failed before instrumentation on a Quickstep ANR, and `94e5368` completed setup but failed the address-readability wait. Checkpoint `392cd53` adds failure-only bounded browser-chrome/window diagnostics, snapshots service status after the original wait, and preserves the original test exit status when capturing a failure screenshot. Its dedicated KVM browser job passed the complete test in run `39049201457`, including address extraction, attached/visible blocking overlay and retained diagnostics after returning Home. See the validation report for the exact artifact and browser/version evidence. The general regression emulator has no supported-browser fixture and reports that missing prerequisite separately; setup failure or a skipped prerequisite is never counted as browser verification.

**Delayed first-run browser hint.** The independent `1529d54` browser run exposed a late Firefox recommendation popup after the external setup had already verified the address bar. Failure-time chrome contained only Firefox's native `cfr.dismiss` button and hint; the address control was absent from the exposed popup root. A screenshot confirmed the “Shake your device to summarize this page in seconds” first-run tip. The service reported the unavailable address rather than claiming a block. Checkpoint `15ea8b2` finishes this exact fixture prerequisite inside the existing 20-second address-readability wait: it requires the observed Firefox-owned native hint text/button metadata, accepts Android ACTION_CLICK once, logs the dismissal, and then requires every original address/overlay/visible-reason/Home-retention assertion. It skips web page content and does not click browser controls in production. Other or repeated prompts are not automatically dismissed. The corrected run `39072838524` passed the complete browser test with this late-hint dismissal actually exercised once. This corrects setup readiness; it does not claim that every obscured browser state supports URL enforcement.

**Compile before booting the fixtures.** Checkpoint `a52c86f` builds both debug APKs with `:app:assembleDebug :app:assembleDebugAndroidTest` before starting either CI emulator. The regression and Firefox jobs then run their existing connected-test commands against those APKs. This avoids competing APK compilation and guest startup on the same CI runner; it does not increase assertion deadlines, disable animations, or convert setup failures into skipped tests. Local software-only emulation produced Android framework ANRs and could not certify browser enforcement; the dedicated KVM job supplies the actual-browser test environment.

## 10. Notification digester

**Root cause.** Cancellation was requested before durable capture, and counts were recorded without Android confirming dismissal. This could lose capture or count an unsuccessful action. Capture/rule races, temporary-suppression expiry, interval defaults, retention/review access, and Digest activation through alternate settings paths were incomplete. CI also exposed acknowledgement after a notification-post request without proof of publication; source review found that an older publication snapshot could acknowledge a newly recreated package group after clearing.

**Correction.** `NotificationFiltering.kt` loads rules from persistence in the listener, refreshes active notifications after connection/configuration changes, requests rebind after disconnect, and schedules reevaluation at rule/suppression/time boundaries. Capture persists bounded pending metadata first, rechecks the current rule, and requests cancellation only if it still applies. Only `REASON_LISTENER_CANCEL` turns that capture into a confirmed digest count. Replay identities are hashed and deduplicated; unreadable storage/permission failures have diagnostics. Rule evaluation includes per-app delivery, schedules/Strict Block scope, keywords, and temporary suppression.

The digest persists counts/reasons rather than message contents or notification actions. Seven-day group retention is enforced on reads/maintenance/capture. The screen exposes counts, reasons, opening the source app through launch-policy checks, per-app/all clearing, and future allowance. Summary work is scheduled from the global filter/digest setting, with inherited versus explicit per-app intervals preserved. `DigestWorker` checks runtime permission, effective posting AppOps and channel state, tags the exact due snapshot with a batch token, and acknowledges it only after finding the matching active Android notification. A listener publication callback supports that confirmation; unavailable publication leaves counts pending for retry. Acknowledgement also requires the snapshot's `retainedAt` to match the current package group, preventing an older worker from announcing counts created after clearing. Selecting Digest in the app policy editor or quick `AppDetails.kt` control enables summaries. Important notification categories and required ongoing/foreground-service notifications are excluded before dismissal. See [NOTIFICATION_FILTERING.md](NOTIFICATION_FILTERING.md).

**Test definitions.** Extended `NotificationRulesTest` covers intervals/default migration in addition to schedule/keyword/suppression semantics. `NotificationListenerTest` defines real-listener capture, filtering, important-category preservation, live rule changes, revoke/rebind, confirmed-dismissal accounting, replay protection, and repository recreation. Its additional generation regression clears and recreates a package group, attempts acknowledgement from the old snapshot, and asserts that the replacement remains pending until its own snapshot is acknowledged. `NotificationDigestWorkerTest` asserts a real summary notification and retention of unannounced counts when effective posting permission is denied. `NotificationDetailsFlowTest` exercises the quick AppDetails selector through persisted master/per-app settings and a real listener notification capture/dismissal.

## 11. Local notification spam mode

**Root cause.** The application had delivery rules but no local spam classifier, spam settings, or persisted explanations for such decisions.

**Correction.** `NotificationSpam.kt` adds bounded per-source signal history and local rules: duplicate/similar normalized text hashes, frequency windows, repeated promotional patterns, and user keywords. Global Off/Conservative/Stronger defaults and per-app overrides persist through `ExperiencePreferences`/`NotificationRule`; app allowlisting and allow words bypass the classifier. Refresh/reconnect replays do not count as fresh arrival bursts. Signal history is memory-only; settings and confirmed count/reason metadata persist. No LLM, cloud request, or new production classifier dependency is used.

Nonclearable/ongoing/foreground-service, group-summary, call, alarm, media, navigation, service and system notifications are preserved before classification; recognized security/authentication wording and possible short codes add another guard. Spam classifications use the confirmed-digest path, remain reviewable even when timed summaries are off, and include visible reasons. **Allow future notifications** resets delivery to Normal, ends suppression, and allowlists the app for spam; it does not change app-launch restrictions.

**Test definitions.** `NotificationSpamTest` covers modes, repeated/similar/frequent promotions, independent source windows, configuration/reconnect replay, keywords/allow words/essential guards, persistence/defaults, and invalid settings. `NotificationListenerTest` includes global spam, per-app Off/allowlist, persisted reasons, and authentication/navigation preservation.

## Platform boundaries and remaining acceptance work

- Accessibility and Notification Access are optional Android-controlled permissions. Revocation, force-stop, lock state, reboot/unlock rebinding, manufacturer battery policies, and unavailable windows can interrupt service behavior. A persisted rule or enabled UI switch does not establish that Android is currently enforcing it.
- App enforcement targets the focused application window. System permission, lock-screen and known emergency controls remain reachable. Recents, notification/deep-link launches, split screen, reboot, battery restrictions, and real authentication flows still need device-specific observation.
- URL enforcement is an Accessibility overlay, not a network firewall. Browser versions, private modes and hidden/inaccessible toolbars can prevent extraction; unlisted browsers have no adapter. Android supplies no per-site usage totals, so website usage limits count browser usage. Selective Shorts/Reels/feed removal is unsupported.
- Document providers own document availability and grant behavior. Real provider selection, rotation during editing, restarts, deletion/revocation, large images/fonts, narrow screens and preview/Home visual parity need physical-device checks.
- Notification filtering happens after posting and cannot undo a sound/heads-up alert already produced. WorkManager has a 15-minute minimum periodic interval and approximate delivery. Android may redact content. Incorrectly categorized or unrecognized critical messages cannot be identified perfectly; essential-app allowlisting remains available.
- Android offers no general listener API to restore an original cancelled notification with its source identity and actions. The implementation retains counts/reasons, offers the source-app path, and allows future notifications; it does not promise restoration of originals. Stronger spam heuristics can produce false positives and require real-app tuning.

## Test/build support and compatibility changes

Compose UI-test and WorkManager test dependencies support the new Android scenarios; the Compose test activity is debug-only. Android-test manifest/resource fixtures support foreground and address-control tests. The production manifest adds HTTPS browser package visibility. Accessibility event subscriptions now include window changes, view text/focus changes, view IDs, and interactive windows. No new production dependency was added for styles, history, folder appearance, or spam classification. The Gradle wrapper executable-bit correction supports the requested `./gradlew` commands.

The browser preflight also records a single narrowly scoped recovery for the exact observed Android Quickstep ANR: it can tap Android's enabled `android:id/aerr_wait` once, captures the dialog, and emits a CI warning. Other apps' ANRs, repeated Quickstep ANRs, and disabled or ambiguous controls fail. It does not close or disable a system package, suppress dialogs, change application permissions, or extend the existing 180-second bound. Five Python host tests check those boundaries; fixture success still requires Firefox's real unedited example address and the separate Android enforcement test.

The Android gesture/navigation fixtures now sample current rendered controls and settled post-gesture geometry instead of comparing stale bounds or appending text to an existing picker query. Widget style navigation distinguishes actual settings rows from the outgoing Home surface and reacquires stable bounds before touching them; native touches exposed and now guard against Home controls covered by Android navigation. These test definitions and synchronization changes do not certify runtime behavior; execution results remain in the validation report.

## Baseline-relative changed-file inventory

The 86 paths below include implementation, tests, test support, CI workflow/browser setup and documentation relative to `b3de953`. The inventory is regenerated from Git's baseline-relative changed paths. It is a source snapshot; later validation/report-only commits are identified separately in the validation report.

```text
.github/scripts/prepare-firefox-fixture.py
.github/scripts/test_prepare_firefox_fixture.py
.github/workflows/android.yml
README.md
app/build.gradle.kts
app/src/androidTest/AndroidManifest.xml
app/src/androidTest/java/com/emmanuela/launcher/BrowserDiagnosticsTest.kt
app/src/androidTest/java/com/emmanuela/launcher/FocusFixtureActivity.kt
app/src/androidTest/java/com/emmanuela/launcher/FocusServiceTest.kt
app/src/androidTest/java/com/emmanuela/launcher/FolderAppearancePersistenceTest.kt
app/src/androidTest/java/com/emmanuela/launcher/FolderUiTest.kt
app/src/androidTest/java/com/emmanuela/launcher/HomeDragTest.kt
app/src/androidTest/java/com/emmanuela/launcher/HomeShortcutFlowTest.kt
app/src/androidTest/java/com/emmanuela/launcher/LauncherFlowTest.kt
app/src/androidTest/java/com/emmanuela/launcher/NotificationDetailsFlowTest.kt
app/src/androidTest/java/com/emmanuela/launcher/NotificationDigestWorkerTest.kt
app/src/androidTest/java/com/emmanuela/launcher/NotificationListenerTest.kt
app/src/androidTest/java/com/emmanuela/launcher/WebSearchExecutionTest.kt
app/src/androidTest/java/com/emmanuela/launcher/WebSearchFlowTest.kt
app/src/androidTest/java/com/emmanuela/launcher/WidgetStyleFlowTest.kt
app/src/androidTest/res/values/focus_fixture_ids.xml
app/src/main/AndroidManifest.xml
app/src/main/java/com/emmanuela/launcher/LauncherViewModel.kt
app/src/main/java/com/emmanuela/launcher/data/Configuration.kt
app/src/main/java/com/emmanuela/launcher/data/ExperiencePreferences.kt
app/src/main/java/com/emmanuela/launcher/data/FocusTargets.kt
app/src/main/java/com/emmanuela/launcher/data/FocusWindows.kt
app/src/main/java/com/emmanuela/launcher/data/FolderAppearance.kt
app/src/main/java/com/emmanuela/launcher/data/FolderGridLayout.kt
app/src/main/java/com/emmanuela/launcher/data/FolderImageGrants.kt
app/src/main/java/com/emmanuela/launcher/data/Folders.kt
app/src/main/java/com/emmanuela/launcher/data/HomePlacement.kt
app/src/main/java/com/emmanuela/launcher/data/HomeShortcut.kt
app/src/main/java/com/emmanuela/launcher/data/NotificationRules.kt
app/src/main/java/com/emmanuela/launcher/data/NotificationSpam.kt
app/src/main/java/com/emmanuela/launcher/data/SurfaceStyles.kt
app/src/main/java/com/emmanuela/launcher/data/V2Preferences.kt
app/src/main/java/com/emmanuela/launcher/data/WebSearchHistory.kt
app/src/main/java/com/emmanuela/launcher/data/WidgetStyleImport.kt
app/src/main/java/com/emmanuela/launcher/platform/BackgroundRuleAccess.kt
app/src/main/java/com/emmanuela/launcher/platform/BrowserAdapters.kt
app/src/main/java/com/emmanuela/launcher/platform/NotificationFiltering.kt
app/src/main/java/com/emmanuela/launcher/platform/RuleEnforcementService.kt
app/src/main/java/com/emmanuela/launcher/platform/WebSearch.kt
app/src/main/java/com/emmanuela/launcher/ui/apps/AppDetails.kt
app/src/main/java/com/emmanuela/launcher/ui/apps/AppManagementUi.kt
app/src/main/java/com/emmanuela/launcher/ui/apps/AppsScreen.kt
app/src/main/java/com/emmanuela/launcher/ui/apps/CursorSearchField.kt
app/src/main/java/com/emmanuela/launcher/ui/apps/WebSearchHistoryList.kt
app/src/main/java/com/emmanuela/launcher/ui/folders/DenseFolderCanvas.kt
app/src/main/java/com/emmanuela/launcher/ui/folders/Editors.kt
app/src/main/java/com/emmanuela/launcher/ui/folders/FolderAppearance.kt
app/src/main/java/com/emmanuela/launcher/ui/folders/FolderAppearanceEditor.kt
app/src/main/java/com/emmanuela/launcher/ui/folders/FolderGrid.kt
app/src/main/java/com/emmanuela/launcher/ui/folders/FoldersScreen.kt
app/src/main/java/com/emmanuela/launcher/ui/folders/MagneticFolderApps.kt
app/src/main/java/com/emmanuela/launcher/ui/home/HomeItemDrag.kt
app/src/main/java/com/emmanuela/launcher/ui/home/HomeScreen.kt
app/src/main/java/com/emmanuela/launcher/ui/home/HomeShortcuts.kt
app/src/main/java/com/emmanuela/launcher/ui/home/HomeWidgets.kt
app/src/main/java/com/emmanuela/launcher/ui/mindful/FocusEnforcementStatus.kt
app/src/main/java/com/emmanuela/launcher/ui/settings/NotificationRuleEditor.kt
app/src/main/java/com/emmanuela/launcher/ui/settings/StructuredSettings.kt
app/src/main/java/com/emmanuela/launcher/ui/settings/WidgetStyleControls.kt
app/src/main/res/xml/rule_enforcement.xml
app/src/test/java/com/emmanuela/launcher/BrowserAdapterTest.kt
app/src/test/java/com/emmanuela/launcher/ConfigurationTest.kt
app/src/test/java/com/emmanuela/launcher/FocusTargetsTest.kt
app/src/test/java/com/emmanuela/launcher/FolderAppearanceTest.kt
app/src/test/java/com/emmanuela/launcher/FolderGridDimensionsTest.kt
app/src/test/java/com/emmanuela/launcher/HomePlacementTest.kt
app/src/test/java/com/emmanuela/launcher/HomeShortcutTest.kt
app/src/test/java/com/emmanuela/launcher/NotificationRulesTest.kt
app/src/test/java/com/emmanuela/launcher/NotificationSpamTest.kt
app/src/test/java/com/emmanuela/launcher/WebSearchHistoryTest.kt
app/src/test/java/com/emmanuela/launcher/WidgetStyleImportTest.kt
docs/BROWSER_COMPATIBILITY.md
docs/DEVICE_REPAIR_IMPLEMENTATION.md
docs/DEVICE_REPAIR_VALIDATION.md
docs/HOME_INTERACTIONS.md
docs/NOTIFICATION_FILTERING.md
docs/PRIVACY.md
docs/SEARCH.md
docs/WIDGET_STYLE_SCHEMA.md
docs/folder-appearance-and-size.md
gradlew
```
