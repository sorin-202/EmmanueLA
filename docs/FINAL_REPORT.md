# EmmanueLA — Final Engineering Report

Date: 2026-10-06. Authoritative implementation: `EmmanueLA_v2_6/EmmanueLA`. Original local baseline `6b676c5` preserved; no remote replacement.

## 1. Executive summary

**BETA READY**. The engineering continuation repaired compilation/lint failures, strengthened intentional search and focus rules, corrected drag/cancellation behavior, and replaced unsupported browser claims with visible capability diagnostics. Measured search work and release resource shrinking improve the existing application without removing languages, ABIs or features. Final validation evidence and its limits are below.

## 2. Feature audit: before → after

| Area | Delivered state | Verified outcome |
|---|---|---|
| Build | Three Kotlin compilation defects; no baseline APK could be produced | Clean JVM/lint/debug/release/AAB build succeeds |
| Search | Alphabetic/substring behavior and unrelated package matches; missing folder/action flows | Exact → prefix → word-prefix → substring; aliases/accents/tags, protected folders, explicit web search and opt-in actions; packages off by default |
| Home/navigation | Home alphabet could disable App List; gesture cancellation could navigate | Independent surfaces, existing identity retained, cancelled gestures do not dispatch |
| Folders/widgets | Grid scrolling cancelled reorder; cancelled widget movement could persist | Ownership until release/cancellation; four consecutive placements; one save per meaningful release, no movement-frame persistence |
| Focus | Incomplete continuous block/Break/allowances and deadline handling | Strict Block, controlled Break, overnight/DST boundaries, warning/grace and transient intentions |
| Notifications | Incomplete scheduling/keyword/lifecycle evidence | Local schedules/keywords/focus/temporary suppression; real listener and editor coverage |
| Browser rules | Unreliable address assumptions | Bounded package/root/URL checks and honest unsupported/unreadable diagnostics |
| Distribution | No measured release/AAB baseline | R8 plus resource shrinking; offline language selection preserved in AABs |

## 3. Root causes and fixes

- Delivered symbol/shadowing/import errors blocked compilation. Minimal corrections restored the existing application; lint errors were corrected rather than suppressed.
- Rule reevaluation missed deadline/time-change cases. Shared temporal evaluation now covers expiry, midnight, overlap and DST.
- LazyVerticalGrid claimed movement after folder long press. It yields scrolling only during active item drag and restores it on release/cancellation.
- Android ACTION_CANCEL appeared as consumed pointer-up. Widget and navigation handlers now reject cancelled completion; widgets restore the starting placement.
- Obscured Home controls remained in overlay semantics. Inactive surfaces now hide their semantics while the arrangement UI retains the intended controls.
- A repeated-drag test failure also exposed upstream DataStore 1.1.7 cold-collector race 431787506. Persistence was correct; continuous observers avoid the reproduced startup window. The dependency limitation remains documented, not claimed fixed globally.
- Release smoke found fixed App List/Folders footers beneath three-button navigation. Full safeDrawing insets replace top-only padding; an Android bounds/navigation test verifies the correction.
- AAB lint identified dynamic local language changes with default language splits. Splitting by language is disabled so offline language changes retain resources.

## 4. User-facing improvements

Search actions reuse existing gesture/Home action selection and secure dispatch. They require an explicit tap and suppress competing single-app auto-launch. Folder search retains authentication. Configuration v9 reads v2–v9 and validates imported bounds/IDs. Breaks are bounded and excluded from portable exports; intentions remain transient. Notification filters preserve calls, alarms, ongoing/media/service notifications. Quiet delivery uses Android channels because post-delivery listeners cannot undo an alert.

## 5. Architecture

The single-module Compose application, package identity and local DataStore format remain. Pure search/focus/configuration rules are separated from platform services and feature screens. Query/name tokens are prepared outside per-app matching. UI drag state remains transient until completion. LauncherViewModel is still a large coordinator; no speculative framework rewrite was undertaken. Existing service/worker compatibility rules are retained under R8.

## 6. Performance

API35 x86_64 emulator, 500 synthetic apps, five query types, warmup then 200 measured searches: median 22.1075→2.6123 ms; p95 52.6212→13.7897 ms; maximum380.5592→28.2069 ms. Both runs returned 31,200 matches; functional ranking/alias/package/hidden-app tests pass. These measure the search engine, not physical keyboard-to-screen latency or FPS.

