package com.rubixscript.flipscout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchLinksTest {
    @Test fun ebayUrlEncodesQuery() {
        assertEquals("https://www.ebay.com/sch/i.php?_nkw=nintendo+switch", searchUrl(SearchTarget.EBAY, "  nintendo switch "))
    }

    @Test fun soldUrlFiltersToCompletedSales() {
        assertEquals(
            "https://www.ebay.com/sch/i.php?_nkw=ps5&LH_Sold=1&LH_Complete=1",
            searchUrl(SearchTarget.EBAY_SOLD, "ps5"),
        )
    }

    @Test fun marketplaceUrlEscapesSpecialCharacters() {
        assertEquals(
            "https://www.facebook.com/marketplace/search/?query=a%26b+%2450",
            searchUrl(SearchTarget.MARKETPLACE, "a&b \$50"),
        )
    }
}
