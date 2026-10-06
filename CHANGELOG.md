# Changelog

## 2.6.0-beta engineering continuation

- Repaired delivered compilation and lint errors; preserved the local v2.6 baseline and compatible configuration imports.
- Added ranked search, independent Home/App List, protected folder results, explicit web search and opt-in search actions.
- Improved Strict Block, bounded Breaks, expiry/DST handling, warning/grace and transient intention prompts.
- Added local notification schedules, keywords, temporary suppression and actual listener diagnostics.
- Added bounded browser address checks and honest capability diagnostics. Firefox157 normal address extraction is unsupported.
- Fixed folder drag scroll ownership and widget/navigation cancellation; expanded Android regression evidence.
- Prepared search tokens reduce repeated matching work; release resource shrinking reduces the measured APK by 8.2%. All ABIs and offline language resources are retained.
- Corrected fixed App List/Folders footer insets; added bounds/navigation regression coverage.
- Final clean validation: 123 JVM tests, 18 API35 tests, lint (0 errors/43 warnings), debug/release APK and release AAB builds; minified release smoke passed. See [performance](docs/PERFORMANCE.md) and [validation](docs/VALIDATION.md).

Earlier delivery history is retained in [docs/CHANGELOG.md](docs/CHANGELOG.md).
