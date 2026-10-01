## Interfață 2.5

Manage widgets → widget-style:<id> → preview și controale independente; Widget options păstrează tap action. Wallpaper editor → Photo N → draft independent → Save, Hide/Show controls. Private Space → Hidden apps (listă doar hidden) / Private apps (launch autentificat și link manager). Folders → Arrange apps; footer Settings când Home off. Toate alegerile Settings folosesc marker de accent comun.

# Adăugiri frontend v2.4

```text
System / Permissions                         # acces runtime și special; stare la revenire
System / Digital Wellbeing destination       # pagină OEM selectabilă, testare shortcut
Home / Enable Home                           # independent de celelalte pagini
Home / Text style                            # stil exclusiv pentru Home, preview local
Home / Widget style / Background opacity     # transparență 0–100%
Home / Widget style / Rounded corners        # colțuri 0–40 dp
Home / Bottom controls / Style & size        # preview fix 168 dp
Home / Home alphabet / Wave movement         # amplitudine 16–80 dp, drag real
App List / Contact search (@)                # Phone implicit, surse Android selectabile
Folders / editor / This folder's size        # lățime și înălțime pentru un singur folder
Wallpaper preview / Dimming & Blur           # disponibile numai aici, fără duplicare
Blank / EmmanueLA / Note to self / Settings   # când toate cele trei pagini sunt off
```

Ierarhia anterioară este păstrată mai jos. Controalele Advanced sunt în continuare accesibile prin ◇/◆.

# Structura frontend v2.3

Arborele descrie rutele principale. `# Advanced` marchează controalele afișate prin ◆. Pagina și scrollul rămân păstrate la comutare.

```text
SETTINGS                                      # ◇ / ◆ persistent pe paginile Settings
├── General                                   # Categorii mari, fără carduri individuale
│   ├── System
│   │   ├── Default launcher                  # Selector Android
│   │   ├── Language                          # Pagină cu numele limbilor și selecție ✓
│   │   ├── Permissions                       # Usage, listener, lock, location, notifications
│   │   ├── Battery optimization              # Setări Android
│   │   └── Backup & restore                  # JSON / reset cu confirmare
│   └── About
│       ├── EmmanueLA / version
│       ├── Quick guide / FAQs
│       ├── GitHub                            # Placeholder până există URL public
│       ├── Privacy
│       └── Version information
└── Your space
    ├── Home
    │   ├── Layout
    │   │   ├── Favorite apps / count          # 0–8, ordonare și selecție
    │   │   ├── Alignment / Spacing
    │   │   └── Text style                     # Advanced
    │   ├── Widgets
    │   │   ├── Manage widgets                 # Icon mic + nume + switch
    │   │   │   ├── Clock / Date / Weather
    │   │   │   ├── Screen time / Battery / Location
    │   │   │   └── Widget detail              # Enabled, Location dacă Weather, Appearance, Tap action
    │   │   ├── Arrange widgets                # Preview fullscreen; favorite fixe; drag/pinch/tap
    │   │   └── Widget style                   # Advanced; font/color/background/scale
    │   ├── Bottom controls
    │   │   ├── Left action / Right action     # Action Picker comun
    │   │   ├── Style / icon picker / Size     # Preview live
    │   │   └── Weight / Position / Custom size # Advanced
    │   ├── System
    │   │   ├── Status bar
    │   │   └── Edge-to-edge                   # Advanced
    │   └── Home alphabet                      # Advanced
    │       ├── Enabled / Style / Position
    │       ├── Animation / Haptics            # Separate de App List
    │       └── ♡ / # / A–Z                    # Afișează aplicații în zona favoritelor
    ├── App List
    │   ├── Layout                             # Alignment, icons, spacing; text size Advanced
    │   ├── Search                             # Position; style vizual 2×2; keyboard; auto-open
    │   │   ├── Delay / aliases / tags / packages # Advanced
    │   │   └── Cursor                         # Advanced; previews, custom character, color, blink
    │   └── Alphabet                           # Enabled; Animation/Haptics Advanced
    ├── Folders
    │   ├── Layout                             # List/Grid/Freeform, icons, spacing; sort Advanced
    │   ├── Appearance                         # Fill; label size Advanced
    │   ├── Behavior                           # Close after successful launch; animation Advanced
    │   ├── Security                           # Advanced; locked folders
    │   └── Folder editor                      # Name, vector, individual color, apps, layout, lock
    ├── Appearance
    │   ├── Theme                              # System/Light/Dark; Custom Advanced
    │   ├── Colors                             # Accent; HEX accent/text Advanced
    │   ├── Typography                         # Font/size/weight/italic; import/opacity Advanced
    │   ├── Wallpaper
    │   │   ├── Choose / Adjust                # Fullscreen drag/pinch; Fit/Fill/Reset/Cancel/Save
    │   │   ├── Dim / Blur                     # Advanced
    │   │   └── Daily wallpaper                # Advanced; enabled/album/time/order
    │   └── Motion                             # Off/Standard/Fluid; speed/reduce Advanced
    ├── Gestures                               # Diagramă direcții + Action Picker comun
    │   ├── Up / Down / Left / Right / Double tap / Hold
    │   └── Triple tap / sensitivity / haptics # Advanced
    ├── Apps
    │   ├── App settings                       # Search, app detail; bulk Advanced
    │   │   └── App detail                     # Alias/tags/folders/hidden/blocked/private
    │   │       └── Usage/notifications/reset  # Advanced
    │   ├── Aliases & tags                     # ~ alias; editor, chips, Add tag, reset, folders
    │   ├── Folder assignments                 # Search + app/folders; SELECT FOLDERS picker
    │   └── Notifications                      # Badges/filter/digest/system settings
    │       └── Interval/per-app rules         # Advanced
    ├── Mindful Use                            # Dashboard Today/goal/most used/Wellbeing
    │   └── Goal/warning/reminder/widget        # Advanced
    ├── Live the Moment                        # Grupuri cu numele app-urilor și limita comună
    │   └── Group                              # Apps, pause, daily allowance, schedule, strict
    │       ├── Delay / repeated-open increase # Advanced
    │       ├── Session / opens per day        # Advanced; enforcement prin launcher
    │       ├── Days / start / end             # Când schedule este activ
    │       └── Prompt / authentication        # Advanced
    └── Private Space
        ├── Hidden apps                        # Search + switch; hide from search Advanced
        ├── Blocked apps                       # Search → app detail → switch
        ├── Private apps                       # Search + switch; Android authentication la launch
        └── Security                           # Method; selector/auto-lock/search Advanced

CHOOSE ACTION                                  # Același picker pentru gestures/widgets/bottom
├── Launcher                                   # Home / App List / Folders / Search / Settings
├── Apps                                       # Open app… → search
├── System                                     # Android shortcuts și screen lock
└── Other                                      # None
```

Dimensiuni: titlu 28sp, secțiune 16sp sau categorie principală 22sp, rând 16sp, valoare 14sp. Fontul global și scala utilizatorului se aplică peste aceste valori. Margini safeDrawing pentru titluri/notch, separatoare discrete, fără card pe fiecare setare.
