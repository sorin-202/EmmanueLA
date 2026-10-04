package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.ui.navigation.LauncherSurface
import com.emmanuela.launcher.ui.navigation.PagePolicy
import org.junit.Assert.*
import org.junit.Test

class V24RegressionTest {
    @Test fun everyCombinationOfPageSwitchesHasAnAccessibleDestination(){
        for(mask in 0..7){
            val p=Preferences(ui=UiPreferences(appListEnabled=mask and 2!=0,
                v2=V2Preferences(foldersEnabled=mask and 4!=0),
                experience=ExperiencePreferences(homeEnabled=mask and 1!=0)))
            val expected=buildList{
                if(mask and 1!=0)add(LauncherSurface.HOME)
                if(mask and 2!=0)add(LauncherSurface.APPS)
                if(mask and 4!=0)add(LauncherSurface.FOLDERS)
            }
            assertEquals(expected,PagePolicy.enabled(p))
            LauncherSurface.entries.forEach{requested->
                val resolved=PagePolicy.resolve(requested,p)
                if(expected.isEmpty())assertEquals(LauncherSurface.BLANK,resolved)
                else assertTrue(resolved in expected)
            }
        }
    }
    @Test fun homeAlphabetExcludesDrawerEvenInOlderInconsistentConfigurations(){
        val p=Preferences(ui=UiPreferences(appListEnabled=true,experience=ExperiencePreferences(homeAlphabet=true)))
        assertFalse(LauncherSurface.APPS in PagePolicy.enabled(p))
        assertEquals(LauncherSurface.FOLDERS,PagePolicy.resolve(LauncherSurface.APPS,p))
    }
    @Test fun packageAndActivitySearchAreIndependentOfLabelsAndAliases(){
        val app=LaunchableApp("com.acme.hidden/.MainActivity","Journal","com.acme.hidden","Notebook")
        val index=AppSearchIndex(listOf(app))
        assertEquals(listOf(app),index.search("com.acme.hidden",searchAliases=false,searchPackages=true).apps)
        assertTrue(index.search("com.acme.hidden",searchPackages=false).apps.isEmpty())
        assertEquals(listOf(app),index.search("mainactivity",searchPackages=true).apps)
        assertTrue(index.search("journal",searchAliases=false,searchPackages=false).apps.isEmpty())
        assertEquals(listOf(app),index.search("notebook",searchAliases=false,searchPackages=false).apps)
    }
    @Test fun newPreferencesAndIndividualSizesSurviveBackup(){
        val e=ExperiencePreferences(homeEnabled=false,homeStyleOverride=true,homeFont="Serif",homeTextScale=1.2f,
            homeItalic=true,homeTextWeight="Medium",widgetBackgroundOpacity=.4f,widgetCornerRadius=24f,
            homeWaveStrength=64f,drawerBeforeHomeAlphabet=false,contactPackages=setOf("com.acme.contacts"),
            wellbeingComponent="com.android.settings/.Wellbeing",weatherWebsite="https://example.com/weather")
        val original=LauncherData(settings=Preferences(ui=UiPreferences(experience=e)),folders=listOf(
            AppFolder("a","One","folder",emptyList(),widthDp=140f,heightDp=180f),
            AppFolder("b","Two","star",emptyList(),widthDp=0f,heightDp=150f)))
        val restored=ConfigurationCodec.decode(ConfigurationCodec.encode(original))
        assertEquals(original,restored)
        assertEquals(140f,restored.folders[0].widthDp,0f)
        assertEquals(0f,restored.folders[1].widthDp,0f)
    }
    @Test fun oldFolderRecordsReceiveSizeDefaults(){
        val root=org.json.JSONObject(FolderCodec.encode(listOf(AppFolder("a","One","folder",emptyList()))))
        root.getJSONArray("folders").getJSONObject(0).remove("widthDp")
        root.getJSONArray("folders").getJSONObject(0).remove("heightDp")
        val folder=FolderCodec.decode(root.toString()).single()
        assertEquals(0f,folder.widthDp,0f);assertEquals(150f,folder.heightDp,0f)
    }
    @Test(expected=IllegalArgumentException::class)
    fun invalidFolderDimensionsAreRejected(){
        FolderCodec.decode(FolderCodec.encode(listOf(AppFolder("a","One","folder",emptyList(),widthDp=-1f))))
    }
    @Test fun homeStyleDoesNotMutateGlobalTypography(){
        val global=Preferences(font="Monospace",textScale=1.1f)
        val changed=global.copy(ui=global.ui.copy(experience=global.ui.experience.copy(homeItalic=true,homeTextWeight="Bold",homeStyleOverride=true)))
        val restored=ConfigurationCodec.decode(ConfigurationCodec.encode(LauncherData(settings=changed))).settings
        assertEquals(global.font,restored.font);assertEquals(global.textScale,restored.textScale,0f)
        assertFalse(restored.ui.italic);assertEquals("Regular",restored.ui.experience.textWeight)
        assertTrue(restored.ui.experience.homeItalic)
    }
}
