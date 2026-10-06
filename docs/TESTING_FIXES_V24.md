# EmmanueLA 2.4 — corecții după testarea pe telefon

Actualizare țintită a proiectului v2.3 existent. Identificatorul `com.emmanuela.launcher`, DataStore-ul, schema de backup 7, paleta și cei 54 de vectori sunt păstrați. Nu s-a pornit un proiect nou.

## Cerințe și verificări de acceptanță

| Cerință raportată | Modificare în surse | Verificare pe dispozitiv |
| --- | --- | --- |
| System / Permissions | Ecran dedicat, stare actualizată la revenire, cereri runtime și rutare la App permissions după refuz permanent | Acordă/refuză/revocă Usage, Notifications, Contacts, Location, Camera, DND și Device Admin; verifică starea după revenire |
| Home / Text style | Preferințe Home separate de Appearance; temă Compose locală | Schimbă font/italic/weight pe Home; App List și Settings trebuie să păstreze stilul global |
| Italic și weight | Typography construită prin argumente numite; `FontSynthesis.All`; aplicare inclusiv textelor fără stil explicit | Testează Sans/Serif/Monospace și font importat, Regular/Medium/Bold și italic; verifică previzualizarea și Home |
| Wallpaper dim/blur | Controalele există numai în Wallpaper preview, inclusiv în modul Standard | Modifică slider-ele, Cancel nu salvează, Save persistă; Appearance nu dublează controalele |
| Lag după prima mutare | Stare de poziție/scală păstrată între salvări; un singur commit la sfârșitul drag/pinch | Mută fiecare widget de 20 de ori consecutiv; rotește dispozitivul; verifică poziția după restart |
| Fundal widget | Colțuri rotunjite, opacitate reglabilă și contur fin | Alege Background → White, opacitate 0.15–0.35 și colțuri 18–24 dp peste o fotografie |
| Preview controale de jos | Cadran fix 168 dp, etichetă Preview și conținut decupat în interior | Schimbă mărimea/greutatea/iconurile; cadrul și poziția celorlalte setări trebuie să rămână constante |
| Home Alphabet vs App List | Activarea dezactivează drawer-ul și reține starea lui anterioară; navigarea verifică politica paginilor | Activează alfabetul, încearcă toate gesturile către Apps, apoi dezactivează-l și verifică restaurarea |
| Wave/haptics Home | Pointer observat înaintea copiilor; fără clickable care să consume începutul; Wave 16–80 dp și tick la schimbarea literei | Glisează lent/rapid; verifică Wave/Bubble/Fade/None și haptics cu feedbackul sistemului activ |
| Scroll aplicații Home Alphabet | LazyColumn are prioritate pentru scroll vertical în zona ei | Pune peste 20 aplicații sub aceeași literă; derulează până la ultima, apoi lanseaz-o |
| Search package names | Predicate separate fără liste intermediare; caută pachetul și componenta/activity ID | Dezactivează autolansarea pentru inspecție; caută un pachet absent din nume cu opțiunea on/off |
| Căutare @contact | Contacte Phone implicit; nume + aplicația sursă, READ_CONTACTS și căutare IO cu debounce; selecție surse în Settings | Caută @nume, refuză/acordă/revocă accesul; atingerea contactului deschide dialer-ul fără a efectua automat un apel |
| Contacte în alte aplicații | Doar Data MIME handlers exportați și disponibili prin Android pentru aplicațiile selectate | Verifică aplicațiile care expun integrarea; selectarea unei aplicații nu inventează contacte inexistente |
| Selecție Layout/Cursor | Fundal și contur în culoarea accentului pentru opțiunea activă | Alege fiecare stil și verifică marcajul după redeschidere |
| Aplicații din folder securizat | Gating per pachet în ViewModel; dialog și reluare prin Home, drawer, alfabet, foldere și shortcut-uri Intent | Încearcă favorite, alias/search, contact/clock shortcut și gest; verifică și apartenența la două foldere protejate |
| Confort digital | Rezolvare independentă de eticheta localizată; destinație OEM selectabilă + Test shortcut | Settings → System → Digital Wellbeing destination; alege pagina exportată Confort digital, testează widgetul și gestul |
| Lanterna | CameraManager.setTorchMode, callback și cerere Camera numai dacă e necesară; acces serializat | Alternează on/off, testează camera ocupată, refuzul permisiunii și telefon fără flash |
| DND / avion / baterie | DND comută cu Policy access; Battery deschide destinația directă; airplane deschide pagina dedicată Android | Testează după acordarea accesului; pe Android 15 DND controlează regula implicită a aplicației |
| Weather tap | Action Picker pentru aplicație sau URL http/https salvat, cu Use website on tap | Configurează alternativ aplicație și site și verifică deschiderea fiecăruia |
| Hidden apps | Fallback la PIN/parola dispozitivului când biometria nu este disponibilă; mesaje explicite și pagină goală accesibilă | Testează fără biometrie, cu PIN, refuz/cancel, fără PIN și cu lista goală |
| Tranziții | Doar deplasare pe axa X + fade discret; easing Standard/Fluid, durate 200/280/360 ms, Off/Reduce motion | Rulează 100 de schimbări Apps ↔ Folders și măsoară FrameTimeline la 60/120 Hz |
| Pagini dezactivate | Home, App List și Folders independente; PagePolicy rezolvă orice destinație către o pagină activă | Testează toate cele 8 combinații; toate off → EmmanueLA, slogan, Note to self și Settings accesibile |
| Resize folder individual | Width/Height în editorul fiecărui folder, persistente; migrare cu valori implicite | Redimensionează un singur folder, verifică celelalte și export/restore; lățimea este limitată de coloana disponibilă |

