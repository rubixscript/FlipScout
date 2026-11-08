package com.rubixscript.flipscout.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SellerTest {
    @Test fun hostStripsSchemePathAndWww() {
        assertEquals("facebook.com", Seller(url = "https://www.facebook.com/marketplace/profile/123").host)
        assertEquals("ebay.com", Seller(url = "https://m.ebay.com/str/vintagefinds").host)
    }

    @Test fun hostIsEmptyWithoutUrl() {
        assertEquals("", Seller().host)
    }
}
