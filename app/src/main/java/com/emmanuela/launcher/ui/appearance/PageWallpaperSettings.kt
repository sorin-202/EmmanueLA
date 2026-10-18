package com.emmanuela.launcher.ui.appearance

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.folders.FolderImages
import com.emmanuela.launcher.ui.folders.folderImage
import com.emmanuela.launcher.ui.settings.SettingsSection
import com.emmanuela.launcher.ui.settings.SliderRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.takeIf { it !== this }?.activity()
    else -> null
}

/** The same bounded document decoder/cache and resume access check as folder covers. */
@Composable
fun PageWallpaperSurface(page: String, settings: Preferences, modifier: Modifier = Modifier,
                         content: @Composable () -> Unit) {
    val wallpaper = settings.ui.pageWallpapers[page]
    val image = folderImage(wallpaper?.uri.orEmpty())
    val colors = MaterialTheme.colorScheme
    val pageColors = if (image == null) colors else colors.copy(
        primary = if (colors.primary.luminance() < .2f) Color.White else colors.primary,
        onPrimary = Color.Black, onSurface = Color.White, onBackground = Color.White,
        surface = Color(0xff101010), onSurfaceVariant = Color(0xffdddddd),
        surfaceContainerHigh = Color(0xff252525), surfaceVariant = Color(0xff252525),
        outline = Color(0xffaaaaaa), primaryContainer = Color(0xff333333), onPrimaryContainer = Color.White
    )
    Box(modifier.fillMaxSize().background(if (image == null) colors.background else pageColors.surface)) {
        if (image != null && wallpaper != null) {
            PagePhoto(image, wallpaper.style, Modifier.matchParentSize().testTag("Page wallpaper: $page"))
        }
        // A decode or deleted-photo fallback must not recreate the App List/search state.
        MaterialTheme(colorScheme = pageColors) { content() }
    }
}

@Composable
private fun PagePhoto(image: ImageBitmap, style: PhotoStyle, modifier: Modifier) {
    Box(modifier.clipToBounds().background(Color.Black)) {
        Image(image, null, Modifier.fillMaxSize().graphicsLayer {
            scaleX = style.zoom; scaleY = style.zoom
            // Zoom adds crop overflow even on an axis that initially fit exactly.
            translationX = -style.x * (style.zoom - 1f) * size.width / 2f
            translationY = -style.y * (style.zoom - 1f) * size.height / 2f
        }.blur(style.blur.dp), contentScale = if (style.fit == "Fit") ContentScale.Fit else ContentScale.Crop,
            alignment = BiasAlignment(style.x, style.y))
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = style.dim)))
    }
}

