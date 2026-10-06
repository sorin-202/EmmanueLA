> Historical delivery notes. For current executed evidence and limitations, see [VALIDATION.md](VALIDATION.md) and [ENGINEERING_VALIDATION.md](ENGINEERING_VALIDATION.md). These notes are not the current task list.

# 2.6.0-beta

Focus quotas/windows and event-driven external enforcement; app/site/URL-keyword rules; Tree Branch drawer; dense variable-span folders; private-entry and folder device authentication; additional clock formats; notification rule refresh; contact deduplication. See UPGRADE_V26.md for limitations and validation status.

## 2.5.2

Location tap action persistent; About GitHub public; bottom controls independente de preset, fără Custom; acțiune adaptivă App List/Folders și fallback Apps ↔ Folders.

## 2.5.1

Patch: selecții spațiate, fără marker la switch/checkbox; folder drag overlay și snap; Hidden public manager, Blocked switch manager; prompt biometric creat la cerere; insets Scaffold și clamp widget.

## 2.5

- Stil independent pentru fiecare widget (font, cursiv, greutate, culoare, fundal, transparență și colțuri), cu preview fix și acces din Manage widgets. Poziția bottom controls este reglabilă și în modul Standard.
- Home și App List au amplitude Wave independente; controlul apare numai pentru Wave. Haptics comun, limitat la un impuls la minimum 35 ms, respectă preferința Android și folosește vibratorul dacă View refuză feedbackul. Permisiunea VIBRATE este declarată. Fiecare secțiune are Test haptics și acces la setările Android.
- Italic/weight aplicate prin Typeface nativ; fonturile importate sunt reutilizate din cache. Stilul Home explicit are prioritate față de Appearance.
- Wallpaper preview are panou ascundabil și salvare separată a crop/zoom/dimming/blur pentru fiecare fotografie din albumul zilnic.
- Hidden apps deschide lista separată de aplicații ascunse. Private apps lansează aplicația cu autorizare legată de invocarea respectivă; revenirea din autentificarea Android nu mai șterge prematur această autorizare.
- App management din avertizarea de limite deschide Live the moment, unde sunt grupurile.
- Arrange apps permite drag-and-drop magnetic în interiorul folderelor, cu ordine persistentă și auto-scroll doar pe durata tragerii. Folderul tras este ridicat deasupra celorlalte elemente ale gridului.
- About include Share the app pentru APK-ul instalat. Selecțiile au marker comun cu accent; subcategoriile widgeturilor sunt indentate și marcate ↳.
- Pagina Folders afișează Settings în loc de Home când Home este dezactivat, inclusiv când App List este dezactivat.


# v2.4 — feedback de testare

Actualizare a v2.3: stil Home separat și sinteză italic/weight; stabilizarea drag-ului cu commit atomic; glass widget background și preview fix; Home Alphabet înlocuiește drawer-ul, Wave/haptics și scroll nativ; @contacts și căutare după componentă; permisiuni reactive, lanternă/DND reale, selector Wellbeing, website weather; deblocarea aplicațiilor din foldere securizate și fallback PIN; PagePolicy + ecran Blank; dimensiuni per-folder; animație orizontală standard; teste de regresie și texte EN/RO.

Nu s-a schimbat applicationId, paleta, biblioteca vectorială sau schema de backup. Compilarea rămâne de executat local.

# v2.3

- Finalizarea paginilor și secțiunilor din REQUESTED_UI.txt pe baza v2.2.
- Conectarea animației folderelor și separarea Home Alphabet de drawer.
- Politică comună pentru Motion / Reduce motion și preview de pagini.
- Selector Language, Action Picker grupat, Folder assignments, Blocked app și Authentication.
- Grupuri: pauză progresivă, session limit, autentificare configurabilă; remindere fără polling.
- Atomicitate în editorul contextual, deblocare directă la atribuirea folderelor, închidere numai după lansare reușită.
- Pinch cu transformare grafică, margini notch, text HEX și traduceri RO suplimentare.
- Teste JUnit noi pentru sesiuni, escaladare, serializare și politica animațiilor; nu executate în cloud.

# v2.2 — 2026-10-01

Actualizare pornită din `EmmanueLA_v2_1_refactor(1).zip`.

- Separare fizică a codului în data / platform / ui și extracția Home, Apps și Folders din root UI.
- Arbore nou de setări și Advanced persistent; pagina și scrollul rămân aceleași la comutare.
- Eliminarea frontend-ului duplicat V2SettingsUi și a dialogului de coordonate numerice.
- Alias/tag/folder editor dedicat cu commit atomic; gestionează aplicațiile prin identități stabile.
- Câmp search cu cursor custom real, debounce și autolansare configurabilă.
- Opțiuni drawer pentru iconițe, spacing, alignment și size; Home alphabet cu index precompute.
- Aranjarea widgeturilor în preview-ul Home complet; drag/pinch și opțiuni pe widget.
- Wallpaper full-screen și album, sort/order, blur/dim advanced.
- FocusGroup + codec + launch gates: pauză, limits, schedule, opens/day, strict edit authentication.
- Private Space cu BiometricPrompt/Keyguard, hidden search controls și auto-lock.
- Păstrarea receiver/service identities și adaptarea worker-ului migrat.
- Eliminarea scanării catalogului la fiecare onResume; package updates rămân reactive.
- 30 vectori outline noi și galerie Compose Preview; paleta originală păstrată.
- Resurse de navigare multilingve, cu fallback englez pentru textele netraduse.
- Teste noi pentru migrare, codec, hidden search, switches de căutare și schedule overnight.

Build/lint/unit tests: neexecutate din cauza descărcării Gradle blocate. Limitele exacte sunt în IMPLEMENTATION_STATUS.md.
