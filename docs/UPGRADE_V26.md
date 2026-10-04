# EmmanueLA 2.6.0-beta — targeted upgrade

This iteration extends 2.5.2. Application ID, persisted configuration store, existing visual palette and vector assets are retained. It does not reconstruct the launcher.

## Implemented changes

| Area | Implementation |
|---|---|
| Live the Moment | Daily minutes, daily opens and session minutes are independent budgets, usable together; per-app or shared group accounting; configurable session break and mindful delay; pause heading is `Look around`. |
| Time windows | Multiple selected-day Limit / Strict block windows; overnight intervals retain the starting weekday; Strict block takes precedence; equal start/end is a full-day window. Limits run all day if there are no Limit windows, otherwise only inside them. |
| Blocklists | Apps, domains/subdomains and URL keywords; browser selections use the installed catalogue and explicit address-bar adapters. |
| External launches | Optional Accessibility Service checks window changes, observes supported browser address nodes, displays an accessibility blocking overlay, and schedules one coroutine deadline for the next quota/schedule boundary. Launcher handoffs avoid a second opening count. Screen-off cancels pending work. |
| Tree Branch | App-list-only toggle; one folder row replaces its members; tap expands; search results open matching branches and show matching children; simplified add/remove/rename/delete dialog. Protected branches require unlocking. Alphabet section positions include expanded children. |
| Folder canvas | Width and height 1–4 cells, global defaults with per-folder overrides; dense freeform placement reserves complete rectangular spans and relocates collisions. Dragged tiles remain above other tiles and stay at the snapped position while saving. |
| Identity | Private Apps page entry and individual private launches authenticate through Android biometrics/device credentials. Protected folders allow device authentication alongside PIN, and can use device-only protection without a folder PIN. |
| Clock | Digital seconds and 12-hour AM/PM, raised compact minutes and an analog canvas containing only hour/minute hands. Second updates exist only while the clock is composed and seconds are enabled. |
| Notifications | Connected listener observes persisted rule changes and reprocesses active notifications. Settings expose NORMAL/MUTE/DIGEST per package and the Android summary permission. Access revocation is handled without a listener crash. |
| Contacts | Aggregated contact IDs deduplicate repeated phone records; normalized numbers deduplicate duplicate phone contacts; external integrations deduplicate by package and aggregate contact. Different available contact applications remain distinct actions. |

## State and execution

`ConfigurationRepository` remains the single persisted configuration source (DataStore JSON, schema 7 with optional fields). Legacy focus schedules and folder fields remain readable. Group windows, browser lists, cell spans and clock options use bounded decoding. Authentication grants and active folder unlocks remain in memory and are not exported.

UI observes state flows. Usage queries, preference counting and disk writes use IO coroutines. Window events drive enforcement; no continuously polling foreground service is introduced. The service schedules deadlines only while a relevant app is foregrounded. List section lookups use precomputed maps. Folder packing reserves occupied cells before drawing; the drag overlay is on the same canvas with higher z-order. Actual refresh-rate performance requires device measurement.

## Permissions and limitations

- Background enforcement is OFF by default. Enable it in Live the Moment and explicitly enable **EmmanueLA focus rules** in Android Accessibility settings. Grant Usage Access for daily/session tracking. Private apps/folders require an Android device lock.
- Without the optional service, rules govern launcher entry points and session reminders; they cannot intercept launches from other Android surfaces.
- Background enforcement is a user-controlled focus aid, not tamper-proof parental control. Android can stop services; disabling accessibility bypasses external enforcement. Android/OEM secure windows can interrupt overlays. Settings, system permission windows and System UI stay reachable.
- Web rules inspect only address-bar nodes exposed by the configured adapters (Chrome/Beta, Brave, Edge, Firefox, Samsung Internet). Browser versions, custom tabs, immersive/hidden address bars and OEM variants can prevent URL access. The UI lists adapters, not a guarantee that every version works. No HTTPS decryption, DNS/VPN filtering, page-content scanning or built-in adult-site database is implemented. URL keyword matching does not match encrypted page text.
- Web quotas currently account for foreground **browser** usage, not exact per-site time. Domain blocking itself uses the exposed current URL. No URLs or page content are saved.
- UsageStats timing is Android-supplied. Background overlays and OEM activity events can affect attribution. Time/session performance needs on-device verification.
- Notification listeners see already-posted notifications: dismissal cannot undo an initial sound/heads-up. Calls, alarms, ongoing media/services and group summaries are preserved. DIGEST requires the master filter and digest toggles; posting summaries also requires notification permission. WorkManager summaries can be deferred by Android.