/** Draft preview is explicit: picking/adjusting never overwrites another page or the saved photo. */
@Composable
fun PageWallpaperSettings(page: String, data: LauncherData, model: LauncherViewModel) {
    require(page in PageWallpapers.pages)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved = data.settings.ui.pageWallpapers[page]
    var uri by rememberSaveable(page, saved) { mutableStateOf(saved?.uri.orEmpty()) }
    var x by rememberSaveable(page, saved) { mutableFloatStateOf(saved?.style?.x ?: 0f) }
    var y by rememberSaveable(page, saved) { mutableFloatStateOf(saved?.style?.y ?: 0f) }
    var zoom by rememberSaveable(page, saved) { mutableFloatStateOf(saved?.style?.zoom ?: 1f) }
    var dim by rememberSaveable(page, saved) { mutableFloatStateOf(saved?.style?.dim ?: .35f) }
    var blur by rememberSaveable(page, saved) { mutableFloatStateOf(saved?.style?.blur ?: 0f) }
    var fit by rememberSaveable(page, saved) { mutableStateOf(saved?.style?.fit ?: "Fill") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentUri by rememberUpdatedState(uri)
    DisposableEffect(page) {
        onDispose {
            if (context.activity()?.isChangingConfigurations != true) {
                FolderImageGrants.cleanup(context, listOf(currentUri))
            }
        }
    }
    fun resetDraft() {
        val discarded = uri
        uri = saved?.uri.orEmpty()
        val style = saved?.style ?: PhotoStyle()
        x = style.x; y = style.y; zoom = style.zoom; dim = style.dim; blur = style.blur; fit = style.fit
        error = null
        FolderImageGrants.cleanup(context, listOf(discarded))
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected ->
        if (selected != null) scope.launch {
            busy = true
            var assigned = false
            try {
                val value = selected.toString()
                require(FolderAppearance.validImageUri(value))
                requireNotNull(FolderImages.load(context, value)) { "Unreadable image" }
                try {
                    context.contentResolver.takePersistableUriPermission(selected, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (failure: SecurityException) {
                    // App-owned files already have durable access; external providers must offer a retained grant.
                    if (selected.authority != "${context.packageName}.files") throw failure
                }
                val discarded = uri
                uri = value; x = 0f; y = 0f; zoom = 1f; dim = .35f; blur = 0f; fit = "Fill"
                assigned = true
                error = null
                FolderImageGrants.cleanup(context, listOf(discarded))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Could not open this photo or retain access. Choose a local image from Files." }
            finally {
                busy = false
                if (!assigned) FolderImageGrants.cleanup(context, listOf(selected.toString()))
            }
        }
    }
    val image = folderImage(uri)
    val style = PhotoStyle(x, y, zoom, dim, blur, fit)
    Text(localized(if (page == "apps") "App List wallpaper" else "Folders wallpaper"), style = MaterialTheme.typography.titleLarge)
    Text(localized("Choose a photo for this page. Home and the other pages keep their own backgrounds."),
        Modifier.padding(top = 8.dp, bottom = 16.dp), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Box(Modifier.fillMaxWidth().height(230.dp).clipToBounds().background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .testTag("Page wallpaper preview").pointerInput(page, saved, uri) {
            detectTransformGestures { _, pan, scale, _ ->
                x = (x - pan.x / size.width.coerceAtLeast(1) * 2).coerceIn(-1f, 1f)
                y = (y - pan.y / size.height.coerceAtLeast(1) * 2).coerceIn(-1f, 1f)
                zoom = (zoom * scale).coerceIn(1f, 3f)
            }
        }, contentAlignment = Alignment.Center) {
        if (image != null) PagePhoto(image, style, Modifier.matchParentSize().testTag("Page wallpaper preview image"))
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val color = if (image != null) Color.White else MaterialTheme.colorScheme.onSurface
            Text(localized(if (page == "apps") "All Apps" else "Organized Folders"), color = color, style = MaterialTheme.typography.headlineSmall)
            Text(if (page == "apps") "Calendar\nCamera\nSettings" else "Personal     Work", color = color, style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (uri.isNotEmpty()) Text(localized("Pinch to zoom and drag to position the photo."), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
    error?.let { Text(localized(it), Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.error) }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    OutlinedButton(enabled = !busy, onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(localized("Choose photo"))
    }
    if (uri.isNotEmpty()) {
        SliderRow("Dimming", dim, 0f..1f) { dim = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = fit == "Fill", onClick = { fit = "Fill" }, label = { Text(localized("Fill")) })
            FilterChip(selected = fit == "Fit", onClick = { fit = "Fit"; zoom = 1f }, label = { Text(localized("Fit")) })
        }
        SettingsSection("More adjustments", "Blur and reset position") {
            SliderRow("Blur", blur, 0f..30f) { blur = it }
            TextButton(onClick = { x = 0f; y = 0f; zoom = 1f }) { Text(localized("Reset position")) }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(enabled = !busy, onClick = ::resetDraft) { Text(localized("Cancel")) }
        Button(enabled = !busy && image != null && uri.isNotEmpty(), onClick = {
            scope.launch {
                busy = true
                try {
                    if (!model.savePageWallpaper(page, PageWallpaper(uri, style))) error = "Could not save wallpaper. Try again."
                } finally { busy = false }
            }
        }) { Text(localized("Save wallpaper")) }
    }
    if (saved != null) TextButton(enabled = !busy, onClick = {
        scope.launch {
            busy = true
            try {
                if (model.savePageWallpaper(page, null)) { val discarded = uri; uri = ""; FolderImageGrants.cleanup(context, listOf(discarded)) }
                else error = "Could not remove wallpaper. Try again."
            } finally { busy = false }
        }
    }) { Text(localized("Remove wallpaper")) }
}
