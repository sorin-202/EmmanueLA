# EmmanueLA v2 — product behavior and acceptance

The existing four surfaces remain: Home, All Apps, Folders and Settings. OLED black/white is the default; personalization must remain optional. Ordinary app lists are text-first, with optional notification count suffixes.

## Home

- Clock, date, battery, screen time and weather can be shown independently. Font, scale and color are shared styling choices; x/y dp coordinates are per widget.
- Arrange mode: direct drag, bounds clamp, configurable magnetic grid and persist on release. Coordinate editor supports precise placement; reset restores defaults.
- Screen time blends progressively toward #FF1744 at the configured daily target. Default shortcut opens Digital Wellbeing where supported, falling back to Usage Access; action can be replaced.
- Weather off by default, manual coordinate setup with data-disclosure text, optional preferred-app target and Open-Meteo attribution. Cached/offline state must be intelligible.
-0–8 favorites use aliases and configurable alignment. Bottom left/right actions use app names or native vectors, with a separate always-reachable Settings control.
- Four swipes, double/triple tap and Hold are configurable; master switch disables custom gestures. App long press retains management priority. Enabling multi-taps delays single tap discrimination.

## Drawer

- Pill, Box, Underline, Floating Outline or legacy Filled search; selectable Top/Bottom location, accent, visibility and auto-keyboard.
- Exit or launch request clears search. Leading # switches to tag-only mode and outlined circle badge immediately. Up to3 actual recently used visible tags appear as chips; tag/search suggestions have separate switches.
- Cached lookup debounce60ms; aliases/original labels/packages match normal mode. Tag terms intersect, with the unfinished final term treated as prefix.
- Alphabet rail shows A–Z and actual additional-script sections. Inactive letters have30% opacity and do not jump; active sections have direct offsets. Wave tracks touch position; active-section changes give subtle haptic feedback. Styles and on/off remain configurable.
- Long press opens centered-icon sheet for launch blocking, hidden state, folder add, name/tags/reset, badge mute, timer/schedules, notification system settings and app info.

## Folders

- Outline or pastel-filled tiles, expanded colors and vector symbols. Fill picks legible black/white text/icons. Membership count excludes hidden apps; protected tiles show locked state.
- Existing one-column/adaptive/freeform layouts plus Canvas mode with column count based on minimum tile width and configured grid gap. Persistent cells retain gaps. Drop to visible targets; no automatic edge scroll.
- Edit name/color/vector/membership, optional password or numeric PIN. Authentication is required to edit/remove protection. In-folder native icons can be enabled; text alias mode remains available.

## Settings hierarchy

About & FAQs (guide/version), Change Default Launcher, More Features, Home Screen, App List, Folders, Appearance, Notification Filter, App Settings, Gestures. Additional direct links to alias, hidden and summary screens remain for convenience.

More includes JSON export/restore and full factory reset. Appearance includes Light/Dark/System/Custom, font import/bold/italic/scale, status bar and directory/rotation/viewport/dim/blur controls. App Settings includes searchable multi-selection and overflow actions Block/Hide/Timer/Add Tags/Assign Folder/Batch Uninstall. Notification master gates saved per-app filtering; local badge counts have a separate switch and per-app mute.

## Mandatory verification

- New package installs alongside EMA; schema2–5 backups import to6 without losing metadata/rules. Invalid input never overwrites saved data.
- Modal editing/dragging never launches or swipes the underlying Home. Search cursor, vertical list scroll and system edges remain usable.
- Recent tags cannot expose tags unique to hidden apps. Protected folders/hidden management relock on background and process restart.
- Bulk selections survive filtering and preserve unchecked fields. Android confirms each uninstall; cancellation causes no false success report.
- Network is absent unless weather is enabled with valid coordinates. Weather/cache and notification permission denial do not crash Home.
- Master notification-off leaves notifications alone; badge mute does not change Android channel settings.
- Reset clears launcher-local configuration/caches/grants, not installed applications. Wallpaper crop preserves original files.
- Build, unit tests, lint, accessibility checks and release device profiling must pass before production use. No zero-lag or60/120fps certification is implied by the source delivery.

Blocking is launcher-entry focus control, not tamper-proof parental enforcement. Notification dismissal is post-arrival, summaries contain counts only, password folders are not encrypted vaults, and weather is forecast-derived rather than guaranteed live station data. See ARCHITECTURE for API constraints.
