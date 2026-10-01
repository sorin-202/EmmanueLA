# EmmanueLA v2 — implementation map

These changes extend the existing EMA repository. Source is under `app/src/main/java/com/emmanuela/launcher`.

| Component | Implementation |
| --- | --- |
| MainActivity | Existing launcher lifecycle; default-app Android settings; Home reset; receiver registration and permission refresh |
| LauncherViewModel | Existing configuration/catalog state plus v2 settings, bulk tags/folder transactions, recent tags, factory reset and badge preferences |
| Configuration / UiPreferences / V2Preferences | Schema6 validation, legacy decoding, preferences and widget coordinate map; one atomic DataStore document |
| Navigation / NavigationUi | Existing finite surface reducer with slide/fade transition and damped drag layer |
| GestureCoordinator | Root Initial-pass arbitration, control exclusions, axis lock, cancellation cleanup and full-content Home swipes |
| HomeTapRouter | Shared double/triple-tap discrimination over child targets and background |
| HomeWidgets | Five independent widgets, coordinate clamping, edit-mode drag, grid snap, usage gradient, tap/hold actions |
| V2SettingsUi | Coordinate/vector/weather/location controls, SAF directory selector, custom colors and wallpaper schedule |
| WallpaperEditor / WallpaperCache | Nondestructive pan/pinch/alignment preview and one sampled hardware bitmap cache |
| WeatherRepository | Optional serialized hourly HTTPS lookup, cached/offline labels, bounded response/timeouts, no GPS/polling worker |
| AppSearchIndex / AppMetadata | Existing indexed alias/tag search, visible-tag lookup and normalized metadata |
| AlphabetIndex / IndexedAppList | Direct section offsets, full rail inactive alpha, wave/fade/bubble feedback, per-section haptics |
| LauncherUi | Search auto-clear/# badge/recent chips, text/vector anchors, protected overlays and widget/viewport edit routes |
| AppManagementUi / BulkActions | Centered icon context sheet, badge mute, multi-select overflow, atomic rule/tag/folder operations and system uninstall queue |
| AppPolicy / AppUsage | Package rules, schedule boundaries and per-package activity interval union; launch-entry enforcement |
| NotificationFiltering / NotificationBadges | Permission-bound listener, master suppression gate, ephemeral badge counts and optional WorkManager count summaries |
| VectorPack |24 original native outline vector drawings, no icon-font/bitmap dependency |
| FolderGrid / FolderSecurity / Folders / Editors | Dimension-derived Canvas cells, password/PIN hashing, persistent placement, vector/color picker and alias/tag reset |
| Theme / SettingsUi | Custom palette, existing font import/scaling, grouped settings and seven-action gesture matrix |
| AndroidManifest / Gradle / proguard-rules | New package/name/version, INTERNET/REQUEST_DELETE_PACKAGES declarations, DocumentFile dependency, release shrinking and Worker constructor retention |

## Widget state example

```kotlin
val placement = WidgetPlacement(x = 32f, y = 96f) // dp in content canvas
model.v2Settings { old ->
    old.copy(widgetPositions = old.widgetPositions + ("clock" to placement))
}
```

During a drag only local pointer state changes. One update at release snaps/clamps and persists placement. Arrangement is an overlay, so global navigation is disabled during editing. Scalar position offsets are read in the placement lambda rather than rebuilding the whole screen per pointer.

## Bulk transaction example

```kotlin
model.changePolicies(selectedPackages) { old -> old.copy(blocked = true) }
model.bulkTags(selectedPackages, "work focus")
model.bulkFolder(selectedPackages, folderId)
```

Each operation performs a single validated DataStore transaction. Hidden rules require authorization; protected folders must be unlocked before assignment. Timer editor Apply switches preserve unselected fields. Batch uninstall is different: Android owns the confirmation for each app and can decline protected packages.

## Pointer and animation contract

Do not add a second full-screen pointer detector over `LauncherSurfaceHost`. The root already handles navigation and excludes registered search/rail controls. `HomeTapRouter` owns multi-taps; app long presses retain context-menu priority. Dragging widgets is restricted to arrangement mode. Drawer vertical gestures remain LazyColumn gestures. Layer transforms provide visual feedback but all Compose pointer/list-state operations still follow the UI frame clock.

## App IDs and permissions

Changing applicationId to `com.emmanuela.launcher` creates a new install. Use old JSON backups to migrate; do not expect private DataStore, imported files or Android grants to cross sandboxes. The application does not request accessibility access or device-owner status. Usage and notification access have explicit Settings entry points, force-lock-only administrator access is revocable, and uninstall requests show Android UI.

## Intentional platform limits

No device-wide parental lock, original-notification replay, background weather/location service, third-party AppWidgetHost, overlapping free-pixel folder canvas, exact wallpaper wakeup alarm or verified frame-rate claim is included. Folder canvas is a responsive snapped cell layout; wallpaper viewport is shared across the chosen album. Weather may be stale/offline. See README and ARCHITECTURE for complete behavior.
