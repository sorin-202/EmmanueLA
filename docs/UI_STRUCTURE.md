# Complete project structure — 2.6.0-beta

Comments are delimited with `#`. This lists the actual packaged files.

```text
EmmanueLA/ # Android Studio project root #
  .github/ # Project directory #
    workflows/ # Project directory #
      android.yml # Project asset / configuration #
  .gitignore # Project asset / configuration #
  README.md # Documentation #
  app/ # Project directory #
    build.gradle.kts # Gradle build configuration #
    proguard-rules.pro # Project asset / configuration #
    src/ # Project directory #
      androidTest/ # Project directory #
        java/ # Project directory #
          com/ # Project directory #
            emmanuela/ # Project directory #
              launcher/ # Project directory #
                NativeTypographyTest.kt # Kotlin implementation or test #
      main/ # Project directory #
        AndroidManifest.xml # Android resource / component configuration #
        java/ # Project directory #
          com/ # Project directory #
            emmanuela/ # Project directory #
              launcher/ # Project directory #
                LauncherUi.kt # Kotlin implementation or test #
                LauncherViewModel.kt # Kotlin implementation or test #
                MainActivity.kt # Kotlin implementation or test #
                SystemComponentCompatibility.kt # Kotlin implementation or test #
                data/ # Project directory #
                  AlphabetIndex.kt # Kotlin implementation or test #
                  AppMetadata.kt # Kotlin implementation or test #
                  AppPolicy.kt # Kotlin implementation or test #
                  AppSearchIndex.kt # Kotlin implementation or test #
                  Configuration.kt # Kotlin implementation or test #
                  ExperiencePreferences.kt # Kotlin implementation or test #
                  FocusGroups.kt # Kotlin implementation or test #
                  FocusWindows.kt # Kotlin implementation or test #
                  FolderGridLayout.kt # Kotlin implementation or test #
                  FolderSecurity.kt # Kotlin implementation or test #
                  Folders.kt # Kotlin implementation or test #
                  ReorderRules.kt # Kotlin implementation or test #
                  SurfaceStyles.kt # Kotlin implementation or test #
                  UiPreferences.kt # Kotlin implementation or test #
                  V2Preferences.kt # Kotlin implementation or test #
                platform/ # Project directory #
                  AppUsage.kt # Kotlin implementation or test #
                  Apps.kt # Kotlin implementation or test #
                  ContactSearch.kt # Kotlin implementation or test #
                  DeviceAuthentication.kt # Kotlin implementation or test #
                  FocusSessions.kt # Kotlin implementation or test #
                  LauncherHaptics.kt # Kotlin implementation or test #
                  NotificationBadges.kt # Kotlin implementation or test #
                  NotificationFiltering.kt # Kotlin implementation or test #
                  Platform.kt # Kotlin implementation or test #
                  RuleEnforcementService.kt # Kotlin implementation or test #
                  SystemActions.kt # Kotlin implementation or test #
                  TypefaceCache.kt # Kotlin implementation or test #
                  WallpaperCache.kt # Kotlin implementation or test #
                  WeatherRepository.kt # Kotlin implementation or test #
                ui/ # Project directory #
                  appearance/ # Project directory #
                    AppearanceControls.kt # Kotlin implementation or test #
                    LauncherTypography.kt # Kotlin implementation or test #
                    Theme.kt # Kotlin implementation or test #
                    WallpaperEditor.kt # Kotlin implementation or test #
                    WeatherLocationPicker.kt # Kotlin implementation or test #
                  apps/ # Project directory #
                    AppDetails.kt # Kotlin implementation or test #
                    AppManagementUi.kt # Kotlin implementation or test #
                    AppsScreen.kt # Kotlin implementation or test #
                    BranchAlphabetRail.kt # Kotlin implementation or test #
                    BulkActions.kt # Kotlin implementation or test #
                    CursorSearchField.kt # Kotlin implementation or test #
                    IndexedAppList.kt # Kotlin implementation or test #
                    TreeBranchList.kt # Kotlin implementation or test #
                  components/ # Project directory #
                    Header.kt # Kotlin implementation or test #
                    Localization.kt # Kotlin implementation or test #
                    VectorPack.kt # Kotlin implementation or test #
                    VectorPackPreview.kt # Kotlin implementation or test #
                  folders/ # Project directory #
                    DenseFolderCanvas.kt # Kotlin implementation or test #
                    Editors.kt # Kotlin implementation or test #
                    FolderGrid.kt # Kotlin implementation or test #
                    FolderMembershipPicker.kt # Kotlin implementation or test #
                    FoldersScreen.kt # Kotlin implementation or test #
                    MagneticFolderApps.kt # Kotlin implementation or test #
                  home/ # Project directory #
                    BlankScreen.kt # Kotlin implementation or test #
                    ClockFace.kt # Kotlin implementation or test #
                    HomeAlphabet.kt # Kotlin implementation or test #
                    HomeScreen.kt # Kotlin implementation or test #
                    HomeWidgets.kt # Kotlin implementation or test #
                  mindful/ # Project directory #
                    FocusUi.kt # Kotlin implementation or test #
                  navigation/ # Project directory #
                    GestureCoordinator.kt # Kotlin implementation or test #
                    HomeTapRouter.kt # Kotlin implementation or test #
                    MotionPolicy.kt # Kotlin implementation or test #
                    Navigation.kt # Kotlin implementation or test #
                    NavigationUi.kt # Kotlin implementation or test #
                    PagePolicy.kt # Kotlin implementation or test #
                  privatespace/ # Project directory #
                    PrivateSpaceSettings.kt # Kotlin implementation or test #
                  settings/ # Project directory #
                    AdditionalControls.kt # Kotlin implementation or test #
                    PermissionSettings.kt # Kotlin implementation or test #
                    SelectionMarker.kt # Kotlin implementation or test #
                    SettingsUi.kt # Kotlin implementation or test #
                    StructuredSettings.kt # Kotlin implementation or test #
                    WidgetStyleControls.kt # Kotlin implementation or test #
        res/ # Project directory #
          drawable/ # Project directory #
            ic_launcher.xml # Android resource / component configuration #
            ic_notification.xml # Android resource / component configuration #
          values/ # Project directory #
            experience.xml # Android resource / component configuration #
            fixes.xml # Android resource / component configuration #
            iteration.xml # Android resource / component configuration #
            navigation.xml # Android resource / component configuration #
            rule_enforcement.xml # Android resource / component configuration #
            strings.xml # Android resource / component configuration #
            styles.xml # Android resource / component configuration #
          values-de/ # Project directory #
            navigation.xml # Android resource / component configuration #
            strings.xml # Android resource / component configuration #
          values-es/ # Project directory #
            navigation.xml # Android resource / component configuration #
            strings.xml # Android resource / component configuration #
          values-fr/ # Project directory #
            navigation.xml # Android resource / component configuration #
            strings.xml # Android resource / component configuration #
          values-it/ # Project directory #
            navigation.xml # Android resource / component configuration #
            strings.xml # Android resource / component configuration #
          values-pl/ # Project directory #
            navigation.xml # Android resource / component configuration #
          values-ro/ # Project directory #
            experience.xml # Android resource / component configuration #
            fixes.xml # Android resource / component configuration #
            iteration.xml # Android resource / component configuration #
            navigation.xml # Android resource / component configuration #
            strings.xml # Android resource / component configuration #
          xml/ # Project directory #
            device_admin.xml # Android resource / component configuration #
            file_paths.xml # Android resource / component configuration #
            locales_config.xml # Android resource / component configuration #
            rule_enforcement.xml # Android resource / component configuration #
      test/ # Project directory #
        java/ # Project directory #
          com/ # Project directory #
            emmanuela/ # Project directory #
              launcher/ # Project directory #
                AppMetadataTest.kt # Kotlin implementation or test #
                AppPolicyTest.kt # Kotlin implementation or test #
                ConfigurationTest.kt # Kotlin implementation or test #
                ExperienceTest.kt # Kotlin implementation or test #
                FinalTouchTest.kt # Kotlin implementation or test #
                FocusSessionTest.kt # Kotlin implementation or test #
                FolderCodecTest.kt # Kotlin implementation or test #
                MotionPolicyTest.kt # Kotlin implementation or test #
                NavigationTest.kt # Kotlin implementation or test #
                ScreenTimeTest.kt # Kotlin implementation or test #
                SearchIndexTest.kt # Kotlin implementation or test #
                SurfaceStylesTest.kt # Kotlin implementation or test #
                V24RegressionTest.kt # Kotlin implementation or test #
                V26RulesTest.kt # Kotlin implementation or test #
                V2ConfigurationTest.kt # Kotlin implementation or test #
  build-apk.bat # Windows Gradle entry point #
  build-apk.sh # Project asset / configuration #
  build.gradle.kts # Gradle build configuration #
  docs/ # Project directory #
    ARCHITECTURE.md # Documentation #
    CHANGELOG.md # Documentation #
    history/DELIVERY_MANIFEST-delivered.json # Historical delivery metadata #
    FRONTEND_STRUCTURE.md # Documentation #
    IMPLEMENTATION_STATUS.md # Documentation #
    IMPLEMENTATION_UPDATES.md # Documentation #
    PRODUCT_REQUIREMENTS.md # Documentation #
    REQUESTED_UI.txt # Project asset / configuration #
    TESTING_FIXES_V24.md # Documentation #
    TESTING_FIXES_V25.md # Documentation #
    TESTING_FIXES_V251.md # Documentation #
    TESTING_FIXES_V252.md # Documentation #
    UI_IMPLEMENTATION_MATRIX.md # Documentation #
    UI_STRUCTURE.md # Documentation #
    UPGRADE_V26.md # Documentation #
    VALIDATION.md # Documentation #
    history/ # Project directory #
      CHANGES_REQUESTED_2026-09-30.md # Documentation #
      DEVICE_CHECKLIST.md # Documentation #
      IMPLEMENTATION_PASS_2.md # Documentation #
      PROJECT_STRUCTURE.md # Documentation #
  gradle/ # Project directory #
    wrapper/ # Project directory #
      gradle-wrapper.jar # Gradle wrapper bootstrap #
      gradle-wrapper.properties # Build / wrapper properties #
  gradle.properties # Build / wrapper properties #
  gradlew # Project asset / configuration #
  gradlew.bat # Windows Gradle entry point #
  settings.gradle.kts # Gradle build configuration #
  tools/ # Project directory #
    check_sources.py # Structural verification helper #
```
