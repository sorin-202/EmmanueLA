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
}
