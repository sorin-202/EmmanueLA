# Widget styles and safe local imports

In Settings → Home → Manage widgets, open a widget's options and select **Style**. Clock, Date, Weather, Screen time, Battery and Location each store their own appearance. Minimal, Editorial/Instrument, Soft card and Outlined presets can be adjusted independently. Reset removes only that widget's override and restores the existing inherited appearance. The preview and Home share text, frame, background, border and padding rendering. Digital typography controls do not change the analogue Hands clock's hands.

**Import JSON style** opens Android's document picker. EmmanueLA reads the selected file once, validates it locally, and shows a preview. **Apply style** saves the validated values for that widget. Cancel, a picker cancellation, or any read/validation error leaves the current style unchanged. The source file is never executed, copied into a code loader, uploaded or fetched later; persistent document access is unnecessary because only the parsed values are stored. Local imports have no network dependency. Some document providers themselves require connectivity; use an on-device file to import offline.

Files must be strict UTF-8 JSON, no larger than 32 KiB, with these four required top-level properties:

```json
{
  "schema": "emmanuela.widget-style",
  "version": 1,
  "widget": "clock",
  "style": {
    "font": "Serif",
    "weight": "Medium",
    "italic": false,
    "scale": 1.1,
    "color": "#F6F0E8",
    "background": "#FF101018",
    "opacity": 0.75,
    "radius": 24,
    "alignment": "Center",
    "width": 280,
    "borderColor": "#A6C9A8",
    "borderWidth": 1.5,
    "horizontalPadding": 16,
    "verticalPadding": 12,
    "letterSpacing": 0.5
  }
}
```

`widget` must match the widget being edited: `clock`, `date`, `battery`, `usage`, `weather` or `location`. Version 1 accepts only the properties below. All style properties are optional; an imported style starts with the documented defaults, rather than merging hidden properties from the previous style.

| Property | Allowed values | Default |
| --- | --- | --- |
| `font` | `Inherit`, `Sans`, `Serif`, `Monospace`, `Custom` (uses an already imported local font) | `Inherit` |
| `weight` | `Inherit`, `Regular`, `Medium`, `Bold` | `Inherit` |
| `italic` | `true`, `false`, `null` (inherit) | `null` |
| `scale` | Number 0.6–1.5, relative to the widget's text size | 1 |
| `color` | `auto`, `#RRGGBB`, `#AARRGGBB` | `auto` |
| `background` | Same color notation; `auto` means transparent | `auto` |
| `opacity` | Number 0–1, multiplies background alpha | 0.25 |
| `radius` | Number 0–40 dp; 0 gives square corners | 18 |
| `alignment` | `Left`, `Center`, `Right` | `Left` |
| `width` | 0 for content width, or 80–320 dp; constrained by available screen width | 0 |
| `borderColor` | Same color notation; `auto` uses translucent white | `auto` |
| `borderWidth` | Number 0–6 dp; 0 hides border | 0 |
| `horizontalPadding` | Number 0–32 dp per side | 8 |
| `verticalPadding` | Number 0–32 dp per side | 8 |
| `letterSpacing` | Number −1–8 sp | 0 |

Unknown properties, duplicate properties, comments, trailing commas/content, arrays, extra nested objects, non-finite numbers, wrong types, unsupported versions, malformed UTF-8, invalid colors and values outside these ranges are rejected. Files cannot define actions, scripts, code, URLs, paths, images, new fonts or remote resources. Old saved widget appearances retain their previous inherited values and background outline when migrated; new style controls persist independently in the existing configuration/DataStore path.

Automated coverage: `WidgetStyleImportTest` checks the full property set, persistence and sibling independence, schema/version/widget mismatch, hostile and unknown properties, strict syntax/types/ranges, file size/encoding limits, presets and legacy migration. `WidgetStyleFlowTest` exercises the widget options route, document-result callback, preview cancellation, apply and DataStore persistence on Android. Physical-device checks still need to cover document-provider behavior, typography on installed fonts, and each preview against Home on narrow and large screens.
