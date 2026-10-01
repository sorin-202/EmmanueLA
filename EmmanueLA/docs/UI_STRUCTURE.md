# Structura completă EmmanueLA 2.5

Fiecare intrare are explicația delimitată prin `#`. Sunt listate fișierele livrate, inclusiv resursele și testele.

```text
EmmanueLA/ # proiect Android existent actualizat
├── .github/ # director .github
│   └── workflows/ # director workflows
│       └── android.yml # Flux CI Android
├── app/ # director app
│   ├── src/ # director src
│   │   ├── androidTest/ # director androidTest
│   │   │   └── java/ # director java
│   │   │       └── com/ # director com
│   │   │           └── emmanuela/ # director emmanuela
│   │   │               └── launcher/ # director launcher
│   │   │                   └── NativeTypographyTest.kt # test instrumentat pentru Typeface cursiv/weight
│   │   ├── main/ # director main
│   │   │   ├── java/ # director java
│   │   │   │   └── com/ # director com
│   │   │   │       └── emmanuela/ # director emmanuela
│   │   │   │           └── launcher/ # director launcher
│   │   │   │               ├── data/ # director data
│   │   │   │               │   ├── AlphabetIndex.kt # Secțiuni alfabetice și poziții precompute
│   │   │   │               │   ├── AppMetadata.kt # Aliasuri și normalizarea tagurilor
│   │   │   │               │   ├── AppPolicy.kt # Limite, blocare și reguli per aplicație
│   │   │   │               │   ├── AppSearchIndex.kt # Căutare în memorie după nume, tag, pachet și componentă
│   │   │   │               │   ├── Configuration.kt # schema 7; import 2–7
│   │   │   │               │   ├── ExperiencePreferences.kt # Personalizare, stil Home și noile opțiuni persistente
│   │   │   │               │   ├── FocusGroups.kt # Model și serializare pentru grupurile mindful
│   │   │   │               │   ├── FolderSecurity.kt # Hash PBKDF2 și verificarea parolelor
│   │   │   │               │   ├── Folders.kt # Foldere, dimensiuni individuale și migrare compatibilă
│   │   │   │               │   ├── ReorderRules.kt # ordine magnetică și păstrarea membrilor nevizibili
│   │   │   │               │   ├── SurfaceStyles.kt # stiluri per-widget și per-fotografie cu migrare legacy
│   │   │   │               │   ├── UiPreferences.kt # Preferințe de interfață persistente
│   │   │   │               │   └── V2Preferences.kt # Widgeturi, wallpaper și opțiuni legacy v2
│   │   │   │               ├── platform/ # director platform
│   │   │   │               │   ├── AppUsage.kt # Agregarea intervalelor de utilizare Android
│   │   │   │               │   ├── Apps.kt # Catalog PackageManager și cache LRU pentru iconuri
│   │   │   │               │   ├── ContactSearch.kt # Contacte Phone și integrări Android exportate; interogări IO
│   │   │   │               │   ├── DeviceAuthentication.kt # Biometrie cu fallback PIN/parolă și refuz explicat
│   │   │   │               │   ├── FocusSessions.kt # Sesiuni UsageEvents și reminder WorkManager fără polling
│   │   │   │               │   ├── LauncherHaptics.kt # feedback tactil comun, fallback vibrator și test
│   │   │   │               │   ├── NotificationBadges.kt # Numărul notificărilor per aplicație
│   │   │   │               │   ├── NotificationFiltering.kt # Listener și sumarizarea notificărilor
│   │   │   │               │   ├── Platform.kt # Intent-uri Android și ecranul de blocare
│   │   │   │               │   ├── SystemActions.kt # Lanternă, DND și destinații Android/OEM configurabile
│   │   │   │               │   ├── TypefaceCache.kt # cache font importat încărcat pe IO
│   │   │   │               │   ├── WallpaperCache.kt # Încărcare și cache limitat pentru fotografii
│   │   │   │               │   └── WeatherRepository.kt # Date meteo și căutare localități
│   │   │   │               ├── ui/ # director ui
│   │   │   │               │   ├── appearance/ # director appearance
│   │   │   │               │   │   ├── AppearanceControls.kt # Import album și editare culori HEX
│   │   │   │               │   │   ├── LauncherTypography.kt # Sinteză italic/weight și tipografie globală sau locală Home
│   │   │   │               │   │   ├── Theme.kt # Temă, fonturi, italic și culori globale
│   │   │   │               │   │   ├── WallpaperEditor.kt # Preview fullscreen și ajustare drag/pinch
│   │   │   │               │   │   └── WeatherLocationPicker.kt # Selector de localitate cu căutare debounced
│   │   │   │               │   ├── apps/ # director apps
│   │   │   │               │   │   ├── AppDetails.kt # Aliasuri, taguri și picker pentru apartenențe
│   │   │   │               │   │   ├── AppManagementUi.kt # Administrare individuală, contextuală și bulk
│   │   │   │               │   │   ├── AppsScreen.kt # Drawer, căutare #/@/pachet și autolansare doar pentru aplicații
│   │   │   │               │   │   ├── BulkActions.kt # Operații pe selecții multiple
│   │   │   │               │   │   ├── CursorSearchField.kt # IME nativ și cursor desenat configurabil
│   │   │   │               │   │   └── IndexedAppList.kt # Listă lazy și alfabet cu efecte configurabile
│   │   │   │               │   ├── components/ # director components
│   │   │   │               │   │   ├── Header.kt # Antet reutilizat pentru ecranele launcherului
│   │   │   │               │   │   ├── Localization.kt # Maparea textelor la resursele limbii alese
│   │   │   │               │   │   ├── VectorPack.kt # 54 de simboluri vectoriale în stilul existent
│   │   │   │               │   │   └── VectorPackPreview.kt # Galerie Compose Preview pentru vectori
│   │   │   │               │   ├── folders/ # director folders
│   │   │   │               │   │   ├── Editors.kt # Editoare pentru folder, favorite și metadate
│   │   │   │               │   │   ├── FolderGrid.kt # Aranjare și reordonare magnetică
│   │   │   │               │   │   ├── FolderMembershipPicker.kt # Atribuire și deblocare pentru foldere protejate
│   │   │   │               │   │   ├── FoldersScreen.kt # Pagina folderelor și listele din interior
│   │   │   │               │   │   └── MagneticFolderApps.kt # drag magnetic în folder, overlay și auto-scroll
│   │   │   │               │   ├── home/ # director home
│   │   │   │               │   │   ├── BlankScreen.kt # Ecranul gol cu slogan, notă personală și Settings accesibile
│   │   │   │               │   │   ├── HomeAlphabet.kt # Rail cu Wave/haptics și listă cu scroll vertical nativ
│   │   │   │               │   │   ├── HomeScreen.kt # Favorite fixe, shortcuts și atmosfera Home
│   │   │   │               │   │   └── HomeWidgets.kt # Transformare persistentă, commit atomic și fundal rotunjit
│   │   │   │               │   ├── mindful/ # director mindful
│   │   │   │               │   │   └── FocusUi.kt # Grupuri și countdown înainte de lansare
│   │   │   │               │   ├── navigation/ # director navigation
│   │   │   │               │   │   ├── GestureCoordinator.kt # Arbitraj între gesturi globale și controale
│   │   │   │               │   │   ├── HomeTapRouter.kt # Double/triple tap și anularea tapurilor
│   │   │   │               │   │   ├── MotionPolicy.kt # Politică comună pentru durată, easing și Reduce motion
│   │   │   │               │   │   ├── Navigation.kt # Regulile Home / Apps / Folders
│   │   │   │               │   │   ├── NavigationUi.kt # Tranziții direcționale Standard / Fluid / Off
│   │   │   │               │   │   └── PagePolicy.kt # Pagini active și fallback central la Home/Apps/Folders/Blank
│   │   │   │               │   ├── privatespace/ # director privatespace
│   │   │   │               │   │   └── PrivateSpaceSettings.kt # Liste hidden/blocked/private și Security
│   │   │   │               │   └── settings/ # director settings
│   │   │   │               │       ├── AdditionalControls.kt # Preview fix, URL weather, destinație Wellbeing și surse contacte
│   │   │   │               │       ├── PermissionSettings.kt # Acces runtime/special și actualizare la revenirea din Android
│   │   │   │               │       ├── SelectionMarker.kt # accent comun pentru selecții și marker subcategorie
│   │   │   │               │       ├── SettingsUi.kt # Rânduri, dialoguri și Action Picker comun
│   │   │   │               │       ├── StructuredSettings.kt # Ierarhia paginilor Standard / Advanced
│   │   │   │               │       └── WidgetStyleControls.kt # preview fix și editor independent pentru fiecare widget
│   │   │   │               ├── LauncherUi.kt # Compoziția ecranelor și accesul securizat
│   │   │   │               ├── LauncherViewModel.kt # Fluxuri de stare, persistență și verificarea lansărilor
│   │   │   │               ├── MainActivity.kt # Lifecycle, broadcast-uri și bare de sistem
│   │   │   │               └── SystemComponentCompatibility.kt # Păstrează identitățile componentelor Android existente
│   │   │   ├── res/ # director res
│   │   │   │   ├── drawable/ # director drawable
│   │   │   │   │   ├── ic_launcher.xml # Icon vectorial al aplicației
│   │   │   │   │   └── ic_notification.xml # Icon pentru notificările launcherului
│   │   │   │   ├── values/ # director values
│   │   │   │   │   ├── experience.xml # Texte pentru controalele noi v2.3
│   │   │   │   │   ├── fixes.xml # Textele noi pentru corecțiile v2.4
│   │   │   │   │   ├── iteration.xml # resursă sau fișier xml
│   │   │   │   │   ├── navigation.xml # Texte localizate pentru navigare
│   │   │   │   │   ├── strings.xml # Texte localizate de bază
│   │   │   │   │   └── styles.xml # Tema XML de pornire Android
│   │   │   │   ├── values-de/ # director values-de
│   │   │   │   │   ├── navigation.xml # Texte localizate pentru navigare
│   │   │   │   │   └── strings.xml # Texte localizate de bază
│   │   │   │   ├── values-es/ # director values-es
│   │   │   │   │   ├── navigation.xml # Texte localizate pentru navigare
│   │   │   │   │   └── strings.xml # Texte localizate de bază
│   │   │   │   ├── values-fr/ # director values-fr
│   │   │   │   │   ├── navigation.xml # Texte localizate pentru navigare
│   │   │   │   │   └── strings.xml # Texte localizate de bază
│   │   │   │   ├── values-it/ # director values-it
│   │   │   │   │   ├── navigation.xml # Texte localizate pentru navigare
│   │   │   │   │   └── strings.xml # Texte localizate de bază
│   │   │   │   ├── values-pl/ # director values-pl
│   │   │   │   │   └── navigation.xml # Texte localizate pentru navigare
│   │   │   │   ├── values-ro/ # director values-ro
│   │   │   │   │   ├── experience.xml # Texte pentru controalele noi v2.3
│   │   │   │   │   ├── fixes.xml # Textele noi pentru corecțiile v2.4
│   │   │   │   │   ├── iteration.xml # resursă sau fișier xml
│   │   │   │   │   ├── navigation.xml # Texte localizate pentru navigare
│   │   │   │   │   └── strings.xml # Texte localizate de bază
│   │   │   │   └── xml/ # director xml
│   │   │   │       ├── device_admin.xml # Politica Android pentru screen lock
│   │   │   │       ├── file_paths.xml # Căi FileProvider pentru partajare
│   │   │   │       └── locales_config.xml # Limbile declarate în Android
│   │   │   └── AndroidManifest.xml # Launcher, servicii, permisiuni și provider
│   │   └── test/ # director test
│   │       └── java/ # director java
│   │           └── com/ # director com
│   │               └── emmanuela/ # director emmanuela
│   │                   └── launcher/ # director launcher
│   │                       ├── AppMetadataTest.kt # Teste aliasuri și taguri
│   │                       ├── AppPolicyTest.kt # Teste reguli de acces și program
│   │                       ├── ConfigurationTest.kt # Teste de backup și migrare
│   │                       ├── ExperienceTest.kt # Teste de compatibilitate a preferințelor
│   │                       ├── FocusSessionTest.kt # Teste de sesiune, escaladare și serializare
│   │                       ├── FolderCodecTest.kt # Teste de serializare folder
│   │                       ├── MotionPolicyTest.kt # Teste pentru viteze și reducerea animațiilor
│   │                       ├── NavigationTest.kt # Teste pentru bucla de swipe
│   │                       ├── ScreenTimeTest.kt # Teste pentru intervalele de utilizare
│   │                       ├── SearchIndexTest.kt # Teste pentru căutarea în memorie
│   │                       ├── SurfaceStylesTest.kt # teste codec, migrare și reordonare
│   │                       ├── V24RegressionTest.kt # Regresii pentru pagini, căutare, backup și dimensiuni
│   │                       └── V2ConfigurationTest.kt # Teste pentru configurația v2
│   ├── build.gradle.kts # Configurația build-ului și dependențelor
│   └── proguard-rules.pro # Reguli R8, inclusiv identități WorkManager
├── docs/ # director docs
│   ├── history/ # director history
│   │   ├── CHANGES_REQUESTED_2026-09-30.md # Cerințe arhivate din etapa precedentă
│   │   ├── DEVICE_CHECKLIST.md # Checklist arhivat pentru dispozitiv
│   │   ├── IMPLEMENTATION_PASS_2.md # Note istorice ale implementării
│   │   └── PROJECT_STRUCTURE.md # Arbore istoric; nu structura actuală
│   ├── ARCHITECTURE.md # Persistență, thread-uri, cache și state flow
│   ├── CHANGELOG.md # Istoricul versiunilor
│   ├── DELIVERY_MANIFEST.json # Inventarul modificărilor acestei livrări
│   ├── FRONTEND_STRUCTURE.md # Arborele complet al meniurilor, cu # Advanced
│   ├── IMPLEMENTATION_STATUS.md # Stare exactă și limite Android / build
│   ├── IMPLEMENTATION_UPDATES.md # Note tehnice din etapele precedente
│   ├── PRODUCT_REQUIREMENTS.md # Cerințele inițiale ale produsului
│   ├── REQUESTED_UI.txt # Specificația frontend autoritară pentru această rundă
│   ├── TESTING_FIXES_V24.md # Matricea cerințelor raportate și testarea locală
│   ├── TESTING_FIXES_V25.md # cerințe v2.5 și pași de validare pe telefon
│   ├── UI_IMPLEMENTATION_MATRIX.md # Corespondența celor 34 de puncte cu sursele
│   ├── UI_STRUCTURE.md # Structura reală completă, cu explicații #
│   ├── VALIDATION.md # Comenzi locale și scenarii de test pe telefon
│   └── build_attempt.log # Eroarea descărcării Gradle din runda precedentă
├── gradle/ # director gradle
│   └── wrapper/ # director wrapper
│       ├── gradle-wrapper.jar # Bootstrap pentru distribuția Gradle
│       └── gradle-wrapper.properties # Versiunea Gradle și adresa distribuției
├── tools/ # director tools
│   └── check_sources.py # Verificare structurală Kotlin / XML, fără compilare
├── .gitignore # Excluderi build și fișiere locale
├── README.md # Deschidere în Android Studio și rezumat release
├── build-apk.bat # Comandă simplificată de build pe Windows
├── build-apk.sh # Comandă simplificată de build pe Linux
├── build.gradle.kts # Configurația build-ului și dependențelor
├── gradle.properties # Opțiuni Gradle și Android
├── gradlew # Script Gradle Linux / macOS
├── gradlew.bat # Script Gradle Windows
└── settings.gradle.kts # Module și repository-uri Gradle
```
