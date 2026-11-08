package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Status
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemCsvTest {

    @Test fun roundTripsEveryField() {
        val items = listOf(
            Item(
                id = "item-1", title = "Stereo, \"vintage\"", buyPrice = 45.5, sellPrice = 120.0, fees = 12.0,
                shipping = 8.25, quantity = 3, status = Status.SOLD, category = "Electronics", platform = "eBay",
                tags = listOf("audio", "retro"), sellerId = "s1", link = "https://ebay.com/x",
                notes = "Line one\nLine two", createdAt = 1_700_000_000_000, soldAt = 1_700_000_100_000,
            ),
        )
        val parsed = csvToItems(itemsToCsv(items))
        assertEquals(items, parsed)
    }

    @Test fun quotesCommasAndNewlinesInsideFields() {
        val csv = itemsToCsv(listOf(Item(title = "A, B \"C\"\nD")))
        // The quoted field wraps onto a second physical line; the trailing terminator adds an empty segment.
        assertEquals(4, csv.lineSequence().toList().size)
        assertEquals(listOf("A, B \"C\"\nD"), csvToItems(csv).map { it.title })
    }

    @Test fun missingIdsGetFreshOnesAndUnknownColumnsAreIgnored() {
        val csv = "title,buy\nHandmade table,25"
        val parsed = csvToItems(csv)
        assertEquals(1, parsed.size)
        assertEquals("Handmade table", parsed[0].title)
        assertEquals(25.0, parsed[0].buyPrice, 0.001)
        assertEquals(Status.WATCHING, parsed[0].status)
        assert(parsed[0].id.isNotBlank())
    }

    @Test fun blankAndGarbageInputYieldNothing() {
        assertEquals(emptyList<Item>(), csvToItems(""))
        assertEquals(emptyList<Item>(), csvToItems("header only"))
    }

    @Test fun windowsLineEndingsParse() {
        val csv = "title,buy\r\nChair,10\r\nDesk,20\r\n"
        assertEquals(listOf("Chair", "Desk"), csvToItems(csv).map { it.title })
        assertEquals(listOf(10.0, 20.0), csvToItems(csv).map { it.buyPrice })
    }
}
