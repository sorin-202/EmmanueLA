# Installable beta APK from Actions

On a successful main-branch push or manual run, `Validate EmmanueLA` retains the existing clean JVM/lint/debug/release/AAB build and API35 instrumentation jobs. A dependent `beta-apk` job signs the exact optimized unsigned release APK from that run. It does not rebuild it as debug or modify its manifest. R8 mapping presence, APK alignment, signature, application ID and `debuggable=false` are checked before installation and launch on a fresh API35 emulator. Only then is `EmmanueLA.apk` uploaded under `EmmanueLA-APK`.

Download: repository **Actions → Validate EmmanueLA → successful main run → Artifacts → EmmanueLA-APK**. Extract the ZIP. Android 8/API26 or newer is required. On the phone, allow installation from the app used to open the APK if Android requests it.

## Dedicated beta signing identity

Configure these repository secrets under **Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `BETA_KEYSTORE_BASE64` | Base64 contents of a dedicated beta keystore |
| `BETA_KEYSTORE_PASSWORD` | Keystore password |
| `BETA_KEY_ALIAS` | Alias of its signing key |
| `BETA_KEY_PASSWORD` | Signing-key password |

Retain the same beta key securely for subsequent APKs. Never commit key material/passwords or use production credentials here. Secrets are available only to the signing step of a trusted main run, after validation; pull requests never invoke signing. The private keystore is decoded into a restricted runner temporary directory and removed on step exit. Passwords are passed to `apksigner` through environment variables, not command-line literals. Only the signed APK is uploaded, never the keystore. The job needs no repository write permission.

The application ID remains `com.emmanuela.launcher`. A different signing key (including an existing debug installation) prevents an in-place update. Export configuration before deciding to uninstall such an installation; uninstalling deletes its data. Beta APKs are not production-signed releases. Local release builds and the separate AAB remain unsigned unless independently configured.

The workflow performs emulator installation, not physical-device acceptance, Play publication or GitHub Release creation. A missing/invalid secret fails signing explicitly rather than substituting a debug or newly generated ephemeral key.
