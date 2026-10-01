# Corespondența structurii frontend v2.3

Acest tabel verifică existența implementării în surse. Nu certifică runtime-ul sau FPS. Limitările Android și traducerile parțiale sunt descrise în IMPLEMENTATION_STATUS.md.

| # | Cerință | Componente | Implementare |
|---|---|---|---|
| 1 | Settings | StructuredSettings | General / Your space și rute dedicate |
| 2 | System / Language | StructuredSettings | Nume de limbi și selecție persistentă |
| 3 | About | StructuredSettings | Versiune reală 2.3, FAQ, Privacy, GitHub placeholder |
| 4 | Home | StructuredSettings / HomeScreen | Secțiuni, favorite, widgeturi, bottom, status, Advanced |
| 5 | Manage Widgets | StructuredSettings | Switchuri, detalii, weather picker, tap actions |
| 6 | Arrange Widgets | HomeWidgets | Fullscreen, favorite fixe, drag/pinch/options |
| 7 | Bottom Controls | HomeScreen / StructuredSettings | Action Picker, text/icons, size, preview, custom Advanced |
| 8 | App List | AppsScreen / StructuredSettings | Layout/search/alphabet, controale Advanced |
| 9 | Cursor | CursorSearchField | Caret desenat, previews, color/width/blink/custom |
| 10 | Home Alphabet | HomeAlphabet | Stil/efect/poziție/haptics separate, ♡/#/A–Z |
| 11 | Folders | FolderGrid / FoldersScreen | List/Grid/Freeform magnetic; setări secționate |
| 12 | Folder Editor | Editors | Nume, vector, culoare individuală, app picker, layout, lock |
| 13 | Appearance | Theme / StructuredSettings | Typography, HEX, wallpaper, motion |
| 14 | Adjust Wallpaper | WallpaperEditor | Fullscreen pan/pinch/Fit/Fill, dim/blur Advanced |
| 15 | Daily Wallpaper | AppearanceControls / HomeScreen | Album/time/order; aplicare la Home vizibil |
| 16 | Motion | MotionPolicy / NavigationUi | Off/Standard/Fluid, speed, reduce, two-page preview |
| 17 | Gestures | GestureCoordinator / StructuredSettings | Diagramă și mapping; triple/sensitivity/haptics Advanced |
| 18 | Action Picker | SettingsUi | Grupat și reutilizat |
| 19 | Apps | StructuredSettings | Patru intrări cerute |
| 20 | App Settings | AppManagementUi / AppDetails | Detalii per app; bulk și reguli Advanced |
| 21 | Aliases & Tags | AppDetails / Editors | Prefix ~, chips, Add tag, reset și atomic save |
| 22 | Folder Assignments | AppDetails / FolderMembershipPicker | Picker dedicat cu deblocare folder |
| 23 | Notifications | NotificationFiltering | Badges/filter/digest, interval, per-app rules |
| 24 | Mindful Use | StructuredSettings / HomeWidgets | Dashboard și gradient spre crimson; fără blocking aici |
| 25 | Live the Moment | StructuredSettings | Grupuri, app labels și daily allowance |
| 26 | Group | FocusUi / FocusGroups / FocusSessions | Daily/session/opens/schedule/prompt/auth; launcher enforcement |
| 27 | Pause Before Opening | FocusUi / LauncherViewModel | Countdown monotonic verificat în VM; open explicit |
| 28 | Private Space | PrivateSpaceSettings | Hidden/blocked/private/security |
| 29 | Hidden Apps | PrivateSpaceSettings | Search + switch; hide-from-search Advanced |
| 30 | Blocked Apps | StructuredSettings / PrivateSpaceSettings | Pagină individuală cu switch |
| 31 | Private Apps | DeviceAuthentication / LauncherUi | Biometric/credential; fallback API 26–29 |
| 32 | Security | DeviceAuthentication / ExperiencePreferences | Method selector, auto-lock, private search visibility |
| 33 | Advanced Toggle | SettingsHeading | Persistat; fără rutare sau reset scroll |
| 34 | Visual Rules | SettingsUi / Theme | Safe top insets, 28sp titles, discrete section separators |
