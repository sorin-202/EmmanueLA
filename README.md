# EmmanueLA 2.6.0-beta

Targeted upgrade of the existing launcher. See [upgrade details and exact local validation](docs/UPGRADE_V26.md). Source checks passed; Android compilation and device tests are not verified in this environment.

# EmmanueLA 2.5.2 — corecții ale proiectului existent

Această arhivă continuă actualizarea v2.3 a surselor `EmmanueLA_v2_1_refactor(1).zip`. Nu este un launcher construit de la zero. Pachetul `com.emmanuela.launcher`, datele locale existente, paleta și cele 24 de iconițe originale sunt păstrate; biblioteca are acum 54 de vectori outline.

## Deschidere în Android Studio

1. Extrage arhiva și deschide folderul `EmmanueLA`, care conține `settings.gradle.kts`.
2. Selectează JDK 17 pentru Gradle și instalează Android SDK 35.
3. Permite sincronizarea dependențelor Gradle. Proiectul folosește Gradle 8.11.1, AGP 8.9.2 și Kotlin 2.1.20.
4. Rulează `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` sau `gradlew.bat` pe Windows.
5. APK-ul debug va fi în `app/build/outputs/apk/debug/` dacă build-ul reușește.

**Compilarea nu este confirmată în acest mediu.** Încercarea reală s-a oprit la descărcarea Gradle, cu `UnknownHostException: services.gradle.org`. Nu s-au executat compilatorul Kotlin, Android lint sau testele JUnit și nu este inclus un APK verificat. Verificările structurale ale surselor și XML-urilor au trecut; acestea nu înlocuiesc compilarea. Vezi `docs/VALIDATION.md`.

## Final touch 2.5.2

Location tap action, GitHub public, bottom controls cu reglaje independente fără Custom, acțiune adaptivă App List/Folders. Vezi `docs/TESTING_FIXES_V252.md` pentru validare și migrare.

## Patch 2.5.1

Marker cu margine interioară numai pentru selecții, drag de folder într-un overlay peste grid, Hidden/Blocked manager cu switch-uri, callback biometric înregistrat numai la cerere și insets Edge-to-edge unificate. Vezi `docs/TESTING_FIXES_V251.md` pentru limitări și retestare locală.

## Completări v2.5

Stil per-widget cu preview, corecție Typeface cursiv/weight, haptics cu fallback și test, ajustări per fotografie, private/hidden apps, reordonare magnetică în folder, Share APK, selecții evidențiate și Settings accesibil din Folders. Vezi `docs/TESTING_FIXES_V25.md` pentru verificări și limite.

## Completări v2.4

- Stil Home separat, italic/weight corectate, widgeturi cu fundal rotunjit/translucid, preview fix pentru bottom controls și drag/pinch cu un singur commit.
- Home Alphabet înlocuiește App List: Wave mai amplu, feedback tactil, scroll în aplicațiile unei litere. Toate cele trei pagini pot fi dezactivate separat; ecranul gol păstrează Note to self și accesul la Settings.
- Căutare după pachet/componentă și `@` contacte cu numele și aplicația sursă; Phone implicit, integrări Android selectabile.
- Permisiuni cu rutare și stări actualizate, lanterna reală, comutare DND cu accesul necesar, selector pentru Confort digital și weather tap către aplicație sau site.
- Protecția aplicațiilor din foldere la toate punctele de intrare, fallback PIN pentru Hidden apps, dimensiuni individuale de folder și selecții Layout/Cursor evidențiate.
- Tranziții orizontale standard; dim/blur numai în Wallpaper preview; texte noi EN/RO.

Vezi `docs/TESTING_FIXES_V24.md` pentru toate cerințele, verificările pe dispozitiv și limitele exacte. **Sursele sunt livrate fără compilare confirmată sau APK verificat.**

## Completări v2.3

- Ierarhia frontend din documentul atașat: selector Language dedicat, subtitluri, preview search 2×2, diagramă Gestures, Action Picker comun grupat și pagini separate pentru Folder assignments / Blocked app.
- Animația folderelor este conectată. Home Alphabet are stil, animație și haptics independente. În v2.4, Reduce motion dezactivează efectele Home Alphabet; animația sa interactivă rămâne independentă de Motion Off pentru pagini. Motion preview arată două pagini și folosește același easing/viteză ca navigarea.
- Pauză progresivă, session limit și require-authentication pentru grupuri. Session limit verifică UsageEvents la intrările launcher-ului și programează un singur reminder WorkManager. Nu închide forțat aplicații externe; Android poate amâna reminder-ul.
- Protecție la atribuirea în foldere cu dialog de deblocare; editorul contextual salvează metadatele și apartenențele atomic.
- Margini pentru notch, pinch prin graphicsLayer în timpul editării, culori text HEX aplicate și în afara Custom Theme, controale suplimentare traduse în română.

## Modificări principale

- Setări reorganizate în **General → System / About** și **Your space → Home / App List / Folders / Appearance / Gestures / Apps / Mindful Use / Live the Moment / Private Space**.
- Modul Standard/Advanced, persistent, comutat prin ◇/◆ fără schimbarea paginii sau resetarea scrollului.
- Editor dedicat aplicației: alias, taguri, chips pentru eliminarea tagurilor și atribuire în mai multe foldere, salvate într-o singură tranzacție.
- Cursor real personalizabil, căutare #tag, autolansare configurabilă, iconițe și aliniere opționale în drawer.
- Aranjarea widgeturilor pe Home complet: drag, pinch, dimensiune, aliniere și eliminare. Wallpaper-ul se ajustează pe tot ecranul, fără câmpuri X/Y/Zoom.
- Grupuri Live the Moment: pauză explicită înainte de lansare, allowance zilnic comun, program săptămânal, număr de deschideri și autentificare pentru editarea grupurilor stricte.
- Private Space: hidden/blocked/private, autentificare Android și blocare imediată, după 30 secunde sau la stingerea ecranului.
- Cod separat în `data`, `platform` și module UI. Componentele Android vechi au adaptoare de compatibilitate.

## Documente și previzualizare

- `docs/FRONTEND_STRUCTURE.md`: arborele meniurilor și controalele Advanced.
- `docs/UI_IMPLEMENTATION_MATRIX.md`: corespondența celor 34 de cerințe cu implementarea.
- `docs/UI_STRUCTURE.md`: arborele proiectului și al meniului, cu comentarii `#`.
- `docs/ARCHITECTURE.md`: stare, cache-uri, fire de execuție și migrare.
- `docs/IMPLEMENTATION_STATUS.md`: funcții și limite exacte.
- `docs/CHANGELOG.md`: modificările față de v2.1.
- `docs/VALIDATION.md`: ce s-a verificat și verificările necesare pe dispozitiv.
- `docs/REQUESTED_UI.txt`: specificația frontend primită.
- `ui/components/VectorPackPreview.kt`: galerie Compose Preview a celor 54 de vectori.

Private Space și limitele protejează lansările prin EmmanueLA; nu blochează accesul prin alte suprafețe Android. Filtrarea notificărilor intervine după sosire. Traducerea navigării este disponibilă pentru EN/RO/DE/FR/ES/IT/PL; unele texte detaliate folosesc în continuare fallback englez. Nu se afirmă performanță 60/120 fps fără măsurători pe dispozitiv.
