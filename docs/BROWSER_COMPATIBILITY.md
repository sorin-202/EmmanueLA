# Website rule capabilities

Website rules are **BEST-EFFORT** overall. They inspect an address field exposed by a selected browser through Android Accessibility. They are not a VPN, DNS filter or network firewall. A browser update, private mode, hidden toolbar, another window or an inaccessible address can prevent detection. App-wide rules remain a separate option.

No browser is currently classified RELIABLE across versions and modes. Only HTTP(S) addresses without credentials, with a valid host and at most 8192 characters, are accepted. The active root must belong to the expected browser package. Page text and localized accessibility descriptions are not treated as addresses. Unsupported/unreadable screens show a diagnostic, not a claim of successful website blocking. The last diagnostic remains visible in Live the Moment when returning Home; it describes the last check, not the current screen.

## Evidence (2026-10-05)

| Browser / mode | Classification | Evidence and limits |
|---|---|---|
| Firefox 157.0, normal tab, API35 x86_64 | UNSUPPORTED address source | Loaded `https://example.com`. Display toolbar has Compose tag `ADDRESSBAR_URL_BOX`, empty text and localized description ` example.com. Search or enter address`. Legacy resource IDs are absent. Do not parse that hint as a URL. |
| Firefox 157.0, new-tab screen | UNSUPPORTED address source | Same tag exposes `Search`, not an address. |
| Firefox 157.0, private mode | UNSUPPORTED / not validated | No support claim; private-mode runtime validation not executed. |
| Older Firefox address fields | BEST-EFFORT, not runtime validated | Legacy `mozac_browser_toolbar_url_view` / `url_bar` hints retained. |
| Chrome / Chrome Beta / Brave / Edge | BEST-EFFORT, not runtime validated | Existing `url_bar` hints retained. No official compatible test APK installed in this environment. |
| Samsung Internet | BEST-EFFORT, not runtime validated | Existing location-bar hints retained; no compatible test fixture installed. |
| Opera / other browsers | UNSUPPORTED | No address adapter. |

Shorts, Reels and in-app feed filtering are UNSUPPORTED; no semantic detection is implemented or claimed.

## Test fixture provenance

Firefox obtained from the [official Mozilla archive](https://archive.mozilla.org/pub/fenix/releases/157.0/android/fenix-157.0-android-x86_64/). APK: `fenix-157.0.multi.android-x86_64.apk`, 139,424,210 bytes, versionCode 2016186462. Android apksigner verification passed; signer organization Mozilla Corporation, Release Engineering.

- APK SHA256: `85CBD0D56C571B7EEC8E34A76A4EDEBAC713AEC773491D41C6A5915BEB2F9354`
- Signing certificate SHA256: `a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04`

An onboarding input-dispatch ANR recovered after force-stop/relaunch. Default-browser request was cancelled; no account used. Fixture is ignored and not redistributed. `BrowserDiagnosticsTest` requires this exact version and completed onboarding; absence is reported as a skipped prerequisite, not a pass. Unit tests exercise root/package mismatch, unknown adapters, bounds, invalid addresses and unsafe schemes. Android validation results are recorded in ENGINEERING_VALIDATION.md.

Android documents that [the active accessibility root may be null](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#getRootInActiveWindow()). Detection cannot guarantee observation of every navigation. No external browser source was copied.
