# Current architecture — 2026-10-06

The existing single-module Compose launcher remains intact. Pure search/focus/configuration rules live in data; Android services and repositories live in platform; feature screens live under ui. LauncherViewModel is still a large coordinator, a maintainability limitation rather than a reason for an unvalidated rewrite.

Configuration v9 reads versions2–9, validates imports and commits updates atomically. Break grants persist only for their bounded session; intentions and launch authentication are transient. Notification content and browsing history are not stored. App admission uses the shared authorization path.

Index preparation runs on Default. M6 verified prepared query/name tokens and release resource shrinking; see PERFORMANCE.md for before/after measurements and environment limits. Drag state remains transient until meaningful release; cancellation restores the starting position. Navigation rejects consumed cancellation events. Overlay semantics hide obscured Home controls.

DataStore1.1.7 has upstream issue431787506: a newly starting collector concurrent with a write can miss that update. A fresh read sees the saved value. Continuous UI/test observers avoid the reproduced startup window, but this is a documented dependency limitation; an upstream stable fix should be adopted when available. No production polling workaround or alpha upgrade was added solely for tests.

The following sections are retained delivery history. Their schema/test-environment statements are superseded by this section and VALIDATION.md.
## Patch 2.5.2

V2Preferences adaugă locationAction opțional cu No action legacy. Codecurile Configuration/Ui/V2 acceptă drawer. PagePolicy.drawer alege o pagină drawer activă înainte de Home/blank; resolve aplică fallback și acțiunilor vechi. bottomSize Custom se normalizează la Medium fără pierderea valorilor Width/Height/Offset.

## Patch 2.5.1

BiometricPrompt este construit doar când începe autentificarea, evitând schimbarea callback-ului activ de către panouri inactive. Hidden manager nu oferă hiddenUnlocked automat; App Management păstrează Unlock hidden autentificat. FolderGrid desenează ghost-ul în Box, sibling cu LazyVerticalGrid; pe drop animă ținta, face commit și eliberează ghost-ul când DataStore reflectă starea. Insets sunt aplicați prin Scaffold și consumați în root.

## Arhitectură v2.5

WidgetStyle și PhotoStyle sunt map-uri opționale în V2Preferences, cu fallback la stilurile legacy. DataStore serializează numai commit-urile; dragul ține poziția temporară în Compose. TypefaceCache încarcă fontul importat pe IO și partajează fața de bază. NativeTypeface realizează italic/weight înainte de FontFamily.

Private authorization este legată de pachet și invocarea launch, nu de un set global care putea fi șters de onStop în timpul autentificării. Folder password și hidden checks rămân în calea comună de lansare.

ReorderRules păstrează pozițiile membrilor ascunși sau indisponibili. MagneticFolderApps folosește bounds cache, overlay ridicat, ordine optimistă și un singur commit pe drop; auto-scroll folosește frame time doar în timpul dragului. LauncherHaptics observă preferința sistemului prin ContentObserver, cache-uiește efectul și vibratorul, limitează impulsurile și eliberează observerul la părăsirea compoziției. Nu are loop de polling.

# Extensii v2.4

- `PagePolicy` este sursa comună pentru paginile permise. Orice gest/button/Home intent trece prin resolve; `BLANK` are Settings și Note to self când lista activă este goală.
- `ExperiencePreferences` adaugă stil Home, opacitate/colțuri, Wave strength, surse contacte, destinație OEM Wellbeing și URL weather. Câmpurile JSON sunt opționale pentru configurațiile vechi. `AppFolder` adaugă widthDp/heightDp cu migrare 0/150 dp.
- `LauncherTypography` construiește toate stilurile Material prin argumente numite și sinteză de font. Tema locală Home nu rescrie setările globale. Typeface-ul extern se citește pe IO, doar la schimbarea fontului/stamp-ului.
- Widgeturile rețin aceeași stare de poziție/scală în timpul editorului. Pan-ul actualizează transformarea graphicsLayer; la sfârșit se salvează o singură transformare poziție+scale. Nu se scrie pe disc la fiecare frame.
- `GestureRegions` folosește hărți obișnuite pentru bounds, fără stare Compose per element. `ownsVerticalScroll` rezervă axa verticală a listei Home, păstrând gesturile orizontale. Rail-ul consumă pointer-ul în Initial, înaintea copiilor; wave-ul este calculat în graphicsLayer.
- AppSearchIndex precomputează label/original/package/component și indexul tagurilor pe Default; filtrarea numelor nu mai construiește o listă de nume la fiecare app/token. ContactSearchRepository folosește IO, READ_CONTACTS, debounce 120 ms, rezultate limitate și integrare MIME exportată. Nu persistă contacte; rezultatele se șterg la revocarea permisiunii/modului.
- SystemActionController înregistrează un TorchCallback, folosește setTorchMode fără sesiune de cameră și serializează schimbările. DND cere Policy access. Selectorul OEM evită dependența de numele tradus al Digital Wellbeing.
- Gating-ul central verifică toate folderele care conțin pachetul. ProtectedShortcut păstrează Intent-ul original în memorie până la autorizare; UI cere parolele necesare și autentificarea private/hidden, apoi reia aceeași acțiune. Autorizațiile nu se serializează în backup.
- PermissionSettings actualizează stările la ON_RESUME. Refuzul permanent duce la App permissions; autentificarea indisponibilă oferă fallback la credentialele Android sau explicație pentru lipsa PIN-ului.

# Arhitectura actualizării v2.3

## Stare și persistență

