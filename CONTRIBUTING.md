# Contributing

Work from the current local implementation; preserve the application ID and backward-compatible configuration. Keep changes focused and describe the user-visible problem, root cause, validation and remaining limits.

Build with JDK 17, Android SDK 35 and the checked-in Gradle wrapper:

```
./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

Use `gradlew.bat` on Windows. Release builds are unsigned unless signing is configured externally. Never commit keystores, credentials, local.properties, build outputs or IDE caches.

Add regression tests for meaningful domain changes. Device checks must cover relevant permissions, lifecycle, font scaling and accessibility. Record tests that could not run rather than claiming they passed. Python source checks are structural checks, not a substitute for Kotlin compilation or Android lint.

Keep processing local, avoid new permissions/dependencies unless justified, and respect licenses. EmmanueLA is MIT licensed. Do not copy GPL/AGPL source into this codebase without resolving compatibility and distribution requirements.
