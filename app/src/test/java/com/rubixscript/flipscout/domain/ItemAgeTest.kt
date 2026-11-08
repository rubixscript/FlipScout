package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemAgeTest {
    private val day = 86_400_000L
    private val now = 100 * day

    @Test fun countsWholeDays() {
        assertEquals(3, Item(createdAt = now - 3 * day - 1000).ageDays(now))
    }

    @Test fun missingOrFutureDateIsZero() {
        assertEquals(0, Item(createdAt = 0L).ageDays(now))
        assertEquals(0, Item(createdAt = now + day).ageDays(now))
    }

    @Test fun onlyUnsoldStockGoesStale() {
        val old = now - 20 * day
        assertTrue(Item(status = Status.LISTED, createdAt = old).isStale(now))
        assertTrue(Item(status = Status.BOUGHT, createdAt = old).isStale(now))
        assertFalse(Item(status = Status.SOLD, createdAt = old).isStale(now))
        assertFalse(Item(status = Status.WATCHING, createdAt = old).isStale(now))
    }

    @Test fun thresholdIsInclusive() {
        assertTrue(Item(status = Status.LISTED, createdAt = now - 14 * day).isStale(now))
        assertFalse(Item(status = Status.LISTED, createdAt = now - 13 * day).isStale(now))
    }
}