`ConfigurationRepository` păstrează documentul versionat într-un singur Preferences DataStore existent (`ema_configuration`). Nu se introduc două baze de date concurente. `LauncherData` include settings, favorites, folders, metadata, policies și focusGroups. Scrierile și validarea JSON se execută pe `Dispatchers.IO`; o configurație invalidă este respinsă înainte de commit. Schema 7 importă schemele 2–7. Digest-ul activ din configurațiile vechi rămâne activ la migrare.

Cheia pentru aliasuri și taguri este componenta launchable stabilă `package/activity`, nu numele afișat. Politicile sunt indexate după package, pentru a acoperi toate activitățile aceleiași aplicații. `saveAppDetails` salvează aliasul, tagurile și toate apartenențele la foldere atomic. Aliasurile sunt decorate înainte de construirea indexului de căutare și apar și în ecranul de pauză.

`LauncherViewModel` expune StateFlow-uri. Compose le colectează cu lifecycle. Modul Advanced este separat de ruta și scrollul paginii. Datele de autentificare, aprobările de lansare și pauzele sunt tranzitorii și nu intră în backup. Folderele au hash PBKDF2 și salt; autentificarea privată folosește credentialele Android, nu un PIN stocat de launcher.

## Indexuri, cache și execuție

- PackageManager este interogat pe IO, la pornire și la broadcast-uri de instalare/actualizare/dezinstalare; nu la fiecare revenire dintr-o aplicație.
- Aliasurile decorate și indexurile sunt calculate pe Default. Indexul drawer-ului se reconstruiește doar la modificarea catalogului, metadatelor sau vizibilității relevante.
- Căutarea are debounce 60 ms și rulează pe Default. Tagurile folosesc index inversat și căutare de prefix în colecția sortată; alfabetul are poziții precompute pentru touch.
- Iconițele sunt încărcate doar pentru celulele vizibile, într-un LRU de 64 bitmap-uri de 96×96 (aproximativ 2,4 MB). WallpaperCache este existent și bounded. Nu se promit bitmap-uri hardware pentru drawables vectoriale.
- UsageStats se citește la revenire, tick-ul minutului când launcher-ul este vizibil și la verificarea regulilor de lansare. Nu există buclă de polling în fundal.
- NotificationListener reacționează la evenimente. WorkManager verifică digest-ul la intervalul minim de 15 minute; intervalul ales controlează când un sumar este eligibil, fără promisiunea unei ore exacte în Doze.
- Weather este opt-in, cu cache orar. Căutarea orașului este debounced, pe IO, cu timeout. Widgetul de locație folosește ultima locație cunoscută, fără abonament GPS continuu.

## Gesturi și animație

Dispatcher-ul observă touch-ul pe suprafața completă în Initial pass, apoi revendică gestul numai după stabilirea axei. Search, alfabetul și alte controale cu gesturi proprii au regiuni de excludere. Pe drawer se păstrează scrollul vertical; swipe stânga comută Apps/Folders, dreapta revine Home. Widget dragging este activ numai în editor și nu concurează cu navigarea globală.

Tranzițiile sunt orizontale, fără traseu arcuit; Standard folosește cubic-bezier (.2,0,0,1), Fluid folosește FastOutSlowIn; deplasarea rămâne pe o linie orizontală, Off/Reduce motion au durată zero. Coordonatele drag-ului sunt citite în `graphicsLayer`; persistența se face la finalul gestului. Pinch folosește transformarea graphicsLayer până la commit; la salvare se recalculează dimensiunea textului. Nu se afirmă „zero layout passes” pentru întregul UI. Animațiile Compose folosesc frame clock-ul; nu sunt prezentate ca execuție exclusiv pe RenderThread.

## Fluxul unei lansări

1. UI cere autentificare pentru hidden/private dacă este necesar.
2. ViewModel verifică ascunderea, autorizarea privată, block, program și daily allowance.
3. Pentru grupurile Live the Moment se verifică totalul utilizării aplicațiilor, programul și deschiderile zilnice.
4. Dacă există pauză, se afișează ecranul Take a moment. După countdown, utilizatorul apasă explicit Open app.
5. Confirmarea verifică din nou regulile înainte de lansare. Intențiile shortcut-urilor sunt păstrate prin pauză.

Componentele `LockAdminReceiver`, `EmmanuelaNotificationListener` și vechiul `DigestWorker` păstrează numele Android originale prin adaptoare; implementările sunt mutate în `platform`. WorkManager folosește UPDATE pentru a migra joburile existente. Application ID și numele DataStore rămân identice cu v2.1.

## Completări v2.3

`MotionPolicy` centralizează durata/easing și anularea efectelor decorative. `folderAnimation` controlează AnimatedVisibility pentru intrarea/ieșirea folderului. `successfulLaunches` închide folderul numai după lansare reușită; refuzul regulilor și anularea autentificării păstrează folderul deschis.

Home Alphabet are preferințe separate, cu valori implicite pentru backup-urile anterioare. `textColor` este utilizat în Theme și Home; culoarea explicită a widgetului are prioritate.

FocusCodec persistă `sessionMinutes`, `escalatingPause`, `requireAuthentication`. Întârzierea crește o dată la trei deschideri, până la 60 secunde. SessionMath unește intervalele activităților grupului și permite o sesiune nouă după o pauză de cel puțin 60 secunde. La o tentativă de redeschidere se verifică limita. Worker-ul de reminder rulează o singură dată la termen, recitește configurația și notifică doar dacă grupul încă este în utilizare și limita este atinsă. Nu este un mecanism de terminare a aplicațiilor externe. Cererile WorkManager au tag pentru anularea la reset și nume păstrat în ProGuard.

Authentication permite biometric + device credential sau credential exclusiv. Pe Android 8–10 se folosește credentialul Android. FolderMembershipPicker reutilizează dialogul de deblocare pentru toate ecranele de apartenență.
