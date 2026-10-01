# EmmanueLA – requested UX update

Implemented in this revision:

- App List ↔ Folders transition simplified to a short, linear-feeling horizontal slide without cross-fade/alpha bending.
- Edge-to-edge launcher content: Scaffold no longer reserves system-bar content insets; wallpaper can cover the whole launcher surface.
- `Show status bar` moved/exposed in Home screen settings.
- App List search auto-opens the app when a non-empty (2+ characters) settled query resolves to exactly one result.
- Wallpaper editor copy improved and Reset added beside photo preview controls.
- Settings top-level hierarchy reorganized into General / Your space / App space.
- Section headings made visually larger than setting rows.
- Weather location can now be searched by city/place via Open-Meteo geocoding, with manual coordinates retained.
- Digital Wellbeing shortcut now tries generic Android Wellbeing, Google's Wellbeing activity, then Usage Access as fallback. This also covers devices where the UI label is localized (e.g. Confort Digital).
- Home navigation shortcut bar now supports width, text/icon scale and distance from bottom.
- Search cursor color can be styled separately.
- Aliases/tags list prefixes aliased apps with `~` and shows folder membership.
- Alias/tag editor displays current folder memberships.
- Mindful use entry added and connected to the existing bulk app-rule engine (screen-time limits, schedules, blocking) and notification filtering.
- GitHub entry is present and ready for `PublicLinks.GITHUB` to be filled later.

Not fully implemented in this revision:

- Full application localization into multiple languages. Most UI text in the existing project is still hard-coded Kotlin text, so real multi-language support requires extracting the UI copy into Android string resources and adding translated `values-xx` resource sets.
- Persistent named Mindful-use groups with their own saved group objects. The current page deliberately reuses bulk App Settings rules so there is no duplicate policy/background engine.
- Direct folder membership editing inside the Alias/Tags dialog. Current membership is displayed there; folder membership editing remains in Folders → Edit folder.
- Non-color cursor shapes (block/underline/custom thickness). Material3 TextField exposes cursor color directly; cursor-shape customization requires replacing it with a lower-level BasicTextField implementation.

Build note:

The source tree was structurally checked. A full Gradle build could not run in the execution sandbox because the wrapper attempted to download Gradle from services.gradle.org and outbound network access for the build process was unavailable. Open the project in Android Studio and let Gradle sync using your normal network/cache, then run `assembleDebug` / install on device.
