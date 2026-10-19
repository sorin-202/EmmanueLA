# Folder appearance and size

Open a folder and choose **Edit**. Changes remain a draft until **Save**; **Cancel** keeps the saved appearance and dimensions.

## Appearance

Each folder has its own palette/RGB color, solid-background option, and optional image. **Apply RGB** accepts three whole numbers from 0 through 255 and updates the preview. Invalid channels cannot be applied. Existing folders retain their colored outline/global fill behavior until their appearance is edited.

**Choose background image** opens Android's document picker with the `image/*` filter. A successful import retains Android read permission for the selected `content://` document. Sources must be decodable images; network URLs and file paths are rejected. Decoding bounds the source to 100 megapixels, checks known file sizes against 40 MB, and samples the displayed bitmap to at most 1024 pixels per edge. Unknown provider lengths are checked through image dimensions. Android 8/8.1 decoding also handles EXIF rotation; newer versions use ImageDecoder. The image is center cropped at presentation and shaded to keep labels readable. The original document is unchanged.

The same appearance is used in the preview, folder cards, drag previews, and the opened folder. When the source is deleted or permission is revoked, a readable fallback appears; returning to the app retries document access. Replace or remove the image in Edit. Images remain local document references; portable configuration exports omit device-specific image URIs.

Abandoned or invalid new imports release their newly acquired persistent permission. Replacing/removing an image or deleting a folder releases its grant when no other folder, wallpaper, or wallpaper album uses that document. A configuration change preserves the draft and its grant.

## Dimensions

**App grid dimensions** configures 1–8 columns and 1–8 rows and selects the Grid layout. Examples include 3×3, 4×4, 5×4, and 5×5. Columns adapt to a minimum 56 dp cell width on smaller screens. Rows use usable 88–144 dp cells; extra apps scroll vertically. Resizing does not change stored membership or manual ordering. Older records without explicit app-grid dimensions migrate to 4×4.

Canvas width/height spans also accept up to 8 cells; zero retains the global value. Canvas spans shrink to the available screen columns and pack without overlap. List/grid folder cards keep their existing independent dp-size controls.

## Device checks still needed

- Choose a real document/photo provider image; save, restart, and confirm both folder card and opened-folder appearance.
- Rotate while an image import draft is open, cancel/reselect repeatedly, and check retained URI permissions.
- Delete the source or revoke provider access while the app is backgrounded, then resume and confirm fallback.
- Check RGB labels and controls on very light/dark backgrounds, narrow phones, landscape, and large font sizes.
- Confirm 3×3, 5×4, and 5×5 folders remain usable while scrolling and manually reordering many apps.