## Verification status

`python3 tools/check_sources.py` passes: balanced Kotlin delimiters, valid XML, manifest components present. Archive integrity and source-byte comparison are checked separately at packaging.

`V26RulesTest` adds eight JVM regression tests for overnight boundaries, overlap precedence, hybrid budgets, domain matching, count keys, dense span collisions and codec round-trip. They are **provided but not executed** in this cloud environment.

The command below was attempted and failed before compilation because `services.gradle.org` could not resolve. See `build_attempt_v26.log`. There is no verified APK, successful lint result, device test result or FPS guarantee in this delivery.

## Exact local validation

1. Extract the ZIP and open its `EmmanueLA` folder in Android Studio. Use JDK 17; install Android SDK Platform 35. Allow Gradle/dependency downloads and Sync.
2. From that project folder on Windows, run:

   ```bat
   gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
   ```

   On macOS/Linux use `./gradlew` with the same tasks. Resolve any compiler/lint failures before installing. Reports: `app/build/reports/tests/testDebugUnitTest/`, `app/build/reports/lint-results-debug.html`.
3. Connect a device/emulator (API 26+), then run:

   ```bat
   gradlew.bat :app:installDebug :app:connectedDebugAndroidTest
   ```

4. Grant Usage Access, notification listener access, notification posting and **EmmanueLA focus rules** accessibility access as relevant. Enroll a biometric and device PIN for security tests. Test on both the minimum supported version and a recent API 35 device.
5. Configure one group with 30 daily minutes, 6 opens and 5 session minutes (shorter limits may speed testing). Check launcher, notification, recents and external deep-link launches; the sixth permitted open must succeed and the seventh must block. Stay foregrounded until a session/daily deadline and verify overlay enforcement. Return Home, wait the configured break and re-open. Verify aggregate versus per-app counting and daily reset.
6. Check Monday 22:00–Tuesday 07:00, overlapping Strict/Limit windows, all-day windows, entry/exit while an app remains foregrounded, screen-off and permission revocation. Check countdown cancellation and rapid app/browser switching do not admit a different app.
7. For each installed browser adapter, test a visible URL, subdomain, unrelated suffix domain, keyword and hidden-address-bar behavior. Do not consider browser support validated until that browser/version passes.
8. Check private page entry, private app launch and protected-folder member launch with biometrics, credential fallback, cancellation, backgrounding and a biometric-only folder. A cancelled prompt must never launch the app.
9. Toggle Tree Branch, expand/collapse, search member aliases/#tags/packages, long-press folder edits, locked branches and alphabet jumps. Duplicate folder membership uses the first configured folder as the drawer owner.
10. Drag mixed 1×1, 2×4 and 4×4 folders in a dense canvas, confirm z-order, snap, no overlap and persistence after restart; repeat with animations disabled and narrow split-screen.
11. Test all clock formats, seconds/AM-PM toggles, imported fonts and widget dragging. Check every notification mode on already-active and newly posted clearable notifications; verify calls/alarms survive and revoked access does not crash.
12. Search a contact with several numbers, duplicated accounts and an exposed third-party integration. Verify one phone row per aggregate contact and one row per actual external application. Measure animations/input with Android Studio Profiler or Perfetto on 60/120Hz hardware.
