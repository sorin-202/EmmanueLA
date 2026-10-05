# Intentional search

App List ranks exact names, prefixes, word prefixes, then substrings. Aliases and accent-insensitive matching are supported; package-name matching is optional and off by default. There is no fuzzy matching. Folder results use the existing folder authentication flow.

- `#tag` filters tagged applications.
- `@name` searches contacts with permission and compatible installed communication handlers.
- `/query` offers an explicit web-search action through Android. A compatible handler is required.
- Settings → App List → Search actions selects up to 16 existing launcher, system or application actions. Search their displayed names, then tap the result. Remove an action by tapping its row in Search actions.

Actions are opt-in and never auto-executed by single-result auto-launch. An action or folder match also prevents automatic launching of a competing app result. Application actions use the same private/folder authentication and focus checks as other launcher entry points. Actions for applications absent from the visible catalog are omitted. System actions retain their normal Android permission flows.

Configuration exports include action IDs, not arbitrary intent strings or executable commands. Older configuration files default to an empty action list. A missing application action can be removed or replaced in settings.
