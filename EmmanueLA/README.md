# EmmanueLA

**A minimalist Android launcher for a calmer, more intentional phone.**

EmmanueLA replaces a crowded home screen with elegant typography, useful widgets, customizable gestures, and simple ways to organize your apps. It is text-first by default, with optional icons and color accents when you want them.

**Status: Beta · Current source version: 2.5 · Android 8.0+**

This project is actively developed. Features, settings, and behavior may change between beta releases. Some integrations depend on your Android version, device manufacturer, and permissions. Bug reports and feedback are welcome.

[Source code](https://github.com/sorin-202/EmmanueLA) · [Report a bug](https://github.com/sorin-202/EmmanueLA/issues)

## Features

### A home screen that stays out of your way

- A clean, text-first layout with generous spacing and an OLED-friendly dark appearance.
- Up to **eight favorite apps**, displayed using their system names or your custom aliases.
- Adjustable app alignment, spacing, and Home-specific typography.
- Two bottom shortcuts with customizable actions, text or vector symbols, size, weight, and vertical position.
- Optional status bar hiding and edge-to-edge wallpaper display.
- Independent switches for Home, App List, and Folders.
- When all three pages are disabled, a minimal EmmanueLA screen keeps your **Note to self** and Settings accessible.
- When Folders is the only active page, its footer provides access to Settings.

### Independent, movable widgets

Choose the widgets you need:

| Widget | What it does |
| --- | --- |
| Clock | Displays the time and opens your chosen action or clock app. |
| Date | Displays the date and can open your calendar or another selected action. |
| Battery | Shows the battery percentage and provides a shortcut to battery information. |
| Screen time | Shows daily device usage and changes color toward red as you approach your configured goal. |
| Weather | Displays weather for a selected location; its tap action can open a weather app or website. |
| Location | Displays approximate latitude and longitude with your permission. |

Each widget has its own appearance settings and a preview: font, text size, weight, italic style, colors, background opacity, and rounded corners. Translucent backgrounds offer a subtle glass-like appearance.

Use **Manage widgets** to open a widget's style editor or enable and disable it. Widget options let you configure its content and tap action. **Arrange widgets** provides a full-screen preview with direct dragging, magnetic placement, and pinch resizing.

The Screen time shortcut supports Android Digital Wellbeing, including manufacturer-specific destinations. You can select a destination when automatic detection is unsuitable.

### Fast app discovery

The App List provides a searchable, alphabetically organized drawer with optional app icons, configurable alignment, spacing, and text size.

Search supports three modes:

| Input | Search behavior |
| --- | --- |
| `camera` | Searches app names, with configurable alias and package-name matching. |
| `#social` | Searches assigned tags instead of app names. |
| `@Alex` | Searches contacts and displays available contact actions or source apps. |

**Tag search** displays a circular `#` badge and can suggest up to three recently used tags. Tag search and suggestions can be switched off independently.

**Contact search** uses phone contacts by default. Available integrations can be selected in settings and depend on what installed apps expose through Android.

Additional search options include:

- Top or bottom placement.
- Pill, Box, Underline, and Floating Outline designs.
- Accent colors and automatic keyboard display.
- Automatic launch when a non-empty app search leaves one matching result, with configurable behavior and delay.
- Automatic query clearing when leaving the drawer or launching an app.
- Cursor presets, color, thickness, blink timing, and a custom cursor character.

### Interactive alphabet navigation

Jump to an app section by tapping or dragging along the alphabet rail. The drawer supports an active-section index or a full alphabet with inactive letters dimmed.

Choose **None, Wave, Bubble, or Fade** feedback. Wave follows your finger, with an adjustable movement amplitude shown only when Wave is selected. Home and App List have separate animation and haptics settings.

The optional **Home Alphabet** brings app browsing into the Home app area. It includes favorites (`♡`), tagged apps (`#`), and alphabetical sections. Long sections remain scrollable. Enabling this mode replaces access to the separate App List page.

Haptic feedback respects Android's touch-vibration setting. A test control helps check device support; a vibrator fallback is used when the standard View feedback is unavailable.

### Aliases, tags, and app management

Long-press an app to open its context panel. From there you can:

- Rename its displayed label with an **alias**, without renaming the installed app.
- Add or remove searchable tags and reset naming metadata.
- Assign it to folders.
- Hide, block, or mark it private.
- Configure time limits and blocked schedules.
- Control launcher notification badges and filtering rules.
- Open Android's app information or notification settings.

The dedicated **Aliases & tags** screen marks renamed apps with a `~` prefix and provides access to their tags and folder membership. **Folder assignments** offers another way to organize apps, including membership in multiple folders.

Bulk management supports selecting several apps and applying blocking, hiding, timers, tags, folder assignments, or uninstall requests. Android asks for confirmation when uninstalling apps.

### Flexible folders

Group apps into minimalist folders with:

- Custom names, a library of **54 outline vector symbols**, and color accents.
- Outlined or filled backgrounds with contrasting text and symbols.
- App counts and optional native app icons inside folders.
- List, grid, and freeform placement options.
- Global layout controls and individual folder dimensions.
- Drag-and-drop placement and persistent folder order.
- An **Arrange apps** mode for magnetic reordering inside a folder, including scrolling while dragging through a long folder.
- Optional password or PIN protection.

Apps belonging to a protected folder require that folder's authorization when launched through EmmanueLA. Protection also applies to editing or removing the folder's lock.

### Mindful Use and Live the Moment

**Mindful Use** helps you understand your habits with daily usage, a screen-time goal, frequently used apps, warning thresholds, and optional gentle reminders.

**Live the Moment** lets you create app groups with shared rules:

- Daily usage allowances across the group.
- Scheduled blocking with selected days and overnight schedules.
- A deliberate pause before opening an app.
- Increasing pauses for repeated opens.
- Maximum daily opens and session reminders.
- Optional prompts and authentication requirements for protected group changes.

Individual apps can also have their own daily limits and schedules. When a limit prevents a launch, the warning provides a route to group management.

**Important:** these controls apply to launches through EmmanueLA. They do not forcibly close another app or provide tamper-proof parental control. Android may delay background reminders.

### Private Space

Private Space separates three useful controls:

- **Hidden apps:** removes apps from the normal drawer and provides a separate hidden-apps page.
- **Blocked apps:** manages apps that cannot be launched under the current rules.
- **Private apps:** requires supported biometric or device-credential authentication before launching.

Security options include search visibility and automatic relocking behavior. Hidden-app access also supports a launcher PIN fallback.

Private Space and protected folders guard launcher entry points. They do not encrypt app data or prevent access through other Android surfaces.

### Notification controls

- Optional launcher notification counts and per-app badge muting.
- A master notification filter with per-app rules.
- Notification digests with configurable intervals.
- Shortcuts to Android's notification controls.

Badge muting affects EmmanueLA's display. Notification filtering requires Notification Access and acts after a notification arrives, so it cannot guarantee that an initial sound or banner is prevented. Digests summarize counts rather than storing message content.

### Appearance and wallpapers

Make the interface your own with:

- Light, Dark, System, and Custom theme options.
- Accent and text colors, including custom HEX values.
- Built-in font choices and `.ttf` / `.otf` font imports.
- Font scaling, Regular/Medium/Bold weight, italic styling, and text opacity.
- Independent Home typography and per-widget overrides.
- Standard, Fluid, or disabled page motion, adjustable speed, and Reduce motion.

Wallpaper tools include photo selection, daily rotation from a local album or selected directory, sequential or shuffled order, and a rotation schedule.

The full-screen editor supports drag/pinch framing, Fit/Fill, dimming, and blur. Its controls can be hidden for a clear preview. Each photo can retain its own adjustment settings for daily rotation. Original images are preserved.

### Full-screen gestures

Map **Swipe Up, Swipe Down, Swipe Left, Swipe Right, Double Tap, Triple Tap, and Hold** to launcher actions, installed apps, or supported system actions.

Default navigation keeps Home as the central hub: swipe up to your selected drawer page, swipe left to switch between App List and Folders, and swipe right to return Home when enabled.

System actions include screen lock, flashlight, Do Not Disturb, Digital Wellbeing, battery information, and shortcuts to relevant Android settings. Airplane mode opens its Android settings screen; the launcher does not silently toggle it.

Sensitivity, haptics, and a gesture master switch are configurable. Widget arrangement and app context menus retain priority over page gestures.

### Settings, backup, and sharing

Settings use a **Standard/Advanced** switch so everyday controls remain simple while deeper customization stays available. Selected choices share a color marker, and subordinate options use indentation and visual markers.

Other utilities include:

- JSON configuration export and restore.
- Launcher factory reset with confirmation.
- Default launcher selection and permission management.
- Note to self.
- Quick guide, FAQs, privacy information, and version details.
- **Share the app**, which opens Android's share sheet with the installed base APK.

Sharing the base APK does not export every split APK from an Android App Bundle installation. Backups contain launcher configuration rather than usage history; local photos, imported fonts, and device-specific access may need to be selected again on another phone.

### Languages

Navigation translations are provided for **English, Romanian, German, French, Spanish, Italian, and Polish**, with a system-language option. Translation coverage is evolving; some detailed labels still fall back to English.

## Permissions and privacy

Grant only the access needed for the features you use.

| Access | Purpose |
| --- | --- |
| Usage Access | Screen time, app usage, and usage-based rules. |
| Notification Access | Notification counts, per-app filtering, and digests. |
| Notification permission | Launcher reminders and summaries on Android versions that require it. |
| Contacts | Optional `@` contact search. |
| Approximate location | Optional location widget and location-related features. |
| Camera access | Flashlight control where Android requires it. |
| Do Not Disturb access | Supported Do Not Disturb actions. |
| Accessibility or Device Administrator | Supported screen-lock actions, depending on the selected method. |
| Photo/document access | Selected wallpaper images, albums, font imports, and backups. |
| Vibration | Optional haptic feedback; no runtime permission prompt is required. |

There are no accounts, ads, or analytics in the current source. Configuration and usage rules are stored locally. Optional weather requests send the selected coordinates to [Open-Meteo](https://open-meteo.com/). Opening websites or using other apps follows those services' own privacy policies.

## Build and run

### Requirements

- Android Studio with **JDK 17**.
- Android SDK **35**.
- An Android **8.0 / API 26** or newer device or emulator.
- Internet access for the initial dependency download.

The project uses Kotlin, Jetpack Compose, Material 3, DataStore, and WorkManager. Its current build configuration uses Gradle 8.11.1, Android Gradle Plugin 8.9.2, and Kotlin 2.1.20.

1. Clone the repository or extract the project archive.
2. Open the folder containing `settings.gradle.kts` in Android Studio.
3. Set the Gradle JDK to 17 and complete Gradle Sync.
4. Connect a device or start an emulator.
5. Build and run the `app` module, then choose EmmanueLA as your default Home app.

On macOS/Linux:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:installDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:installDebug
```

Connected tests and installation require an attached device or running emulator. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

For a distributable build, use Android Studio's **Generate Signed App Bundle / APK** flow and retain your signing key for future updates. Debug builds include development tooling and are not representative of release download size.

## Beta status and testing

The current source iteration has passed structural Kotlin/XML checks and archive-integrity checks. That does **not** confirm compilation, Android lint, automated tests, or device behavior. Those checks were not executed for this source iteration in the delivery environment.

Before distributing a build, test permissions, navigation, widget dragging, font rendering, authentication, search, wallpapers, and usage rules on real devices. See [the validation checklist](docs/TESTING_FIXES_V25.md) and [validation status](docs/VALIDATION.md).

High-refresh-rate fluidity and low resource usage are development goals. No universal frame-rate, battery-life, or installation-size guarantee is made. OEM integrations, imported font capabilities, and system power policies can affect behavior.

## Contributing and feedback

Open an [issue](https://github.com/sorin-202/EmmanueLA/issues) with:

- App version, device model, and Android version.
- Steps to reproduce the problem.
- Expected and actual behavior.
- Relevant permissions and settings.
- A screenshot, screen recording, or error log when helpful.

Avoid including contact details, notification content, private app names, or other sensitive information in public reports.

Contributions that preserve the minimalist design, accessibility, privacy, and responsiveness are welcome. For significant changes, describe the proposal in an issue first.

## Acknowledgments

EmmanueLA draws inspiration from the simplicity of **Olauncher**, the intentional-use philosophy of **Minimalist Phone**, and the fluid app navigation of **Niagara Launcher**. It is an independent project and is not affiliated with those applications.
