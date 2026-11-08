package com.rubixscript.flipscout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SharedListingParserTest {
    @Test fun bareLinkKeepsOnlyUrl() {
        val l = parseSharedListing("https://www.facebook.com/marketplace/item/123456/")
        assertEquals("https://www.facebook.com/marketplace/item/123456/", l.url)
        assertEquals("", l.title)
        assertEquals(0.0, l.price, 0.001)
    }

    @Test fun marketplaceShareWithTitleAndPrice() {
        val l = parseSharedListing("Check out this listing: Vintage Film Camera - $45\nhttps://www.facebook.com/marketplace/item/9/")
        assertEquals("https://www.facebook.com/marketplace/item/9/", l.url)
        assertEquals("Vintage Film Camera", l.title)
        assertEquals(45.0, l.price, 0.001)
    }

    @Test fun ebayShareStripsSuffixAndParsesThousandsAndCents() {
        val l = parseSharedListing("Sony A7 Camera Body $1,249.99 | eBay https://ebay.us/abc123")
        assertEquals("https://ebay.us/abc123", l.url)
        assertEquals("Sony A7 Camera Body", l.title)
        assertEquals(1249.99, l.price, 0.001)
    }

    @Test fun trailingPunctuationIsNotPartOfUrl() {
        assertEquals("https://ebay.us/x", parseSharedListing("Look: https://ebay.us/x.").url)
    }

    @Test fun plainTextWithoutLinkBecomesTitle() {
        val l = parseSharedListing("Standing desk")
        assertEquals("", l.url)
        assertEquals("Standing desk", l.title)
    }
}
