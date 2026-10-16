# Home placement

On the normal Home screen, hold a widget or an app icon until its outline and
shadow appear, then keep holding and drag. The final position snaps to the
configured widget grid when released. A cancelled Android touch gesture restores
the original position. Movement is local Compose state; configuration is written
only after a changed, completed drop. Ordinary widget taps retain their existing
actions. The arrangement screen remains available for existing pinch controls.

Hold and release without moving to open that item's options. Accessibility users
can use the item's **Options** action. Widget options retain size, alignment and
removal. Individual widget styles are available in the widget's settings.

Use **+ App** beside Settings on Home to select an installed launcher activity.
The shortcut shows the application's existing icon and label. Hold it to move it,
or hold and release to choose **Remove from Home**. Removal does not uninstall the
application. Shortcuts and their dp positions are included in local configuration
and backups. Older configurations migrate with an empty shortcut list.

Hidden apps are excluded from the picker and from Home even if a shortcut was
created before the app was hidden. Private apps, protected-folder apps and blocked
apps use the same authenticated, policy-checked launch callback as the existing
Home favorites and App List. A Home icon cannot bypass those rules. Uninstalled
apps retain a removable unavailable placeholder; they cannot launch an arbitrary
intent. Up to 64 activity shortcuts can be saved; the user may move overlapping
items to fit their preferred layout.

## Verification

`HomePlacementTest` checks snapping and viewport bounds. `HomeShortcutTest`
checks configuration round trips, migration, hidden-app selection, placement and
invalid stored activity/coordinate limits. Android `HomeDragTest` injects held
touch movement followed by release and by `ACTION_CANCEL` on normal Home, checks
that moving produces no persistence emissions, and checks recreation.
`HomeShortcutFlowTest` covers choosing an app, moving/cancelling, recreation,
blocked launch, removal without uninstall, and launching an installed activity.
Actual-device gesture feel and device authentication still require physical
testing; automated test results are reported separately in the final report.
