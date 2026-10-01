# EmmanueLA — launcher refactor pass 2

## Implemented in source
- App List ↔ Folders transition uses a short linear horizontal transition and linear drag tracking (no nonlinear resistance curve).
- Widget dragging uses `graphicsLayer` translation while dragging and persists/snap-clamps only at gesture end to avoid relayout jank.
- Widget arrangement is a true full-screen preview with controls overlaid on top.
- Global italic typography now also supplies a default `ProvideTextStyle`, fixing Text nodes that did not explicitly choose Material typography.
- App-list single-result auto-launch retained with debounce/race protection.
- Home status-bar visibility remains user configurable; Home runs edge-to-edge and wallpaper fills the root.
- Approximate Location widget added. It requests coarse permission only when enabled and reads last-known location without continuous tracking.
- Weather picker keeps city/place search with manual coordinate fallback.
- Digital Wellbeing launcher has generic + Google Wellbeing + Usage Access fallbacks, so Romanian `Confort digital` is reached by component/action rather than translated display name.
- Navigation bar settings expanded: width, height, horizontal padding, text scale, font weight and bottom distance.
- Settings hierarchy: General / Your space / App space / Private space.
- Aliases & tags editor now edits alias, multiple tags and direct multi-folder membership in one screen.
- Private app policy added. Launches initiated through EmmanueLA require Android device credential; direct ViewModel launch paths cannot silently bypass the policy.
- Dedicated Hidden / Blocked / Private entries are exposed under Private space.
- Android locale configuration added for en, ro, de, fr, es, it plus a Language row that opens per-app language settings on Android 13+.

## Important implementation boundaries
- Search cursor color is fully rendered. Thickness and blink interval are persisted and exposed in Settings, but Material3 TextField does not expose those two parameters. They are intentionally not faked; the next step is replacing only the search control with a BasicTextField-backed cursor renderer.
- `Mindful use` continues to use the existing bulk AppPolicy engine (block, schedule, daily limit, notification batching). Named reusable Screen-Zen groups are not yet persisted as first-class objects.
- Locale infrastructure is ready, but most legacy screen copy is still hard-coded English and must be migrated to `stringResource()` before claiming full translations.

## Build verification
The source tree was checked for consistency, but a full Gradle compile could not be run in this environment because the Gradle wrapper distribution is not cached and network access to `services.gradle.org` is unavailable. Open in Android Studio, allow Gradle Sync, then run `:app:assembleDebug` or deploy directly to the connected device.