Five process-cold debug starts with warm OS caches: baseline 6280/3371/3679/2883/3373 ms; optimized 4949/3398/2958/3153/2994 ms. Medians 3373/3153 ms; this small emulator sample does not prove causal startup improvement. PSS snapshots 96,405/98,674 KiB and RSS 222,420/224,952 KiB do not show a memory reduction. Host memory pressure and emulator System UI startup ANR limit interpretation. Physical frame rate and battery claims are not made. See repository `docs/PERFORMANCE.md` for reproducible methods and additional runtime observations.

Final minified release process-cold starts with warm OS caches: 1042/981/1190/998/1012 ms, median 1012 ms. A post-start memory snapshot showed PSS 34,948 KiB and RSS 180,932 KiB. Four short idle Home CPU samples were 0.0%, 0.0%, 0.5%, 0.0%; they do not establish long-term idle or battery behavior.

## 7. Privacy

No account, ads or analytics SDK was added. Settings/rules/contact matching/notification filtering stay local. Notification bodies, browser history and intention text are not persisted. Optional weather sends selected coordinates/location queries to Open-Meteo over HTTPS. Sensitive permissions have feature-specific explanations and denied-access behavior documented in `docs/PRIVACY.md`. App/folder authentication controls launcher entry; it does not encrypt or isolate other Android apps. Accessibility enforcement is optional and best-effort.

Permissions retained are tied to optional features: contacts, notifications/listener, accessibility, usage access, location, camera/torch, DND, biometrics and vibration, plus package visibility for launcher app enumeration. No speculative permission removal disabled a working feature. Backup remains disabled; optional network access serves weather/location lookup and user-requested web actions. Existing background services/workers are feature-driven; no analytics, new polling loop or background measurement service was introduced. The full manifest permission inventory and denial behavior are documented in repository `docs/PRIVACY.md`.

## 8. Artifact size

| Artifact (bytes) | Before M6 | Final clean |
|---|---:|---:|
| Debug APK | 14,748,754 | 14,286,186 |
| Unsigned universal release APK | 3,038,438 | 2,789,120 |
| Release AAB | 5,581,009 | 5,534,410 |

Release APK reduction: 249,318 bytes (8.2%). AAB reduction: 46,599 bytes (0.83%). Resource shrinking reduced compressed resources from 1,035,938 to 789,141 bytes; all four ABIs and language resources remain. Final artifact SHA256 hashes are recorded in `docs/PERFORMANCE.md` and were rechecked after the documentation checkpoint.

Before-M6 debug packaging was incremental, so its difference from clean output is not attributed entirely to code optimization. Release compressed DEX/resources dominate. Native libraries total 60,292 bytes across four ABIs; removing them was unjustified. AAB contains compressed R8 mapping metadata that is not application download code. APK values are artifact file sizes, not Play delivery estimates. Android Storage reports the final release as app 6.75 MB, user data 270 kB, cache 561 kB, total 7.59 MB (rounded platform values). Baseline installed size was not measured; no installed-size reduction is claimed.

## 9. Executed validation

Production code checkpoint `056f629`; later changes affect documentation/CI only. Final clean build PASSED in 8m55s, followed by the complete API35 suite PASSED in 2m22s:

```sh
./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:assembleDebugAndroidTest --max-workers=2 --console=plain
./gradlew :app:connectedDebugAndroidTest --max-workers=2 --console=plain
```

- JVM: **123 tests**, zero failures/errors/skips.
- Android integration/UI: **18 tests**, zero failures/errors/skips, including the installed official Firefox 157 fixture. XML totals independently rechecked.
- Lint: **0 errors, 43 warnings**; warnings retained, no suppression to obtain a pass.
- Debug APK, unsigned release APK, release AAB and Android test APK: **PASSED**.
- Minified release copy signed with the existing local development key: signature verification and non-debuggable check **PASSED**. Home/App List/Folders/Settings, explicit search and Romanian resource switching **PASSED**. Crash buffer empty.
- Actual AAB BundleConfig: language splitting disabled, confirming offline language packaging. Play-delivered installation itself is **NOT EXECUTED**.
- Artifact size/hash checks, workflow/template YAML parsing, current documentation links, whitespace review and absence of temporary production tracing: **PASSED**.

