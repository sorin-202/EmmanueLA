# Research decisions — 2026-10-04

Research informs design; no external source code or new dependency has been copied into EmmanueLA. The owner confirmed EmmanueLA's MIT license. Repository licenses must be rechecked at a pinned revision before any future reuse.

| Reference | Relevant concept and decision | Trade-off / rejected expansion |
|---|---|---|
| [Kvaesitso](https://github.com/MM2-0/Kvaesitso) | Search-first launcher; make result sources intentional and predictable. Keep a local precomputed app index. | Do not import its GPL implementation, plugin ecosystem or cloud search providers. |
| [Lawnchair](https://github.com/LawnchairLauncher/lawnchair) | Dedicated launcher UI and benchmark/profile infrastructure are useful comparison points for future device tests. | Do not transplant Launcher3/Quickstep architecture or visual identity. |
| [Olauncher](https://github.com/tanujnotes/Olauncher) | Minimal text-first Home reinforces intentional use; preserve EmmanueLA's calm default. | Avoid adding options simply for parity. |
| [mLauncher](https://github.com/CodeWorksCreativeHub/mLauncher) | The supplied repository redirects here; customization should remain secondary to direct interaction. | No copied UI or dependencies. |
| [Rethink](https://github.com/celzero/rethink-app) | Network filtering is a distinct system with VPN/DNS lifecycle responsibilities. | No VPN added to a launcher; encrypted path filtering cannot be promised from DNS filtering. |
| [NetGuard](https://github.com/M66B/NetGuard) | Per-app network access control differs from browser-page blocking. | Avoid implying website accessibility rules are a firewall. |
| [App Manager](https://github.com/MuntashirAkon/AppManager) | Package and permission management should expose actual Android capability/state. | Avoid privileged/root/package-management scope expansion. |
| [NewPipe](https://github.com/TeamNewPipe/NewPipe) | Intentional content consumption is a useful product principle. | No extraction, downloads or embedded media client. |
| [Nudge](https://github.com/astraedus/nudge) | Delay-to-open and local budgets support intentional launch; retain targeted friction. Native surface blocking uses accessibility detection. | GPL source is not imported. Detection across app versions is best-effort, not guaranteed. |
| [Reef](https://github.com/aload0/Reef) | Groups and recurring routines suggest concise focus summaries. | No additional tracking/dashboard metrics merely for parity. |
| [Lor Focus](https://github.com/mecrimino/Lor-Focus) | On-device focus timer and selective blocking are relevant concepts to evaluate. | Repository claims are not proof of reliability on EmmanueLA's supported devices. |
| [FocusGram](https://github.com/Ujwal223/FocusGram) | Its selective Instagram experience uses a web wrapper. | AGPL source not imported; a launcher cannot apply web-wrapper modifications to another native app. |

## Platform sources

- [Android common intents](https://developer.android.com/guide/components/intents-common): use explicit user action for ACTION_WEB_SEARCH, not network requests while typing. Preserve launch authorization checks for the resolved target.
- [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService): listener connection and cancellation are separate from notification-posting permission. Document post-arrival limitations; re-evaluate active rows on connection/configuration changes.
- [OpenJDK Windows socket discussion](https://mail.openjdk.org/pipermail/nio-dev/2023-March/013297.html): relocating jdk.net.unixdomain.tmpdir resolved the observed build-runner loopback failure locally.

## Implementation decisions

Search will prioritize visible labels/aliases over optional package matches. Tag/contact/web modes are explicit prefixes. Alphabet navigation is for alphabetically arranged results; relevance results must not be re-sorted by the rail. Folder results must use the existing authentication/open path. Focus rules must share temporal logic; device behavior and browser adapters need independent runtime verification.

CI extension: reactivecircus/android-emulator-runner v2 runs API35 instrumentation on GitHub-hosted Linux. Its upstream LICENSE is Apache-2.0 (inspected 2026-10-06 at https://github.com/ReactiveCircus/android-emulator-runner/blob/main/LICENSE); used as a CI action only, with no source copied or app dependency added. Action inputs were checked against upstream action.yml. Local equivalent tests execute here; remote GitHub CI execution is not claimed.
