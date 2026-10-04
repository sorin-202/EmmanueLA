## Stare 2.5.2

Final touch implementat în surse. PASS structural; Gradle/lint/JUnit/instrumentare/telefon NOT RUN. Vezi TESTING_FIXES_V252.md.

## Stare 2.5.1

Patch-ul este implementat în surse. Testele Gradle/dispozitiv nu au rulat. Hidden manager nu mai este protejat; autorizarea private/foldere rămâne. Causa crash-ului Edge-to-edge nu este confirmată fără Logcat; sunt corectate insets și limitele negative de layout. Vezi TESTING_FIXES_V251.md.

## Stare 2.5

Cerințele ultimei iterații sunt implementate în surse; vezi TESTING_FIXES_V25.md. Build, JUnit, lint, instrumentare și verificările pe telefon NU AU RULAT. Rezultatele vechi de mai jos rămân istorice.

# Stare release v2.4

Modificările de surse pentru raportul de testare sunt integrate; detalii și limite în `TESTING_FIXES_V24.md`. Arhiva este un proiect Android Studio complet, nu un APK validat. Nu se afirmă că toate comportamentele OEM sau performanța au fost confirmate pe telefon.

Mod avion: rutare la setarea Android, fără comutare privilegiată. Confort digital: destinație configurabilă în funcție de exporturile OEM. Contacte terțe: doar integrări efectiv expuse. Resize folder: dimensiuni individuale, lățime limitată de coloana layout-ului. Glass: transparență/contur, fără backdrop blur pe fiecare widget.

# Funcțiile existente din v2.3, păstrate

Implementarea continuă proiectul existent. Vezi UI_IMPLEMENTATION_MATRIX.md pentru cele 34 de puncte.

| Zonă | Cod inclus | Limite practice |
|---|---|---|
| Settings | General / Your space, rute dedicate, Standard/Advanced persistent, titluri și secțiuni | Necesită verificare vizuală pe dispozitiv |
| Motion | Standard/Fluid/Off, viteză, preview real de pagini, folder Off/Standard, alfabet Wave/Bubble/Fade/None | Fără măsurători 60/120 Hz |
| Home | Widgeturi, fullscreen arrange, pinch grafic, favorite fixe, bottom controls, alfabet independent | Pozițiile legacy rămân în dp intern, fără câmpuri de coordonate în UI |
| Apps | Alias/taguri, Add tag, reset, assign picker, cursor, search #, auto-launch și operații bulk | Testare IME/TalkBack necesară |
| Folders | List/Grid/Freeform magnetic, editor individual, PIN/parolă, fill/contrast, icon/text/grid | Freeform este magnetic pe celule, nu pixeli arbitrari; drag peste ecran necesită scroll separat |
| Live the Moment | Grupuri, daily allowance, schedule, opens/day, strict authentication, pauză progresivă, session limit | Enforcement la intrările launcher-ului; session reminder nu forțează închiderea app-ului și poate fi amânat de Android |
| Private Space | Hidden/Blocked/Private, Authentication selector, auto-lock, search visibility | Nu este Android Private Space OS/vault; alte suprafețe Android rămân independente |
| Wallpaper | Fullscreen pan/pinch/Fit/Fill, dim/blur, album și daily schedule | Blur Android 12+; rotația este aplicată când Home este vizibil, fără alarmă exactă |
| Notifications | Badges, filter, digest, reguli per-app | Sistemul poate alerta înainte de dismiss; accesul listener și permisiunea de notificare sunt separate |
| Language | EN/RO/DE/FR/ES/IT/PL, nume de limbi, mai multe controale RO | Traducerile detaliate au fallback englez, nu sunt toate integral traduse |
| Build | Wrapper și surse incluse; verificări structurale PASS | Compile/lint/JUnit/APK neconfirmate în cloud |

Arhiva conține surse pentru validare în Android Studio, nu un APK certificat. Nu sunt garantate FPS sau comportamente OEM fără testare pe telefon.
