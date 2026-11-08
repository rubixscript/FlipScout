package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Status
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class InsightsTest {

    private val zone = ZoneId.systemDefault()
    private fun at(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private val today = LocalDate.now(zone)

    @Test fun monthlyProfitBucketsBySoldDate() {
        val items = listOf(
            Item(status = Status.SOLD, buyPrice = 10.0, sellPrice = 40.0, soldAt = at(today)),                    // this month +30
            Item(status = Status.SOLD, buyPrice = 10.0, sellPrice = 25.0, soldAt = at(today.minusMonths(1))),     // last month +15
            Item(status = Status.SOLD, buyPrice = 10.0, sellPrice = 30.0, soldAt = at(today.minusMonths(2))),     // 2 months ago +20
            Item(status = Status.LISTED, buyPrice = 10.0, sellPrice = 99.0, soldAt = at(today)),                  // not sold: ignored
        )
        val months = items.monthlyProfit(4)
        assertEquals(listOf(0.0, 20.0, 15.0, 30.0), months.map { it.profit })
        assertEquals(YearMonth.now(zone), months.last().month)
    }

    @Test fun soldItemsWithoutDateFallBackToCreatedAt() {
        val created = at(today.minusMonths(3))
        val items = listOf(Item(status = Status.SOLD, buyPrice = 5.0, sellPrice = 15.0, createdAt = created))
        // monthlyProfit(4) = [3 months ago … now], so the fallback lands in the first bucket.
        assertEquals(listOf(10.0, 0.0, 0.0, 0.0), items.monthlyProfit(4).map { it.profit })
    }

    @Test fun emptyMonthsStillGetZeroBars() {
        assertEquals(MutableList(6) { 0.0 }, emptyList<Item>().monthlyProfit(6).map { it.profit })
    }

    @Test fun profitByCategoryBestFirstAndUsesFallback() {
        val items = listOf(
            Item(status = Status.SOLD, category = "Electronics", buyPrice = 10.0, sellPrice = 50.0), // +40
            Item(status = Status.SOLD, category = "Electronics", buyPrice = 10.0, sellPrice = 30.0), // +20
            Item(status = Status.SOLD, category = "", buyPrice = 5.0, sellPrice = 12.0),             // +7 → fallback
            Item(status = Status.BOUGHT, category = "Clothing", buyPrice = 5.0),                     // not sold: ignored
        )
        val groups = items.profitByCategory("Uncategorised")
        assertEquals(listOf("Electronics", "Uncategorised"), groups.map { it.label })
        assertEquals(listOf(60.0, 7.0), groups.map { it.profit })
        assertEquals(listOf(2, 1), groups.map { it.count })
    }

    @Test fun profitByPlatformBestFirstAndUsesFallback() {
        val items = listOf(
            Item(status = Status.SOLD, platform = "Depop", buyPrice = 5.0, sellPrice = 10.0), // +5
            Item(status = Status.SOLD, platform = "eBay", buyPrice = 10.0, sellPrice = 60.0), // +50
        )
        val groups = items.profitByPlatform("No platform")
        assertEquals(listOf("eBay", "Depop"), groups.map { it.label })
        assertEquals(listOf(50.0, 5.0), groups.map { it.profit })
    }

    @Test fun breakdownsSortTiesAlphabeticallyForStableOrder() {
        val items = listOf(
            Item(status = Status.SOLD, platform = "Zettle", buyPrice = 1.0, sellPrice = 2.0),
            Item(status = Status.SOLD, platform = "Alibaba", buyPrice = 1.0, sellPrice = 2.0),
        )
        assertEquals(listOf("Alibaba", "Zettle"), items.profitByPlatform("?").map { it.label })
    }
}
