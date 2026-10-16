package com.emmanuela.launcher

import com.emmanuela.launcher.platform.BrowserAdapters
import org.junit.Assert.*
import org.junit.Test

class BrowserAdapterTest {
    private val browser="org.mozilla.firefox"
    @Test fun rejectsForeignAndMissingWindows(){
        assertNull(BrowserAdapters.address(browser,"com.android.settings",listOf("example.com")))
        assertNull(BrowserAdapters.address(browser,null,listOf("example.com")))
    }
    @Test fun skipsEmptyHintsAndAcceptsARealAddress(){
        assertEquals("https://example.com/path",BrowserAdapters.address(browser,browser,listOf("","Search or enter address","https://example.com/path")))
    }
    @Test fun unsupportedBrowserIsExplicitlyUnavailable(){
        assertNull(BrowserAdapters.address("com.opera.browser","com.opera.browser",listOf("example.com")))
    }
    @Test fun blankAndOversizedValuesCannotRetainPreviousAddress(){
        assertNull(BrowserAdapters.address(browser,browser,listOf("")))
        assertNull(BrowserAdapters.address(browser,browser,listOf("example.com/"+"x".repeat(8192))))
    }
    @Test fun onlyWebAddressesWithoutCredentialsAreAccepted(){
        listOf("javascript://example.com","file://example.com/path","https://user:password@example.com","https://example.com/has space").forEach{
            assertNull(BrowserAdapters.address(browser,browser,listOf(it)))
        }
        assertEquals("example.com/path",BrowserAdapters.address(browser,browser,listOf("example.com/path")))
    }

    @Test fun firefoxDisplayDescriptionsAndChromeTextUseOnlyAddressControls(){
        assertEquals("https://example.com/page",BrowserAdapters.inspect(browser,browser,listOf(BrowserAdapters.Field(description="Address bar, https://example.com/page"))).address)
        assertEquals("example.com",BrowserAdapters.inspect(browser,browser,listOf(BrowserAdapters.Field(description="example.com"))).address)
        assertEquals("example.com.",BrowserAdapters.inspect(browser,browser,listOf(BrowserAdapters.Field(description=" example.com. Search or enter address"))).address)
        assertEquals("example.com/path",BrowserAdapters.inspect("com.android.chrome","com.android.chrome",listOf(BrowserAdapters.Field(text="example.com/path"))).address)
        assertEquals(BrowserAdapters.Unavailable.UNREADABLE_ADDRESS,BrowserAdapters.inspect(browser,browser,listOf(BrowserAdapters.Field(description="Search or enter address"))).unavailable)
        assertNull(BrowserAdapters.inspect(browser,browser,listOf(BrowserAdapters.Field(description="example.com other.example.com"))).address)
    }
    @Test fun reportsDistinctFailuresAndNeverBlocksAnAddressStillBeingTyped(){
        assertEquals(BrowserAdapters.Unavailable.UNSUPPORTED_BROWSER,BrowserAdapters.inspect("unsupported","unsupported",emptyList()).unavailable)
        assertEquals(BrowserAdapters.Unavailable.FOREIGN_WINDOW,BrowserAdapters.inspect(browser,"com.android.settings",emptyList()).unavailable)
        assertEquals(BrowserAdapters.Unavailable.MISSING_ADDRESS_FIELD,BrowserAdapters.inspect(browser,browser,emptyList()).unavailable)
        assertEquals(BrowserAdapters.Unavailable.EDITING_ADDRESS,BrowserAdapters.inspect(browser,browser,listOf(BrowserAdapters.Field(text="example.com",editing=true))).unavailable)
    }
}
