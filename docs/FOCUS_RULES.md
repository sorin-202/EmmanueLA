# Live the Moment rule behavior

Rules run locally. Launcher admission checks apply when opening through EmmanueLA. Optional Accessibility access extends checks to foreground apps and supported browser address fields; it is best-effort and is not a device-owner security boundary.

## Continuous block and protection

Block continuously stops this group's selected apps/sites even when no time window is configured. Protect rule changes is separate: it requires device authentication for edits and Break changes when authentication is enabled. Existing protected groups keep their protection settings during migration.

## Break

A manual Break grants temporary access for 5, 10, 15 or a custom 1–120 minutes. The editor displays remaining time and offers End Break. The grant survives process restart until its absolute expiry. It is omitted from portable configuration exports. Device clock changes can change remaining wall time; a clock earlier than the grant's start does not activate it.

Scheduled Break windows use local time and support overnight intervals. A scheduled Strict Block takes precedence over both manual and scheduled Break in the same group. Otherwise Break suspends that group's continuous block, pauses and limits. Other groups, per-app blocks, authentication and hidden/private restrictions still apply. Usage and opens are not reset by taking a Break.

## Time boundaries

Rules reevaluate at schedule boundaries and midnight, including while a blocking overlay is visible. Expired budgets do not create rapid polling. Repeated DST hours include both occurrences of a boundary, and clock transitions trigger reevaluation. Time/timezone changes also invalidate the pending deadline.

## Validation scope

Domain tests cover overlap, overnight expiry, DST transitions, bounded Break imports, old configuration compatibility, clock rollback and expiry. Android tests cover persistence and startup. The Android workflow test passes Home → App List → explicit /query UI → Settings → Live the Moment → saved Break start/end. Physical-device enforcement and OEM behavior remain separate acceptance work.

## Allowances and intentions

A rule can Block or Warn when a daily/session/open limit is reached. Warn permits continued use; it never overrides Strict Block. Optional grace minutes extend daily and session time allowances, not open counts. Warning lead time and grace are advanced controls. Warnings appear at launch and during use with optional background rule access. Warning-only rules do not block an app when Usage Access is unavailable; they report the missing access.

Enter an intention can require text before opening a selected app, even with a zero-second delay. EmmanueLA does not save the text. Launcher confirmation and the optional accessibility pause enforce the same choice. Per-session reminders use the same grace-adjusted allowance as launch and foreground checks.
