# Notification filtering and local spam detection

Notification Access is required. Settings → Apps → Notifications shows both Android's permission state and whether the listener is connected. A reconnect control requests an Android rebind when access is enabled but the listener is disconnected. Android owns service binding; the service also requests a rebind after disconnection. Listener configuration loads independently of an open launcher activity. WorkManager persists scheduled summary work across process death and reboot.

## Delivery rules

Each application's notification rule supports Normal, Dismiss, or Digest, an always/schedule/Strict Block scope, optional title/text keywords, and temporary suppression. Overnight schedules use their starting day. A clock/time-zone receiver and one scheduled boundary job reevaluate existing active notifications when the rule changes or its next boundary arrives. Temporary suppression expires automatically. Notification text is read locally only when filtering is applicable; no message content is written to the digest.

Digest capture first persists pending metadata. The listener then rechecks connection, filter configuration, and the current rule before asking Android to cancel the notification. Only Android's `REASON_LISTENER_CANCEL` removal callback turns pending metadata into a confirmed count. Failed persistence leaves the original notification alone. Configuration changes during capture cause reevaluation instead of a stale dismissal. Replayed events do not duplicate counts.

Settings → Apps → Notifications → View notification summary opens the digest even without permission to post EmmanueLA notifications. It shows per-app counts and filtering reasons, lets the user open the source app through the launcher's existing authentication/blocking checks, clear one app or all counts, or allow future notifications. Allow future switches delivery to Normal, ends temporary suppression, and adds the app to the spam allowlist. It does not remove app-launch restrictions.

Counts and reasons have a seven-day maximum group lifetime. Expired groups are omitted on access and removed during digest maintenance or subsequent captures. They contain package names, timestamps, classification reasons, counters, and hashed notification identities, not titles, text, notification actions, or URLs. Pending metadata is bounded to 1,000 records; confirmed package groups are bounded to 500. Clearing the digest also dismisses EmmanueLA's summary notification.

Timed summaries require the digest setting, Notification Access to capture originals, and permission to post EmmanueLA notifications. Android's summary channel must also be enabled. Failed delivery leaves counts unannounced for a later attempt. WorkManager's minimum periodic interval is 15 minutes; Android battery restrictions can delay delivery. The default interval remains effective for untouched app rules; a per-app interval can explicitly override it, including 30 minutes. Legacy non-default app intervals remain effective.

## Spam modes

Spam detection uses a bounded local rule engine, with no LLM, cloud request, or additional production dependency. A global default offers Off, Conservative, and Stronger. Each app can inherit it or select a different mode, and an app allowlist always bypasses the classifier. Delivery rules and temporary suppression remain separately applicable.

- Conservative detects three similar promotional notifications from one app within five minutes.
- Stronger also detects three similar notifications, multiple promotional patterns, or eight recent promotional arrivals within a minute.
- User spam keywords explicitly classify matches; user allow words take priority over those keywords.

Similarity normalizes whitespace, numbers, and URLs before hashing. Bounded in-memory history stores hashes, identities, and times, never message content. Configuration refreshes do not count an existing notification as a new arrival. Old active notifications do not become an artificial burst after reconnect. Signal history resets with the listener process; settings, confirmed counts, and reasons persist.

Spam captures and reasons are available in the digest even when timed summaries are disabled. Each filtered group shows its reason. Allow future notifications provides a direct correction for false positives. The stronger mode can produce false positives for repetitive noncritical messages; per-app Off, allowlisting, and allow words provide local control.

## Android limitations and physical checks

The listener receives notifications after posting: Android may already have played a sound or shown a heads-up alert. The app's Android notification channels can be made Silent for quiet delivery. Android does not provide a general way for a listener to restore an original notification after cancellation, including its source identity and actions. The digest therefore offers the authenticated source-app path and future allowance instead of fabricating a restored notification.

Nonclearable, ongoing, foreground-service, group-summary, call, alarm, transport/media, service, navigation, and system notifications are preserved. Recognized security/authentication wording and possible 4–8 digit codes are also preserved. Apps that mislabel critical notifications or use unrecognized wording cannot be identified perfectly; essential applications should be allowlisted. Android can redact sensitive content, so keyword matching may be unavailable. OEM power management or user revocation can interrupt the listener; the UI reports the actual state and does not substitute a UI toggle for permission.

Instrumentation covers listener access/reconnect, keyword filtering, protected categories, digest capture/confirmation/persistence/clearing, spam default/overrides/allowlisting, and a real summary notification plus effective notification-permission denial. JVM tests cover schedules, keywords, interval migration/defaults, codec validation, classifier modes, frequency/similarity windows, allowlists, security guards, and replay/reconnect behavior. Physical-device checks remain necessary for OEM binding/reboot behavior, battery-restricted delivery, real messaging apps' categories/redacted content, and false-positive tuning.