Local logs: `.artifacts/final-clean-build.log`, `.artifacts/final-android-validation.log`; release smoke XML/signature/crash/storage evidence and artifact measurements are also in `.artifacts/`. These generated local files are intentionally untracked; durable results are recorded in the repository's validation/performance documents. JDK 17, SDK 35, API35 x86_64 emulator, WHPX/host graphics; the host has approximately 7.4 GiB RAM. Heavy compilation and emulator runtime validation were separated to avoid resource pressure.

Earlier M6 fixture failures were diagnosed: Firefox continuous onboarding showed Android's default-browser role dialog; the test now dismisses only that identified system prompt. Break assertions now observe before interaction, retaining fresh persisted-state assertions. No production browser/focus workaround or test bypass was added. One clean run was intentionally stopped after visual evidence required the inset correction; another successful clean run preceded the language packaging correction. Only the final run supplies final artifacts.

## 10. Remaining limitations

- BLOCKED BY ENVIRONMENT: physical haptics, real biometric sensors, OEM lifecycle behavior, battery and 60/90/120Hz acceptance; no physical hardware is connected.
- Firefox 157 normal address source is UNSUPPORTED based on actual accessibility-tree/service evidence. Other browser/mode adapters are unvalidated; Opera and selective native Shorts/Reels/feed suppression are unsupported. There is no universal website-blocking claim.
- DataStore 1.1.7 concurrent cold-subscription limitation remains; stable dependency retained rather than adding an alpha solely for tests.
- Existing lint warnings and partial localization remain documented. Remote GitHub CI and production signing/publication are not executed locally. Play-delivered split installation is not certified by a universal APK smoke test.
- Emulator coverage is substantial but is not an exhaustive TalkBack, keyboard, OEM or multi-device acceptance campaign.

The hardware checks require actual devices/sensors and cannot be established by an emulator; emulator UI and release smoke checks are the available substitute, not a pass for those requirements. Next external acceptance should use the final beta on physical devices and record OEM, sensor and refresh-rate evidence. Production distribution requires owner signing credentials and publication authorization. Remote CI requires publishing/running the workflow. None was fabricated or bypassed.

## 11. Repository readiness

MIT license, README/logo/real screenshots, privacy/security/contribution guidance, changelog and issue/PR templates are present. CI validates clean JVM/lint/debug/release/AAB and a separate API35 suite; missing official Firefox fixture is explicitly a skip, not browser certification. Workflow/template YAML and local documentation links pass checks. Historical delivery claims are labeled; obsolete build-failure logs are removed and old manifest hashes archived. No build outputs, keystores, local configuration or temporary production tracing are committed. Nothing was pushed or published by this run.

## 12. Important changed files

- `data/SearchRanking.kt`, `AppSearchIndex.kt`: deterministic ranking and prepared search tokens.
- `data/*Rules*`, configuration codecs and platform enforcement/listener files: bounded rules, compatible storage, local processing and lifecycle diagnostics.
- `ui/home/HomeWidgets.kt`, folder grid, `ui/navigation/GestureCoordinator.kt`, `LauncherUi.kt`: completed/cancelled interaction ownership and overlay semantics.
- `ui/apps/AppsScreen.kt`, `ui/folders/FoldersScreen.kt`: fixed footer safe insets.
- `app/build.gradle.kts`: release resource shrinking and offline language packaging.
- JVM/Android tests: configuration/rule/search coverage, real notification/browser fixtures, UI persistence/gestures/editors, inset regression and measured search workload.
- `scripts/Measure-Artifacts.ps1`, `docs/PERFORMANCE.md`: repeatable archive measurement and evidence.
- README, CI workflow and repository docs: synchronized capabilities and validation limits.

## 13. Release decision

**BETA READY** — all feasible local implementation milestones are addressed, the final clean build and full regression suite pass, and the minified release works in executed emulator smoke checks. No introduced regression is known. Physical/OEM acceptance, broader accessibility/device coverage, remaining lint/localization limitations, remote CI and production signing prevent a release-candidate classification. This is suitable for controlled beta testing, not a claim of universal platform support.

Final local checkpoints: `056f629` (M6 code/tests/measurements), `cedca34` (M7 documentation/CI/screenshots); this report is the M8 documentation checkpoint. Exact operational state is in root `docs/CURRENT_STATUS.md`; physical/distribution follow-up must preserve the verified implementation and rerun affected checks for any future code changes.
