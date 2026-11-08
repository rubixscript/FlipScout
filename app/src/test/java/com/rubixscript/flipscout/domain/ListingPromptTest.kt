package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListingPromptTest {
    @Test fun includesPlatformTitleAndPrice() {
        val p = listingPrompt(Item(title = "Retro Console", sellPrice = 80.0, notes = "Works"), Platform.MARKETPLACE)
        assertTrue(p.contains("Facebook Marketplace"))
        assertTrue(p.contains("Retro Console"))
        assertTrue(p.contains("Target price: $80.00"))
        assertTrue(p.contains("Condition/details: Works"))
    }

    @Test fun omitsOptionalLinesAndFallsBackToPhotoIdentification() {
        val p = listingPrompt(Item(), Platform.EBAY)
        assertTrue(p.contains("(identify from photos)"))
        assertFalse(p.contains("Target price"))
        assertFalse(p.contains("Condition/details"))
    }
}