## Limite Android și limitele validării

- Un launcher obișnuit nu poate comuta direct modul avion. Gestul deschide instant pagina Android pentru modul avion; nu pretinde activarea fără confirmare.
- DND necesită acces Notification Policy. Cu targetSdk 35 pe Android 15, `setInterruptionFilter` controlează regula implicită asociată aplicației, nu poate anula toate regulile altor aplicații.
- Digital Wellbeing nu are o activitate exportată identică pe toate telefoanele. Selectorul afișează activități exportate și fără permisiune proprie din pachetele Settings/Wellbeing cunoscute. Dacă producătorul nu expune pagina, nu poate fi forțată deschiderea ei.
- Protecția folderelor/private apps se aplică intrărilor prin EmmanueLA. Alte launchere, notificările și setările Android rămân în afara acestui control. Deblocarea respectă politica existentă Auto lock; pentru solicitare la fiecare revenire folosește Immediately.
- Fundalul de widget oferă aspect de sticlă prin transparență și contur. Nu include refracție sau blur live al fiecărui widget. Blur-ul wallpaper folosește suportul Compose/Android 12+, fără fallback software costisitor pe Android 8–11.
- Contactele nu sunt salvate în backup sau baze de date ale launcherului. Integrarea cu alte aplicații este limitată la ceea ce expune Contacts Provider + activități exportate.
- Haptics respectă setările Android și existența actuatorului. Nu se forțează vibrații când sistemul le dezactivează.
- Nicio performanță 60/120 fps și nicio compatibilitate OEM nu sunt declarate măsurate fără testare pe dispozitiv.

## Build și testare locală obligatorie

Nu există Android SDK, Gradle instalat sau compilator Kotlin disponibil în acest mediu. Încercarea de download Gradle din etapa anterioară a eșuat; logul istoric este păstrat în istoricul Git. Pentru acest release nu s-a rulat compilarea cloud, JUnit sau lint. Nu este inclus un APK verificat.

1. Extrage arhiva și deschide folderul EmmanueLA în Android Studio.
2. Selectează Gradle JDK 17 și instalează Android SDK Platform 35; fă Gradle Sync.
3. Din folderul proiectului, pe Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
.\gradlew.bat :app:installDebug
```

Pe Linux/macOS:

```sh
chmod +x gradlew
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:installDebug
```

APK debug după build reușit: `app/build/outputs/apk/debug/app-debug.apk`. Pentru actualizare fără pierderea datelor folosește aceeași applicationId și aceeași cheie de semnare ca versiunea instalată. Nu dezinstala versiunea existentă ca metodă de upgrade.

Teste noi: V24RegressionTest verifică politica paginilor, căutarea pachetului/componentelor, izolarea stilului Home, round-trip backup, migrarea și respingerea dimensiunilor invalide. Testele MotionPolicy au duratele actualizate. Aceste teste sunt incluse, nu executate în cloud.
