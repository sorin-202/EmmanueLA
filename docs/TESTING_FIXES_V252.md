# EmmanueLA 2.5.2 — final touch peste 2.5.1

Identitatea Android, semnătura cerută pentru update, stilul și datele existente sunt păstrate. versionName 2.5.2, versionCode 13. Surse, fără APK verificat.

## Modificări implementate

- Location: acțiune configurabilă prin Manage widgets → Location → Widget options → Tap action. Aceeași listă de aplicații/acțiuni ca celelalte widgeturi. locationAction este salvat în V2Preferences și backup; configurațiile vechi primesc No action.
- About → GitHub deschide https://github.com/sorin-202/EmmanueLA, fără Coming soon.
- Bottom controls: Size are numai Small/Medium/Large. Position from bottom, Width, Height, Padding, Text size și Text weight sunt disponibile independent de dimensiune, inclusiv în Standard. Preseturile Size stabilesc înălțimea și scala textului, după care se pot ajusta individual. Opțiunea Custom a dispărut; valorile sale vechi se păstrează la migrare, cu eticheta Medium pentru preset.
- Action picker include App List/Folders. Când ambele sunt disponibile, deschide App List; când una este dezactivată, deschide cealaltă. Acțiunile vechi Apps/Folders au același fallback dacă pagina cerută este dezactivată. Dacă ambele sunt off, revine la Home; dacă Home este și el off, ecran blank.
- Home Alphabet contează ca App List dezactivat. Alegerea App List/Folders trimite atunci la Folders dacă este disponibilă.

## Validare

PASS structural: 83 Kotlin, XML/manifest/resurse, wrapper JAR, arhivă ZIP și comparație cu sursele. Testele FinalTouchTest au fost adăugate pentru rutare, backup/migrare și Location; nu au fost executate.

NOT RUN: compilare Kotlin/Android, lint, JUnit, instrumentare, telefon/performance. Mediul nu are SDK Android/Gradle instalat. Sunt necesare validările locale.

Deschide folderul EmmanueLA în Android Studio, JDK 17, SDK 35, Gradle Sync. Pe Windows PowerShell:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:installDebug
```

Pe macOS/Linux folosește `./gradlew`. Connected tests necesită telefon/emulator conectat. APK debug: app/build/outputs/apk/debug/app-debug.apk. Pentru update păstrează aceeași semnătură.

## Checklist pe telefon

1. Location → Tap action → Maps sau o altă aplicație: atingerea widgetului lansează ținta, iar restart/export/import păstrează selecția.
2. GitHub deschide repository-ul corect, fără Coming soon.
3. Small/Medium/Large: verifică sliderul Position în fiecare; apoi Width/Height/Padding/Text size/Weight fără Custom și fără Advanced. Preview rămâne fix.
4. Importă o configurație veche cu Custom: păstrează înălțimea, lățimea și offset-ul.
5. Gesturi → Swipe Up → App List/Folders: ambele on → Apps; Apps off → Folders; Folders off → Apps; ambele off → Home; toate off → blank.
6. Repetă cu vechile acțiuni All Apps și Folders, cu Home Alphabet on și cu shortcuturile bottom.
7. Retestează patch-ul 2.5.1: biometrie, hidden/blocked manager, drag folder și Edge-to-edge. Vezi TESTING_FIXES_V251.md.
