# Intentional search

App List ranks exact names, prefixes, word prefixes, then substrings. Aliases and accent-insensitive matching are supported; package-name matching is optional and off by default. There is no fuzzy matching. Folder results use the existing folder authentication flow.

- `#tag` filters tagged applications.
- `@name` searches contacts with permission and compatible installed communication handlers.
- `/query` executes immediately when IME Search or hardware Enter is pressed. Web mode shows the last five searches, newest first, instead of a redundant result button. Tapping history executes that query; Clear history removes it locally. Case and whitespace variants are deduplicated.
- Settings → App List → Search actions selects up to 16 existing launcher, system or application actions. Search their displayed names, then tap the result. Remove an action by tapping its row in Search actions.

Actions are opt-in and never auto-executed by single-result auto-launch. An action or folder match also prevents automatic launching of a competing app result. Application actions use the same private/folder authentication and focus checks as other launcher entry points. Actions for applications absent from the visible catalog are omitted. System actions retain their normal Android permission flows.

Web mode offers Android default, DuckDuckGo, Google and Bing. The selected engine is saved locally. Android default uses a concrete `ACTION_WEB_SEARCH` handler; when none exists, it sends an encoded DuckDuckGo HTTPS search to the default browser. Android does not expose a browser's internal engine preference. Explicit engine choices use the default browser. Browser launches retain the launcher's app, folder and focus checks.

History and the engine preference are held in a private local DataStore, separate from portable configuration exports. Factory reset clears both; Clear history keeps the engine choice. Displaying history performs no network request. A query is passed to the external handler only when the user submits it or taps history.

Folder search results are compact icon/name rows, with a protection indicator. Search never expands a folder's app tree. Opening uses the same authentication flow as browsing folders; tree browsing remains available with an empty search.

Configuration exports include action IDs, not arbitrary intent strings or executable commands. Older configuration files default to an empty action list. A missing application action can be removed or replaced in settings.
