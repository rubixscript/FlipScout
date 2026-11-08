package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Status
import org.junit.Assert.assertEquals
import org.junit.Test

class PortfolioStatsTest {
    @Test fun emptyListIsAllZero() {
        assertEquals(PortfolioStats(), emptyList<Item>().portfolioStats())
    }

    @Test fun aggregatesSoldAndInStock() {
        val items = listOf(
            Item(status = Status.SOLD, buyPrice = 20.0, sellPrice = 50.0, fees = 5.0), // +25, 125%
            Item(status = Status.SOLD, buyPrice = 10.0, sellPrice = 15.0),             // +5, 50%
            Item(status = Status.BOUGHT, buyPrice = 30.0),
            Item(status = Status.LISTED, buyPrice = 12.0, sellPrice = 40.0),
            Item(status = Status.WATCHING, buyPrice = 99.0),
        )
        val s = items.portfolioStats()
        assertEquals(30.0, s.profit, 0.001)
        assertEquals(42.0, s.inStock, 0.001)
        assertEquals(2, s.soldCount)
        assertEquals(87.5, s.avgRoi, 0.001)
    }
}
