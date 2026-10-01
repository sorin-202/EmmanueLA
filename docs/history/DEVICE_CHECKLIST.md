# EmmanueLA 5.0 verification checklist

Authoring status: Gradle failed before compilation while resolving services.gradle.org; no Android SDK is installed in the authoring environment. Tests below are required, not reported as passing. XML and ZIP structural checks do not prove Kotlin compilation.

## Build and upgrade

- Use JDK17/SDK35; run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- Test clean install and upgrade signed with the existing key. Verify v2 favorite-name migration, v3 metadata, old folder JSON and version5 backup round trip. Reject corrupt/future backups without losing current data.
- Exercise process recreation, rotation, Home intents, Back and restoring a disabled All Apps preference. Ensure Settings is always reachable.

## Touch, search and accessibility

- Swipe in all four Home directions over background, clock, date, battery, favorite text and shortcuts. Verify no accidental app click. Check diagonal/short/two-finger gestures, long-press menus and system edges.
- Enable double and triple taps independently/together. Verify assignment, timeout/distance rejection, no duplicate click and cancellation on swipe/navigation. With both None, app tap is immediate.
- Drawer vertical scroll and search cursor/selection must remain native. Left loops Apps↔Folders; right always Home. Test gesture animation while keyboard is shown.
- Alias update must change all text lists/sort order. Search original label/package; test bare #, #prefix, multiple exact tags, final prefix, tag suggestions, whitespace, tag-mode off, Unicode, emoji and digit initials.
- Rail must contain only current results, reach exact section offsets during fast dragging, and not navigate pages. Exercise Bubble/Fade/None and alphabet disabled.
- TalkBack, large fonts/display size, landscape, light/dark/system, imported font, color choices and smallest supported screen. Check overflow of bottom shortcuts/widget, crowded alphabet and touch target accessibility.

## Rules and protection

- Block from Home, drawer, folders, gesture app targets and resolved camera/clock/dial shortcuts. No launcher path should bypass policy. Explain that Recents/notifications/other launchers remain outside enforcement.
- Test limit below/at/above boundary, denied/revoked Usage Access, day rollover, timezone changes and overnight weekday schedule. Existing running apps are not forcibly closed.
- Bulk-edit selected sections while preserving unchecked rules; validate failed save and multi-activity package consistency.
- Hide removes every ordinary app entry and selector. Device credential cancellation must reveal nothing. Reauthentication after background, Home reset and process death. Test device PIN removal/recreation.
- Password folder create/change/remove, wrong attempts/cooldown, canceled dialogs, protected editor after background. Verify folder passwords do not imply system-wide app blocking or encrypted backups.

## Notifications and battery

- Grant/revoke listener access; test NORMAL/DISMISS/DIGEST. Calls, alarms, ongoing/service/media notifications and group summaries remain untouched.
- Verify post-arrival caveat and Android channel settings link. Count dedup/bounds; permission-denied and summary-channel-disabled states must not crash or falsely acknowledge a summary.
- Worker exists only with DIGEST policy; disable all batching, restore/reset config, reboot and test Doze-delayed delivery. No exact delivery-time promise.
- Summary opens local counts; hidden apps are not listed without authentication. Counts clear and remain absent from JSON backup.

## Folder layout and media

- Adaptive width, one-column and two-column freeform; reorder adjacent/nonadjacent visible cells and empty slots, restart, verify persistent gaps/order. Settings arrows reset sequential cell order intentionally.
- Drag cancel and modal edits; large folder counts. No offscreen drop auto-scroll is implemented.
- Symbol top/left/hidden, size, fill and contrasting foreground. Optional folder icons use bounded cache.
- Huge photo/corrupt photo/revoked URI, wallpaper rotation, dim/blur, font import failure and portable restore. Blur is only available on Android12+.

## Physical-device performance gate

- Profile release/profileable builds on 60Hz and 120Hz devices using Perfetto/FrameTimeline and Android Studio memory profiler. Record device/API/build, p50/p95/p99 frame duration and missed frame deadlines for Home/drawer/rail/keyboard/folder transitions.
- Budget 16.67ms at60Hz and8.33ms at120Hz. No zero-jank guarantee until traces support it. Measure cold/warm startup separately from external app startup.
- Populate 500+ apps/aliases/tags; trace fast rail dragging, bulk edits, image decode, app launches with daily limits and notification bursts. Check disk/package queries off Main and GC spikes.
- Measure idle CPU and battery with screen off; no second-by-second usage service should run. Compare batching enabled/disabled. Check icon cache after repeated scrolling and bitmap memory with blur.

## EmmanueLA v2 additions

- Verify the new application ID installs alongside EMA. Export/import old config versions2–5, then regrant access and reselect font/photos. Schema6 round trip must preserve widget coordinates, custom colors, recent tags and badge mute.
- Drag each widget in Arrange mode, test grid sizes, coordinate editor, orientation/small screens, overlays and overlapping widgets. Global gestures must not fire while arranging. All seven mappings and master-off must leave Settings reachable.
- Check gradient below/at/above screen-time target; Digital Wellbeing OEM fallback; explicit custom widget actions.
- Weather: opt-in disclosure, invalid latitude/longitude, no permission for GPS, hourly caching, offline/timeout handling, location changes, old-cache label and disabled state. Confirm no requests occur with weather disabled. Validate HTTPS and provider attribution on a real device.
- Directory selection: denied/revoked tree, empty directory, >50 images, rescan and scheduled local time rollover. Crop/pinch/zoom/alignment must preserve source files and match the Home viewport. Blur may cost frames and is Android12+ only.
- Full rail: inactive A–Z at30%, active offsets, additional Unicode initials, wave under continuous drag, haptics only for changed active section. Recent tags should exclude hidden-only metadata. Query clears after leaving/launch request.
- Bulk Add Tags/Folder while searching; preserve selection across filters, require protected-folder unlock, reject invalid tags without partial updates. Review/cancel/continue uninstall queue including system apps; each request must invoke Android confirmation.
- Verify badge-only mute does not dismiss notifications. Master filter-off stops dismissal and worker scheduling. Badge count state clears on listener disconnect; no body/message is persisted.
- Full factory reset: configuration, counters, cached weather, imported font and retained photo grants clear. Installed apps remain; Android permission grants may remain until manually revoked.
- Release R8 build (`:app:assembleRelease`) and Worker recreation after process death. Configure release signing separately. No compiled/release APK or performance benchmark has been verified here.
