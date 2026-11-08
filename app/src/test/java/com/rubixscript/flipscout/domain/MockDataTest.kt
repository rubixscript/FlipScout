package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDataTest {
    private val now = 1_800_000_000_000L

    @Test fun everyMockRecordIsRecognisableAndUnique() {
        val ids = mockItems(now).map { it.id } + mockSellers(now).map { it.id }
        assertTrue(ids.all(::isMockId))
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun userIdsAreNeverMock() {
        assertFalse(isMockId("3f2a-user-item"))
    }

    @Test fun coversEveryStatus() {
        assertEquals(Status.entries.toSet(), mockItems(now).map { it.status }.toSet())
    }

    @Test fun soldItemsHaveSaleDatesAndOthersDoNot() {
        mockItems(now).forEach { assertEquals(it.status == Status.SOLD, it.soldAt > 0) }
    }

    @Test fun sellerLinksPointAtMockSellers() {
        val sellerIds = mockSellers(now).map { it.id }.toSet()
        mockItems(now).filter { it.sellerId.isNotBlank() }.forEach { assertTrue(it.sellerId in sellerIds) }
    }

    @Test fun mockSearchesAreMockAndMixPinnedWithRecent() {
        val searches = mockSearches(now)
        assertTrue(searches.all { isMockId(it.id) })
        assertEquals(searches.size, searches.map { it.id }.toSet().size)
        assertTrue(searches.any { it.pinned && it.category.isNotBlank() })
        assertTrue(searches.any { !it.pinned })
    }

    @Test fun includesStaleStock() {
        assertTrue(mockItems(now).any { it.isStale(now) })
    }
}
