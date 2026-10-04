package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.junit.Assert.*
import org.junit.Test

class SurfaceStylesTest {
    @Test fun stylesRemainIndependentAcrossWidgetsAndPhotos(){
        val prefs=Preferences(ui=UiPreferences(v2=V2Preferences(
            widgetStyles=mapOf("clock" to WidgetStyle(font="Serif",color=0xFFFFFFFFL,background=0xFF000000L,italic=true),"battery" to WidgetStyle(scale=.8f,italic=false)),
            photoStyles=mapOf("content://photos/a" to PhotoStyle(zoom=2f,dim=.7f),"content://photos/b" to PhotoStyle(blur=12f,fit="Fit")))))
        val restored=ConfigurationCodec.decode(ConfigurationCodec.encode(LauncherData(settings=prefs))).settings
        assertEquals(prefs,restored)
        assertEquals(true,restored.styleForWidget("clock").italic)
        assertEquals(false,restored.styleForWidget("battery").italic)
        assertEquals(2f,restored.styleForPhoto("content://photos/a").zoom,0f)
        assertEquals(12f,restored.styleForPhoto("content://photos/b").blur,0f)
    }
    @Test fun missingStylesUseLegacyConfigurationWithoutChangingIt(){
        val p=Preferences(dim=.65f,ui=UiPreferences(widgetFont="Serif",widgetScale=1.2f,widgetBackground=0xFFFFFFFFL))
        assertEquals("Serif",p.styleForWidget("clock").font)
        assertEquals(1.2f,p.styleForWidget("date").scale,0f)
        assertEquals(.65f,p.styleForPhoto("content://photo").dim,0f)
        assertTrue(p.ui.v2.widgetStyles.isEmpty())
    }
    @Test fun portableBackupRemovesDeviceBoundPhotoStyles(){
        val data=LauncherData(settings=Preferences(wallpapers=listOf("content://a"),ui=UiPreferences(v2=V2Preferences(photoStyles=mapOf("content://a" to PhotoStyle())))))
        assertTrue(ConfigurationCodec.portable(data).settings.ui.v2.photoStyles.isEmpty())
    }
    @Test fun magneticReorderPreservesEveryAppAndOnlyChangesTheSelectedFolder(){
        val a=AppFolder("a","One","folder",listOf("a/.A","hidden/.H","b/.B","c/.C"))
        val b=AppFolder("b","Two","folder",listOf("x/.X"))
        val changed=a.copy(apps=ReorderRules.move(a.apps,"c/.C","a/.A"),manualOrder=true)
        assertEquals(listOf("c/.C","a/.A","hidden/.H","b/.B"),changed.apps)
        assertEquals(a.apps.toSet(),changed.apps.toSet());assertEquals(listOf("x/.X"),b.apps)
        assertEquals(changed,FolderCodec.decode(FolderCodec.encode(listOf(changed))).single())
        assertEquals(a.apps,ReorderRules.move(a.apps,"missing","a/.A"))
    }
    @Test fun mergeVisibleOrderRetainsHiddenAndUnavailableEntries(){
        assertEquals(listOf("b","hidden","a","gone"),ReorderRules.mergeVisible(listOf("a","hidden","b","gone"),listOf("b","a")))
    }
    @Test(expected=IllegalArgumentException::class)
    fun rejectsUnknownWidgetBeforeSaving(){SurfaceStyleCodec.readWidgets(SurfaceStyleCodec.widgets(mapOf("unknown" to WidgetStyle())))}
    @Test(expected=IllegalArgumentException::class)
    fun rejectsInvalidPhotoCrop(){SurfaceStyleCodec.readPhotos(SurfaceStyleCodec.photos(mapOf("content://a" to PhotoStyle(zoom=9f))))}
}
