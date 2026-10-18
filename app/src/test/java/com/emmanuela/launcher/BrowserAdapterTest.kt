package com.emmanuela.launcher

import com.emmanuela.launcher.platform.BrowserAdapters
import com.emmanuela.launcher.data.FocusGroup
import com.emmanuela.launcher.data.FocusWindows
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
        assertEquals("example.com",BrowserAdapters.inspect(browser,browser,listOf(BrowserAdapters.Field(description=" example.com. Search or enter address"))).address)
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

    @Test fun abbreviatedDomainTextDoesNotDiscardFullPathOrQueryMetadata(){
        BrowserAdapters.ids.keys.forEach { owner ->
            val full="https://example.com/watch/shorts?id=42"
            val result=BrowserAdapters.inspect(owner,owner,listOf(BrowserAdapters.Field(text="example.com",description="Address bar, $full")))
            assertEquals("Richer native address metadata for $owner",full,result.address)
            assertNull(result.unavailable)
            assertFalse(result.hostOnly)
            val acrossControls=BrowserAdapters.inspect(owner,owner,listOf(BrowserAdapters.Field(text="example.com"),BrowserAdapters.Field(text=full)))
            assertEquals("A preceding domain control must not hide the complete address",full,acrossControls.address)
        }
    }

    @Test fun wwwElisionCanEnrichOnlyDomainDisplayFromTheDetailedAddress(){
        val chrome="com.android.chrome"
        val result=BrowserAdapters.inspect(chrome,chrome,listOf(BrowserAdapters.Field(text="youtube.com",description="Address bar, https://www.youtube.com/shorts/42")))
        assertEquals("https://www.youtube.com/shorts/42",result.address)
        assertFalse(result.hostOnly)
        assertEquals(BrowserAdapters.Unavailable.AMBIGUOUS_ADDRESS,BrowserAdapters.inspect(chrome,chrome,listOf(
            BrowserAdapters.Field(text="youtube.com/watch/42",description="Address bar, https://www.youtube.com/shorts/42")
        )).unavailable)
        assertEquals(BrowserAdapters.Unavailable.AMBIGUOUS_ADDRESS,BrowserAdapters.inspect(chrome,chrome,listOf(
            BrowserAdapters.Field(text="youtube.com/shorts/42",description="Address bar, https://www.youtube.com/shorts/42")
        )).unavailable)
        assertEquals(BrowserAdapters.Unavailable.AMBIGUOUS_ADDRESS,BrowserAdapters.inspect(chrome,chrome,listOf(
            BrowserAdapters.Field(text="foo.youtube.com",description="Address bar, https://bar.youtube.com/shorts/42")
        )).unavailable)
    }

    @Test fun descriptionQueryCommasAndSemicolonsRemainPartOfTheAddress(){
        val full="https://example.com/watch;section=video?tags=one,two;shorts"
        val generic=BrowserAdapters.inspect("com.android.chrome","com.android.chrome",listOf(BrowserAdapters.Field(text="example.com",description="Address bar, $full")))
        assertEquals(full,generic.address)
        val firefox=BrowserAdapters.inspect(browser,browser,listOf(BrowserAdapters.Field(description=" $full. Search or enter address")))
        assertEquals(full,firefox.address)
        assertTrue(FocusWindows.matches(FocusGroup(keywords=setOf("two;shorts")),firefox.address!!))
    }

    @Test fun percentEncodedAliasesAreCompatibleAndLiteralPlusIsPreserved(){
        val result=BrowserAdapters.inspect(browser,browser,listOf(BrowserAdapters.Field(
            text="example.com/%73horts/a%2Bb?tag=one%2Btwo",
            description="Address bar, https://example.com/shorts/a+b?tag=one+two"
        )))
        assertEquals("https://example.com/shorts/a+b?tag=one+two",result.address)
        assertNull(result.unavailable)
        assertTrue(FocusWindows.matches(FocusGroup(keywords=setOf("a+b")),result.address!!))
        val differentPlus=BrowserAdapters.inspect(browser,browser,listOf(BrowserAdapters.Field(
            text="example.com/one+two",description="Address bar, https://example.com/one%20two"
        )))
        assertEquals(BrowserAdapters.Unavailable.AMBIGUOUS_ADDRESS,differentPlus.unavailable)
    }

    @Test fun contradictoryAddressControlsNeverChooseAnArbitraryHostOrPath(){
        listOf(
            listOf(BrowserAdapters.Field(text="example.com",description="Address bar, https://other.example/path")),
            listOf(BrowserAdapters.Field(text="example.com/allowed",description="Address bar, https://example.com/shorts")),
            listOf(BrowserAdapters.Field(text="example.com/search?q=allowed"),BrowserAdapters.Field(text="https://example.com/search?q=shorts")),
            listOf(BrowserAdapters.Field(description="example.com other.example.com"))
        ).forEach { fields ->
            val result=BrowserAdapters.inspect(browser,browser,fields)
            assertNull(result.address)
            assertEquals(BrowserAdapters.Unavailable.AMBIGUOUS_ADDRESS,result.unavailable)
            assertFalse(result.hostOnly)
        }
    }

    @Test fun domainOnlyCoverageDoesNotClaimAccessToAHiddenPathOrQuery(){
        listOf("example.com","https://example.com","https://example.com/").forEach { value ->
            val result=BrowserAdapters.inspect(browser,browser,listOf(BrowserAdapters.Field(text=value)))
            assertEquals(value,result.address)
            assertTrue("Only the domain was exposed: $value",result.hostOnly)
        }
        listOf("example.com/shorts","https://example.com/?q=shorts","https://example.com/#shorts").forEach { value ->
            val result=BrowserAdapters.inspect(browser,browser,listOf(BrowserAdapters.Field(text=value)))
            assertEquals(value,result.address)
            assertFalse("The URL exposes additional components: $value",result.hostOnly)
        }
    }

    @Test fun firefoxNativeHintSeparatorDoesNotChangeARealPeriodInThePath(){
        assertEquals("example.com/focus-fixture",BrowserAdapters.inspect(browser,browser,listOf(
            BrowserAdapters.Field(description=" example.com/focus-fixture. Search or enter address")
        )).address)
        assertEquals("example.com/ends-in-period.",BrowserAdapters.inspect(browser,browser,listOf(
            BrowserAdapters.Field(description=" example.com/ends-in-period.. Search or enter address")
        )).address)
        assertEquals("example.com/ends-in-period.",BrowserAdapters.inspect(browser,browser,listOf(
            BrowserAdapters.Field(description="example.com/ends-in-period.")
        )).address)
        assertEquals("example.com/shorts",BrowserAdapters.inspect(browser,browser,listOf(
            BrowserAdapters.Field(description=" example.com/shorts. Caută sau introdu o adresă")
        )).address)
    }

    @Test fun richerBrowserAddressReachesPathAndQueryKeywordMatching(){
        listOf(
            "https://example.com/shorts/42" to "shorts",
            "https://example.com/search?q=celestial%20navigation" to "celestial navigation"
        ).forEach { (full,keyword) ->
            val result=BrowserAdapters.inspect(browser,browser,listOf(BrowserAdapters.Field(text="example.com",description="Address bar, $full")))
            assertEquals(full,result.address)
            assertTrue("The extracted address must reach keyword evaluation",FocusWindows.matches(FocusGroup(keywords=setOf(keyword)),result.address!!))
            assertFalse(FocusWindows.matches(FocusGroup(keywords=setOf("unrelated-keyword")),result.address!!))
        }
    }
}
