# Privacy and platform capabilities

EmmanueLA stores settings, aliases, tags, folders and focus rules locally. It has no account, advertising or analytics SDK. Android backup is disabled; explicit configuration exports exclude media grants and font files. Password-protected folders use salted PBKDF2; this is launcher access control, not encryption of other apps.

## Permission audit
| Permission / access | Feature | Denied or disabled |
|---|---|---|
| INTERNET | Opt-in Open-Meteo weather/geocoding | Weather unavailable; launcher works offline. |
| ACCESS_COARSE_LOCATION | Optional approximate location widget | Manual weather location remains available. |
| READ_CONTACTS | Explicit @ contact search | Search explains access requirement; app search works. |
| CAMERA | Flashlight via CameraManager | Flashlight explains denial; camera app intents remain independent. |
| VIBRATE | Optional haptics | No vibration; system touch preference respected. |
| USE_BIOMETRIC | Private/hidden app and folder authentication | Device-credential fallback; requires a configured device lock. |
| PACKAGE_USAGE_STATS | Local usage display and time limits | Limits fail closed with an access explanation; ordinary launcher functions remain. |
| POST_NOTIFICATIONS | Digest and session reminders | No reminder; listener access is separate. |
| ACCESS_NOTIFICATION_POLICY | User-requested DND action | Opens Android access settings. |
| REQUEST_DELETE_PACKAGES | User-requested app uninstall | Android owns confirmation and outcome. |
| Device administrator | Optional screen lock gesture | Opens explicit setup; disable admin before uninstalling. |
| Notification listener | Dismiss/digest selected notifications | Filters do not run. Listener sees notifications after posting; initial alerts cannot be undone. |
| Accessibility service | Optional foreground app rules and browser address matching | Rules apply through launcher admission only. Not a device-owner security boundary. |

Weather requests send selected coordinates to api.open-meteo.com and typed location queries to geocoding-api.open-meteo.com. Requests are HTTPS with timeouts. External intents are handled by the selected Android app and its privacy policy. Notification digest stores package counts and hashes of identities, not message bodies. Accessibility inspects supported browser address fields; it does not retain browsing history.

## Reliability classification
- Launcher app admission: deterministic local rule evaluation; API35 tests verify blocked-policy and intention gates. Physical/OEM acceptance remains outstanding.
- External-app blocking: BEST-EFFORT optional accessibility overlay; owner/OEM can disable it.
- Website blocking: BEST-EFFORT visible address matching. The tested official Firefox157 normal address source is UNSUPPORTED and reports an honest diagnostic. Legacy Firefox IDs and Chrome/beta, Brave, Edge and Samsung hints are unvalidated. Hidden address bars, private modes, custom tabs, browser updates and inaccessible trees can prevent matching. See BROWSER_COMPATIBILITY.md for exact version/mode evidence.
- Opera: UNSUPPORTED by current adapters.
- Selective Shorts/Reels/Explore/feed suppression: UNSUPPORTED. No claim that a launcher can reliably remove arbitrary native app surfaces.
- Notification quiet delivery: initial sound/heads-up cannot be reversed after listener delivery. Use Android per-app channels for reliable quiet delivery.
- Focus session reminders: deferred WorkManager notifications; Android may delay them in Doze.

No network VPN or embedded social-media client is added. A VPN cannot identify encrypted URL paths without invasive interception; app-specific accessibility heuristics or web wrappers would add privacy, maintenance and reliability costs.

Notification rule extension: optional keyword matching reads title/text/big-text only for applicable opted-in rules, in memory. No message content is logged or saved. Notification access and connected-listener status are distinct. Per-app schedules and Strict Block integration reevaluate on rule/time changes and at boundaries; incoming notifications are evaluated at their arrival time. Temporary suppression dismisses clearable notifications for 30 minutes and is excluded from portable exports. Calls, alarms, ongoing, media and service notifications remain exempt. Configuration v9 reads v2-v8. See Android's NotificationListenerService lifecycle contract: https://developer.android.com/reference/android/service/notification/NotificationListenerService .
