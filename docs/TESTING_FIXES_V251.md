# EmmanueLA 2.5.1 — patch al versiunii 2.5

Această versiune păstrează pachetul, datele, paleta și iconițele versiunii 2.5. versionCode 12, versionName 2.5.1. Nu este o reconstrucție.

## Modificări

- Markerul unei selecții din listă are 12 dp spațiu interior orizontal și 6 dp vertical. Cursorul folosește același marker. Switch-urile și checklist-ul de apartenență la foldere nu mai au chenar suplimentar.
- Folderul tras este un preview separat, deasupra LazyVerticalGrid. Elementul original este invizibil pe durata dragului, dar păstrează spațiul. La drop, preview-ul se animă la țintă, iar ordinea se salvează. Preview-ul rămâne până când starea persistentă reflectă mutarea; un timeout de 1,5 secunde îl elimină dacă salvarea nu se finalizează. Fără polling continuu. Cancel revine fără mutare. Folder animation Off și Reduce motion dezactivează animația de drop.
- Hidden apps este acum o pagină de administrare fără autentificare: toate aplicațiile, inclusiv cele ascunse, pot fi afișate/ascunse cu switch. Aplicațiile ascunse apar primele. Ascunderea singură nu mai necesită un PIN Android; Private apps rămâne protejat.
- Blocked apps are switch direct în listă, cu aplicațiile blocate primele. Nu mai deschide o pagină de detaliu pentru activare/dezactivare.
- Promptul biometric nu mai este creat de fiecare panou inactiv. Numai panoul care începe autentificarea înregistrează callback-ul. Aceasta corectează conflictul de callback-uri între Root, Settings și Private Manager. Succesul continuă acțiunea originală; anularea nu autorizează nimic. Există fallback la credential Android când biometria nu este disponibilă.
- Auto-lock nu mai șterge autorizarea la întoarcerea imediată din promptul Android. Screen-off continuă să relocheze accesul.
- Edge-to-edge folosește insets prin Scaffold și consumă padding-ul o singură dată în root, evitând înlocuirea ramurii de modificatori de layout. Limitele coordonatelor widgeturilor nu pot deveni negative pe suprafețe mici. Fără logcat nu este confirmată cauza exactă a crash-ului raportat; verificarea pe telefon este obligatorie.

## Verificat aici

Checker structural Kotlin/XML/manifest: PASS. Resurse XML: PASS. Wrapper JAR și ZIP: integritate verificată. Arhiva corespunde fișierelor sursă.

NOT RUN: compilare Kotlin/Android, lint, JUnit, instrumentare, teste pe telefon. SDK Android/Gradle nu sunt instalate. Aceste verificări structurale nu detectează erori de tip Kotlin sau probleme runtime. Arhiva este de surse, nu un APK verificat.

## Validare locală

Extrage proiectul, deschide folderul EmmanueLA în Android Studio, selectează JDK 17, instalează SDK 35 și finalizează Gradle Sync.

Windows PowerShell:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:installDebug
```

macOS/Linux: înlocuiește `.\gradlew.bat` cu `./gradlew`. Connected tests necesită telefon/emulator conectat. APK: `app/build/outputs/apk/debug/app-debug.apk`. Update peste versiunea instalată necesită aceeași semnătură.

| Test | Rezultat așteptat |
| --- | --- |
| Choice/font/cursor/action | Chenarul nu atinge textul, selecția se vede prin accent. |
| Switch/checkbox | Fără chenar în jurul întregului rând, indiferent de On/Off. |
| Folders List/Grid/Freeform | Trage peste alte foldere și sloturi goale; preview-ul rămâne deasupra gridului, drop-ul salvează ordinea și restartul o păstrează. |
| Folders cancel/off | Cancel nu mută folderul. Animation Off/Reduce motion omit animația de drop. |
| Hidden apps | Pagina se deschide direct, fără PIN/biometrie; arată toate aplicațiile, cele ascunse primele. Switch-ul este reversibil și starea persistă după restart. |
| Blocked apps | Cele blocate primele; switch-ul schimbă direct regula, fără pagină suplimentară. |
| Unlock hidden | App settings în Advanced → Unlock hidden → biometrie: aplicațiile ascunse apar în App management. Repetă cu credential Android, apoi cu cancel. |
| Private apps | Cu Biometric + device credential, autentificarea continuă lansarea aplicației selectate. Repetă din lista private și App List. Cancel nu lansează. |
| Protected folder | O aplicație privată aflată și într-un folder protejat păstrează verificarea parolei folderului. |
| Edge-to-edge | On/Off repetat în Settings; Home/Folders/App List, tastatură vizibilă/ascunsă, rotație, multi-window și ecran mic. Fără crash sau padding duplicat. |
| Security | Screen-off relochează. Revenirea din autentificare nu pierde imediat autorizarea. |
| Regression | Widgeturi independente, italic/weight, wallpaper per-photo, haptics și căutarea #/@ rămân funcționale. |

Dacă Edge-to-edge încă produce crash: atașează stacktrace-ul AndroidRuntime din Logcat, modelul telefonului, versiunea Android și pașii exacți. Schimbările actuale tratează riscurile de layout identificate, fără a pretinde că substituie diagnosticul pe dispozitiv.
