# Website rules and foreground enforcement

Website filtering uses Android Accessibility to inspect **only the address control of a selected browser**. It displays a blocking Accessibility overlay for a matching active rule. It does not intercept network requests. Browser versions, private modes, hidden toolbars and inaccessible windows can prevent detection; there is no universal browser support claim.

## Implementation and root corrections

The former service waited for a new `TYPE_WINDOW_STATE_CHANGED` event. On connection, permission enable or restart while an app was already open, it could have no foreground package to evaluate. It now reconciles actual application windows on connection, rule enable, window changes and screen unlock. A single timer schedules the next rule/usage boundary. Usage Access changes have an AppOps callback. There is no foreground polling loop. Android owns Accessibility service rebinding after reboot; rules are loaded from local persistence after unlock. Force-stop, permission revocation and manufacturer battery controls can interrupt enforcement.

The former browser reader used only legacy resource IDs and `text`. Firefox's Compose address control can instead expose an `ADDRESSBAR_URL_BOX` resource tag and its address in `contentDescription`. The adapter now recognizes that exact tag, with a bounded search of app chrome metadata that skips WebView/GeckoView content. It reads no arbitrary page-node text. Display descriptions must contain one unambiguous valid HTTP(S) address token. Focused editable address controls wait for navigation, so typing a blocked domain is not itself treated as visiting it.

Application and website groups share package targeting and usage accounting. A mixed app/site group includes the current browser in its usage total. Android does not expose per-site usage totals: website daily/session allowances use browser usage, including unrelated sites. A rule boundary that ends a block now removes an existing overlay; per-group monotonic session anchors prevent adding time twice when a session rule becomes active while the app remains open.

## Adapter coverage

| Browser packages | Address controls | Runtime verification required |
|---|---|---|
| Chrome / Chrome Beta (`com.android.chrome`, `com.chrome.beta`) | `url_bar` | Normal and private tabs; collapsed/expanded bar; current installed version |
| Brave (`com.brave.browser`) | `url_bar` | Same; not assumed equivalent to Chrome |
| Edge (`com.microsoft.emmx`) | `url_bar` | Same; not assumed equivalent to Chrome |
| Firefox (`org.mozilla.firefox`) | Legacy `mozac_browser_toolbar_url_view`, `mozac_browser_toolbar_edit_url_view`, `url_bar`; Compose `ADDRESSBAR_URL_BOX` | Text and display-description variants; private mode; GeckoView/toolbar versions |
| Samsung Internet (`com.sec.android.app.sbrowser`) | `location_bar_edit_text`, `location_bar_text_view` | Real Samsung device/current version; private mode |
| Opera / other packages | None | Unsupported; no website-enforcement claim |

These are implemented adapters, not a matrix of physically verified browser versions. Prior reports and old passing tests are not evidence of current browser behavior. Current execution results belong in the task's validation report.

## Validation and truthful diagnostics

`BrowserAdapterTest` verifies supported address samples, Firefox descriptions, package ownership, editing states, ambiguous labels, bounded input and unsafe addresses. `FocusTargetsTest` checks mixed-group accounting, domain/subdomain boundaries, IDN normalization, encoded URL keywords, schedules and Break. Existing Focus allowance, Break, session and deadline tests cover shared rule semantics.

`FocusServiceTest` uses a separate APK fixture to exercise the real Accessibility service: an app already open before permission enable, live rule/Break/schedule changes, Usage Access revocation/restoration, service permission revocation/rebind, and an external task launch. It also reads a real Android address-control fixture through Accessibility. This is not a substitute for testing an official browser.

`BrowserDiagnosticsTest` requires an installed supported browser with first-run onboarding completed. It opens an example HTTP(S) navigation, requires an address to become readable, then requires the actual website-rule overlay to appear in Android's accessibility tree. It no longer passes merely because Firefox reports an unreadable address. Absence of a browser is a skipped prerequisite, not a successful browser test. No browser APK is bundled or redistributed.

### Verified automated browser run

[CI run 39049201457](https://github.com/sorin-202/EmmanueLA/actions/runs/39049201457), commit `392cd5331a02d22bbd873926f8541f8e574ca50e`, ran the dedicated browser test on an Android 15/API 35 x86_64 KVM emulator with the official Firefox **157.0.1** APK. Its downloaded APK matched the workflow's pinned SHA-256. The archived JUnit XML reports **1 test passed, 0 failures, 0 errors, 0 skipped** for `supportedBrowserNavigationActuallyDisplaysBlockingOverlay`.

The archived preflight identifies Firefox's `ADDRESSBAR_URL_BOX` control, whose empty `text` and `contentDescription` of ` example.com/focus-fixture. Search or enter address` exercise the display-description adapter in a normal tab. The test then independently requires the real Accessibility service to connect, read the navigated browser address, report `OVERLAY_SHOWN`, expose the exact rule reason in Android's accessibility tree, return to EmmanueLA, and retain the readable-browser diagnostic without an issue. A successful onboarding/preflight alone would not satisfy those assertions. The preflight recorded **0 Quickstep ANR recovery attempts** (`not_needed`) for this run.

Evidence is archived as the run's `firefox-browser-test-reports` artifact (ID `12107595024`): `firefox-preflight/result.json`, its captured Android hierarchy, and the `BrowserDiagnosticsTest` JUnit XML/logcat. This establishes this Firefox/version/emulator path. Private tabs, other browser packages and versions, hidden toolbars, redirects, and physical-device behavior still require their own runtime tests. The general regression job intentionally lacks Firefox and skips this prerequisite-dependent case; the dedicated browser job supplies the executed case above.

Live the Moment shows Android permission state separately from current service connection. Browser diagnostics distinguish unsupported package, foreign window, missing control, address editing, unreadable value and tree query failure. The last browser check remains visible when returning Home. An overlay is reported only after WindowManager accepts it. A successful Home API call is reported as **requested**, not as proof that Android navigated. A failed overlay/Home action is visible as a failure. No URLs or page contents are persisted or transmitted by this service.

## Physical-device acceptance still required

For each browser/version under test, record normal/private mode, address-bar visibility, direct navigation/redirect/back/forward behavior, matching domains and subdomains, unrelated domains, keyword encoding, and actual overlay visibility. For app enforcement, test already-open apps, Recents, another launcher, notifications and deep links; daily/session deadlines, warnings/grace, schedule boundaries, Break expiry, Usage Access/Accessibility revocation, reboot and manufacturer battery restrictions. Test multiwindow behavior separately: the implementation targets the focused application window.

Android system permission, lock-screen and known emergency-system packages remain reachable. A user can disable Accessibility or force-stop the launcher. Unreadable browser addresses cannot be selectively blocked. Shorts, Reels and in-app feed removal are unsupported. Android documents that [the accessibility root can be null](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#getRootInActiveWindow()).
