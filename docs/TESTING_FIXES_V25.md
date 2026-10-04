# EmmanueLA 2.5 — actualizarea proiectului existent

Sursele păstrează identitatea Android, configurația existentă, paleta și biblioteca de 54 de vectori. Nu s-a reconstruit proiectul.

## Modificări implementate în surse

- Stil independent pentru fiecare widget (font, cursiv, greutate, culoare, fundal, transparență și colțuri), cu preview fix și acces din Manage widgets. Poziția bottom controls este reglabilă și în modul Standard.
- Home și App List au amplitude Wave independente; controlul apare numai pentru Wave. Haptics comun, limitat la un impuls la minimum 35 ms, respectă preferința Android și folosește vibratorul dacă View refuză feedbackul. Permisiunea VIBRATE este declarată. Fiecare secțiune are Test haptics și acces la setările Android.
- Italic/weight aplicate prin Typeface nativ; fonturile importate sunt reutilizate din cache. Stilul Home explicit are prioritate față de Appearance.
- Wallpaper preview are panou ascundabil și salvare separată a crop/zoom/dimming/blur pentru fiecare fotografie din albumul zilnic.
- Hidden apps deschide lista separată de aplicații ascunse. Private apps lansează aplicația cu autorizare legată de invocarea respectivă; revenirea din autentificarea Android nu mai șterge prematur această autorizare.
- App management din avertizarea de limite deschide Live the moment, unde sunt grupurile.
- Arrange apps permite drag-and-drop magnetic în interiorul folderelor, cu ordine persistentă și auto-scroll doar pe durata tragerii. Folderul tras este ridicat deasupra celorlalte elemente ale gridului.
- About include Share the app pentru APK-ul instalat. Selecțiile au marker comun cu accent; subcategoriile widgeturilor sunt indentate și marcate ↳.
- Pagina Folders afișează Settings în loc de Home când Home este dezactivat, inclusiv când App List este dezactivat.

## Validare locală obligatorie

În acest mediu nu există SDK Android/Gradle instalat. Compilarea, lint, JUnit, testele instrumentate și performanța pe dispozitiv nu sunt confirmate. Verificarea structurală nu detectează erori de tip Kotlin sau incompatibilități de API.

1. Extrage arhiva și deschide folderul EmmanueLA în Android Studio; configurează JDK 17 și Android SDK 35. Gradle Sync necesită acces la Google Maven, Maven Central și services.gradle.org.
2. Din rădăcina proiectului, pe Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:installDebug
```

Pe macOS/Linux folosește `./gradlew` în loc de `.\gradlew.bat`. Testele connected necesită un telefon/emulator conectat. APK-ul rezultat este `app/build/outputs/apk/debug/app-debug.apk`.

3. Testează fiecare rând de mai jos pe dispozitiv înainte de distribuție. Actualizarea peste versiunea instalată necesită aceeași semnătură.

| Zonă | Verificare pe telefon |
|---|---|
| Widgeturi | Schimbă doar Clock, verifică celelalte widgeturi neschimbate; preview, reset, rotație și restart; drag repetat fără salvări la fiecare pixel. |
| Fonturi | Cursiv și weight în Appearance, Home override și fiecare widget; font importat .ttf/.otf; API 26 și API 28+. |
| Bottom controls | Standard/Advanced, slider poziție, preview cu dimensiune fixă, controale mari fără schimbarea înălțimii paginii Settings. |
| Alphabet | Home/App List, Wave/Bubble/Fade/None; amplitudine apare doar pentru Wave; scroll pe o secțiune cu 20+ aplicații; Reduce motion. |
| Haptics | Activează vibrația tactilă în Android și toggle-ul secțiunii; Test haptics, atingere repetată a aceleiași litere Home, drag pe litere active și gesturi. Dezactivează toggle-ul secțiunii apoi vibrația Android: rail/gesturi nu trebuie să vibreze. Testul explicit poate vibra cu toggle-ul secțiunii oprit, însă respectă setarea Android. |
| Wallpaper | Selectează fiecare fotografie, salvează separat Fit/Fill, crop/zoom/dim/blur; ascunde/arată panoul; verifică rotația zilnică. Save păstrează editorul deschis; Done nu salvează un draft neconfirmat. |
| Private space | Hidden arată numai aplicațiile ascunse; PIN/biometrie, anulare, revenire Home, screen-off; private app se lansează după autentificare; parolă folder încă solicitată. |
| Live the moment | Depășește limita și apasă App management; se deschide Live the moment cu grupurile. |
| Foldere | Arrange apps list/grid, magnetic snap, auto-scroll, restart și schimbare alias; aplicațiile ascunse/indisponibile își păstrează pozițiile. Folderul tras rămâne peste grid. |
| Settings | Selecții cursor/font/layout/acțiuni/limbă; ierarhia Widgets și subcategorii; Home și App List off lasă Settings accesibil în Folders. |
| Share | About → Share the app, alege destinatarul și verifică APK-ul primit. Exportul este APK-ul de bază instalat; nu exportă toate split APK-urile unei distribuții bundle. |
| Migrare | Import/export configurație veche, aliasuri/taguri/PIN-uri/ordine; datele dispozitivului și fonturile/wallpaperurile necesită reselectare pe alt telefon. |
| Performanță | Perfetto/FrameTimeline la 60/120 Hz, scroll/drag/100 tranziții; verifică frame time, memorie și consum. Nu există o garanție de zero frame drops fără măsurare. |

## Limite de platformă

Permisiunile Usage Access, Accessibility/Admin și Notification Listener trebuie acordate explicit. Haptics depinde de hardware și politica Android; mesajul „sent” confirmă apelul API, nu vibrația fizică. Autorizarea launcherului nu împiedică deschiderea aplicațiilor din alte launchere/intents. Textul cursiv poate depinde de capabilitățile fontului importat. Share APK nu execută publicare sau trimitere fără selectarea destinatarului de către utilizator.
