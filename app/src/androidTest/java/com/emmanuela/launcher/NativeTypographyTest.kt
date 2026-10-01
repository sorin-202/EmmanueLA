package com.emmanuela.launcher

import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emmanuela.launcher.ui.appearance.nativeStyledTypeface
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real framework faces: JVM Android stubs cannot validate italic or font weights. */
@RunWith(AndroidJUnit4::class)
class NativeTypographyTest {
    @Test fun italicCreatesAnActuallySlantedNativeFace(){
        listOf("sans-serif","serif","monospace").forEach{family->
            val base=Typeface.create(family,Typeface.NORMAL)
            assertTrue(nativeStyledTypeface(base,FontWeight.Normal,true).isItalic)
            assertFalse(nativeStyledTypeface(base,FontWeight.Normal,false).isItalic)
            assertTrue(nativeStyledTypeface(base,FontWeight.Bold,true).isItalic)
            assertTrue(nativeStyledTypeface(base,FontWeight.Bold,true).isBold)
        }
    }
    @Test fun mediumWeightIsResolvedOnSupportedAndroidVersions(){
        if(Build.VERSION.SDK_INT>=28)assertEquals(500,nativeStyledTypeface(Typeface.DEFAULT,FontWeight.Medium,false).weight)
    }
}
